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
from gh_md_to_html import main as markdown

RECOMMENDED_ROWS = 10
CACHE_LIFETIME = 24 * 60 * 60
CACHE_PATH = path.join(path.dirname(__file__), "cache.json")
PAGE_ROOT = path.abspath(path.join(path.dirname(__file__), "pages"))

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

        self._cache = {"yt": {}, "lyrics": {}}
        if path.isfile(CACHE_PATH):
            f = open(CACHE_PATH, "r")
            try:
                self._cache = json.loads(f.read())
            except JSONDecodeError as e:
                utils.warn(f"Parsing cache file at {CACHE_PATH} failed (ignoring)\n{e.msg}")
            f.close()

        @self.app.route("/")
        def index():
            md = markdown("server/pages/index.md", enable_image_downloading=False, website_root=PAGE_ROOT)

            f = open(path.join(PAGE_ROOT, "github-markdown-css/github-css.css"), "r")
            css = f.read()
            f.close()

            return md.replace(
                """<link href="/github-markdown-css/github-css.css" rel="stylesheet"/>""",
                f"<style>{css}</style>"
            )

        @self.app.route("/<resource>/")
        def indexResource(resource: str):

            resource = path.abspath(path.join(PAGE_ROOT, resource))
            if not resource.startswith(PAGE_ROOT):
                utils.err(f"GET | {resource}")
                abort(403)

            if not path.isfile(resource):
                utils.err(f"GET | {resource}")
                abort(404)

            utils.log(f"GET | {resource}")

            f = open(resource, "rb")
            data = f.read()
            f.close()
            return data

        @self.app.route("/<resource>/<subresource>/")
        def indexSubResource(resource: str, subresource: str):
            return indexResource(path.join(resource, subresource))

        @self.app.route("/status/")
        def status():
            return {
                "uptime": int(time() - self.start_time)
            }

        @self.app.route("/restart/")
        @self.requireKey
        def _restart():
            self.restart()
            return jsonify(0)

        @self.app.route("/stop/")
        @self.requireKey
        def _stop():
            self.stop()
            return jsonify(0)

        @self.app.route("/update/")
        @self.requireKey
        def update():
            subprocess.getoutput("git fetch")
            if (subprocess.getoutput(f"git diff --stat main origin/main") == ""):
                return "Already running the latest version"
            subprocess.getoutput("git pull")
            self.restart()
            return jsonify("Updated and restarting")

        @self.app.route("/feed/")
        @self.requireKey
        def feed():
            if not self.cached_feed_set.get():
                self.refresh_mutex.acquire()
                self.refresh_mutex.release()
            return jsonify(self.cached_feed.get())

        @self.app.route("/feed/latest/")
        @self.requireKey
        def latestFeed():
            if isMutexLocked(self.refresh_mutex):
                self.refresh_mutex.acquire()
                self.refresh_mutex.release()
            return jsonify(self.cached_feed.get())

        @self.app.route("/feed/refreshed/")
        @self.requireKey
        def refreshedFeed():
            if not isMutexLocked(self.refresh_mutex):
                self.refreshFeed()

            self.refresh_mutex.acquire()
            self.refresh_mutex.release()

            return jsonify(self.cached_feed.get())

        @self.app.route("/feed/refresh/")
        @self.requireKey
        def refreshFeed():
            return jsonify(self.refreshFeed())

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

            cached = self.getCache("lyrics", id)
            if cached is not None:
                return jsonify(cached)

            lyrics = {
                "lyrics": Lyrics.getLyrics(id).getWithFurigana(),
                "source": "PetitLyrics", # TODO
                "timed": True
            }
            self.setCache("lyrics", id, lyrics)
            return jsonify(cached)

        @self.app.route("/youtubeapi/<endpoint>/")
        @self.requireKey
        def youtubeApi(endpoint: str):
            params = dict(request.args)
            params["key"] = self.ytapi_key
            url = f"https://www.googleapis.com/youtube/v3/{endpoint}?" + urlencode(params)

            cached = self.getCache("yt", url)
            if cached is not None:
                return jsonify(cached)

            response = requests.get(url)
            if response.status_code != 200:
                return error(response.status_code, f"{response.reason}\n{response.text}")

            data = json.loads(response.text)

            self.setCache("yt", url, data)
            return jsonify(data)

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

    def getCache(self, dir: str, id):
        cached = self._cache[dir].get(id)
        if cached is None:
            return None

        if time() - cached["time"] > CACHE_LIFETIME:
            self._cache[dir].pop(id)
            self.saveCache()
            return None

        return cached["data"]

    def setCache(self, dir: str, id, data):
        self._cache[dir][id] = {"data": data, "time": int(time())}
        self.saveCache()

    def saveCache(self):
        f = open(CACHE_PATH, "w")
        f.write(json.dumps(self._cache))
        f.close()
