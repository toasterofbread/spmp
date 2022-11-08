import atexit
import multiprocessing as mp
import subprocess
from threading import Thread
import sys
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
from waitress import serve
import lyrics as Lyrics
from os import path
from gh_md_to_html import main as markdown
from ytapi import YtApi

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

class Server:

    def __init__(self, port: int, creds: dict):
        self.port = port

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

        YtApi(self.app, self)

        self._cache = {}
        if path.isfile(CACHE_PATH):
            f = open(CACHE_PATH, "r")
            try:
                self._cache = json.loads(f.read())
            except JSONDecodeError as e:
                utils.warn(f"Parsing cache file at {CACHE_PATH} failed (ignoring)\n{e.msg}")
            f.close()

        @self.route("/")
        def index():
            md = markdown("server/pages/index.md", enable_image_downloading=False, website_root=PAGE_ROOT)

            f = open(path.join(PAGE_ROOT, "github-markdown-css/github-css.css"), "r")
            css = f.read()
            f.close()

            return md.replace(
                """<link href="/github-markdown-css/github-css.css" rel="stylesheet"/>""",
                f"<style>{css}</style>"
            )

        @self.route("/<resource>/")
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

        @self.route("/<resource>/<subresource>/")
        def indexSubResource(resource: str, subresource: str):
            return indexResource(path.join(resource, subresource))

        @self.route("/status/")
        def status():
            return {
                "uptime": int(time() - self.start_time)
            }

        @self.route("/restart/", True)
        def _restart():
            self.restart()
            return jsonify(0)

        @self.route("/stop/", True)
        def _stop():
            self.stop()
            return jsonify(0)

        @self.route("/update/", True)
        def update():
            subprocess.getoutput("git fetch")
            if (subprocess.getoutput(f"git diff --stat main origin/main") == ""):
                return "Already running the latest version"
            subprocess.getoutput("git pull")
            self.restart()
            return jsonify("Updated and restarting")

        @self.route("/lyrics/", True)
        @self.cacheable
        def lyrics():
            title = request.args.get("title")
            artist = request.args.get("artist")

            if title is None:
                return self.errorResponse(400, "Missing title parameter")

            id = Lyrics.findLyricsId(title, artist)
            if id is None:
                return self.errorResponse(404, "Query doesn't match any songs")

            lyrics = {
                "lyrics": Lyrics.getLyrics(id).getWithFurigana(),
                "source": "PetitLyrics", # TODO
                "timed": True
            }

            return jsonify(lyrics)

    def route(path: str, require_key: bool):
        if not path.endswith("/"):
            path += "/"
        if not path.startswith("/"):
            path = "/" + path
        
        def wrapper(func):
            @wraps(func)
            @server.route(path)
            def decorated(*args, **kwargs):
                utils.log(f"{'Authenticated' if (require_key) else 'Unauthenticated'} request recieved with url {request.url}")
                if request.args.get("key") and request.args.get("key") == self.api_key:
                    return func(*args, **kwargs)
                else:
                    return self.errorResponse(401, "Missing or invalid key parameter")
                
            return decorated
        return wrapper

    def cacheable(self, func):
        @wraps(func)
        def decorated(*args, **kwargs):
            params = dict(kwargs)
            params.update(request.args)
            key = func.__name__ + str(params)

            if not request.args.get("noCached", None):
                cached = server.getCache(key, url)
                if cached is not None:
                    return jsonify(cached)

            ret: Response = func(*args, **kwargs)
            if ret.status_code == 200:
                server.setCache(key, url, ret.get_json())

            return ret
        return decorated

    def start(self):
        self.start_time = time()
        # sys.stdout = open("/dev/null", "w")
        # sys.stderr = sys.stdout

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
        psutil.Process(self.ngrok_process.pid).kill()
        if self.restart_queue is not None:
            self.restart_queue.put(True)
        utils.log("Restarting...")

    def stop(self):
        psutil.Process(self.ngrok_process.pid).kill()
        if self.restart_queue is not None:
            self.restart_queue.put(False)
        utils.log("Stopping...")

    def getCache(self, dir: str, id):
        if not dir in self._cache:
            return None

        cached = self._cache[dir].get(id)
        if cached is None:
            return None

        if time() - cached["time"] > CACHE_LIFETIME:
            self._cache[dir].pop(id)
            self.saveCache()
            return None

        return cached["data"]

    def setCache(self, dir: str, id, data):
        if not dir in self._cache:
            obj = {}
            self._cache[dir] = obj
        else:
            obj = self._cache[dir]
        obj[id] = {"data": data, "time": int(time())}
        self.saveCache()

    def saveCache(self):
        f = open(CACHE_PATH, "w")
        f.write(json.dumps(self._cache))
        f.close()

    def errorResponse(self, code: int, message: str):
        response = jsonify(message)
        response.status_code = code
        return response
