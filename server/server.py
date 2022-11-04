import atexit
import multiprocessing as mp
import subprocess
from threading import Thread
import sys
from urllib.parse import urlencode
import psutil
from apscheduler.schedulers.background import BackgroundScheduler
from spectre7 import utils
from urllib3.exceptions import ProtocolError
import json
from json import JSONDecodeError
from functools import wraps
import requests
from flask import Flask, abort, jsonify, request
from flask.wrappers import Response
from time import time, sleep
from scrapeytmusic import scrapeYTMusicHome
from waitress import serve
import lyrics as Lyrics
from os import path

RECOMMENDED_ROWS = 10
YT_API_CACHE_LIFETIME = 24 * 60 * 60
YT_API_CACHE_PATH = path.join(path.dirname(__file__), "ytapicache.json")

def getNgrokUrl(headers: dict):
    result = requests.get("https://api.ngrok.com/tunnels", headers=headers)
    return json.loads(result.text)["tunnels"][0]["public_url"]

def isMutexLocked(mutex):
    locked = mutex.acquire(blocking=False)
    if locked == False:
        return True
    else:
        mutex.release()
        return False

def error(code: int, message: str):
    response = jsonify(message)
    response.status_code = code
    return response

class Server:

    def __init__(self, port: int, firefox_path: str, creds: dict):
        self.port = port
        self.firefox_path = firefox_path

        self.api_key = creds["api_key"]
        self.ytapi_key = creds["ytapi_key"]
        self.ytm_headers = creds["ytm_headers"]
        self.ngrok_headers = creds["ngrok_headers"]

        self.app = Flask("SpMp Server")
        self.start_time = 0
        self.restart_queue = None

        manager = mp.Manager()
        self.refresh_mutex = manager.Lock()
        self.cached_feed = manager.Value("l", [])
        self.cached_feed_set = manager.Value("b", False)
        self.exiting = manager.Value("b", False)

        self._yt_api_cache = {}
        if path.isfile(YT_API_CACHE_PATH):
            f = open(YT_API_CACHE_PATH, "r")
            try:
                self._yt_api_cache = json.loads(f.read())
            except JSONDecodeError as e:
                utils.warn(f"Parsing YtApi cache file at {YT_API_CACHE_PATH} failed (ignoring)\n{e.msg}")
            f.close()

        @self.app.route("/")
        def index():
            return "Hello World!"

        @self.app.route("/status/")
        def status():
            return {
                "uptime": int(time() - self.start_time)
            }

        @self.app.route("/restart/")
        @self.requireKey
        def _restart():
            self.restart()
            return "Restarting..."

        @self.app.route("/stop/")
        @self.requireKey
        def _stop():
            self.stop()
            return "Stopping..."

        @self.app.route("/update/")
        @self.requireKey
        def update():
            subprocess.getoutput("git fetch")
            if (subprocess.getoutput(f"git diff --stat main origin/main") == ""):
                return "Already running the latest version"
            subprocess.getoutput("git pull")
            self.restart()
            return "Updated and restarting"

        @self.app.route("/feed/")
        @self.requireKey
        def feed():
            if not self.cached_feed_set.get():
                self.refresh_mutex.acquire()
                self.refresh_mutex.release()
            return self.cached_feed.get()

        @self.app.route("/feed/latest/")
        @self.requireKey
        def latestFeed():
            if isMutexLocked(self.refresh_mutex):
                self.refresh_mutex.acquire()
                self.refresh_mutex.release()
            return self.cached_feed.get()

        @self.app.route("/feed/refreshed/")
        @self.requireKey
        def refreshedFeed():
            if not isMutexLocked(self.refresh_mutex):
                self.refreshFeed()

            self.refresh_mutex.acquire()
            self.refresh_mutex.release()

            return self.cached_feed.get()

        @self.app.route("/feed/refresh/")
        @self.requireKey
        def refreshFeed():
            return self.refreshFeed()

        @self.app.route("/lyrics/")
        @self.requireKey
        def lyrics():
            title = request.args.get("title")
            artist = request.args.get("artist")

            if title is None:
                return error(400, "Missing title parameter")

            id = Lyrics.findLyricsId(title, artist)
            if id is None:
                return error(404, "Query doesn't match any songs")

            lyrics = Lyrics.getLyrics(id)
            return {
                "lyrics": lyrics.getWithFurigana(),
                "source": "PetitLyrics", # TODO
                "timed": True
            }

        @self.app.route("/youtubeapi/<endpoint>/")
        @self.requireKey
        def youtubeApi(endpoint: str):
            params = dict(request.args)
            params["key"] = self.ytapi_key
            url = f"https://www.googleapis.com/youtube/v3/{endpoint}?" + urlencode(params)

            cached = self.getYtApiCache(url)
            if cached is not None:
                return cached

            response = requests.get(url)
            if response.status_code != 200:
                return error(response.status_code, f"{response.reason}\n{response.text}")

            data = json.loads(response.text)
          
            self.setYtApiCache(url, data)
            return data

    def requireKey(self, func):
        @wraps(func)
        def decoratedFunction(*args, **kwargs):
            utils.log(f"Request recieved with url {request.url}")
            if request.args.get("key") and request.args.get("key") == self.api_key:
                return func(*args, **kwargs)
            else:
                abort(Response("Missing or invalid key parameter", 401))
        return decoratedFunction

    def refreshFeed(self):
        if isMutexLocked(self.refresh_mutex):
            return {"result": 1, "error": "Already refreshing"}

        def thread():
            try:
                self.refresh_mutex.acquire()
                utils.log("Refreshing feed...")

                feed = scrapeYTMusicHome(self.ytm_headers, self.firefox_path, shouldCancel = lambda : self.exiting.get())
                if feed is not None:
                    self.cached_feed.set(feed)
                    self.cached_feed_set.set(True)
                    utils.log("Feed refresh completed")
                else:
                    utils.info("Feed refresh cancelled")

                self.refresh_mutex.release()
            except ProtocolError as e:
                try:
                    print(e)
                except:
                    pass

        Thread(target=thread).start()
        return {"result": 0}

    def start(self, refresh_feed: bool = True):
        self.start_time = time()
        # sys.stdout = open("/dev/null", "w")
        # sys.stderr = sys.stdout

        self.scheduler = BackgroundScheduler()
        self.scheduler.add_job(func=self.refreshFeed, trigger="interval", seconds = 60 * 60 * 6)
        self.scheduler.start()
        atexit.register(self.scheduler.shutdown)

        def run(queue):
            self.restart_queue = queue
            serve(self.app, host="0.0.0.0", port = self.port)

        self.ngrok_process = subprocess.Popen(f"exec ngrok http {self.port}", shell=True, stdout=open("/dev/null", "w"))

        q = mp.Queue()
        app_process = mp.Process(target=run, args=(q,))
        app_process.start()

        sleep(1)

        for msg in (
            f"Running locally on http://127.0.0.1:{self.port}",
            f"Public URL is {getNgrokUrl(self.ngrok_headers)}"
        ):
            utils.info(f"* {msg}")

        if refresh_feed:
            self.refreshFeed()

        # Wait for restart request
        restart = False
        while True:
            try:
                sleep(1)
            except KeyboardInterrupt:
                utils.info("\nExiting...")
                exit()

            if not q.empty():
                self.exiting.set(True)
                restart = q.get(True)
                break

        app_process.terminate()
        if restart:
            subprocess.call([sys.executable] + sys.argv)

    def restart(self):
        self.scheduler.shutdown()
        psutil.Process(self.ngrok_process.pid).kill()
        if self.restart_queue is not None:
            self.restart_queue.put(True)
        utils.log("Restarting...")

    def stop(self):
        self.scheduler.shutdown()
        psutil.Process(self.ngrok_process.pid).kill()
        if self.restart_queue is not None:
            self.restart_queue.put(False)
        utils.log("Stopping...")

    def getYtApiCache(self, url: str):
        cached = self._yt_api_cache.get(url)
        if cached is None:
            return None

        if time() - cached["time"] > YT_API_CACHE_LIFETIME:
            self._yt_api_cache.pop(url)
            self.saveYtApiCache()
            return None

        return cached["data"]

    def setYtApiCache(self, url: str, data):
        self._yt_api_cache[url] = {"data": data, "time": int(time())}
        self.saveYtApiCache()

    def saveYtApiCache(self):
        f = open(YT_API_CACHE_PATH, "w")
        f.write(json.dumps(self._yt_api_cache))
        f.close()
