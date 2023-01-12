import sys
from spectre7 import utils
import json
from json import JSONDecodeError
from functools import wraps
import requests
from flask import Flask, abort, jsonify, request
from flask.wrappers import Response
from time import time, sleep
from werkzeug.serving import make_server as wserve
import lyrics as Lyrics
from os import path
from gh_md_to_html import main as markdown
from ytapi import YtApi
from threading import Thread
from urllib.parse import urlencode

RECOMMENDED_ROWS = 10
CACHE_LIFETIME = 24 * 60 * 60
CACHE_PATH = path.join(path.dirname(__file__), "cache.json")
PAGE_ROOT = path.abspath(path.join(path.dirname(__file__), "pages"))

def getNgrokUrl(headers: dict, timeout: int = 5):
    while timeout > 0:
        try:
            result = requests.get("https://api.ngrok.com/tunnels", headers=headers)
            return json.loads(result.text)["tunnels"][0]["public_url"]
        except IndexError as e:
            sleep(1)

            timeout -= 1
            if timeout <= 0:
                raise e

def isMutexLocked(mutex):
    locked = mutex.acquire(blocking=False)
    if locked == False:
        return True
    else:
        mutex.release()
        return False

class Server:

    def __init__(self, port: int, creds: dict, integrated: bool = False):
        self.port = port
        self.integrated = integrated

        self.api_key = creds["api_key"]
        self.ytapi_key = creds["ytapi_key"]
        self.ytm_headers = creds["ytm_headers"]
        self.ngrok_headers = creds["ngrok_headers"]

        self.app = Flask("SpMp Server")
        self.server = None
        self.start_time = 0
        self.restart_queue = None

        YtApi(self.app, self)

        self._cache = {}
        if not self.integrated and path.isfile(CACHE_PATH):
            f = open(CACHE_PATH, "r")
            try:
                self._cache = json.loads(f.read())
            except JSONDecodeError as e:
                utils.warn(f"Parsing cache file at {CACHE_PATH} failed (ignoring)\n{e.msg}")
            f.close()

        @self.route("/", disable_on_integrated=True)
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
        def indexResource(resource: str, disable_on_integrated=True):

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

        @self.route("/<resource>/<subresource>/", disable_on_integrated=True)
        def indexSubResource(resource: str, subresource: str):
            return indexResource(path.join(resource, subresource))

        @self.route("/status/", disable_on_integrated=True)
        def status():
            return {
                "uptime": int(time() - self.start_time)
            }

        @self.route("/restart/", True, True)
        def _restart():
            return jsonify(self.restart())

        @self.route("/stop/", True, True)
        def _stop():
            return jsonify(self.stop())

        @self.route("/update/", True, True)
        def update():
            import subprocess
            subprocess.getoutput("git fetch")
            if (subprocess.getoutput(f"git diff --stat main origin/main") == ""):
                return "Already running the latest version"
            subprocess.getoutput("git pull")
            self.restart()
            return jsonify("Updated and restarting")

        @self.cacheable
        @self.route("/get_lyrics/", True)
        def lyricsGet():
            lyrics_id = request.args.get("id")
            if lyrics_id is None:
                return self.errorResponse(400, "Missing id parameter")

            return getLyricsData(lyrics_id)

        @self.cacheable
        @self.route("/search_lyrics/", True)
        def lyricsSearch():
            title = request.args.get("title")
            artist = request.args.get("artist")

            if title is None:
                return self.errorResponse(400, "Missing title parameter")

            return jsonify(Lyrics.searchForLyrics(title, artist))

        @self.cacheable
        @self.route("/search_and_get_lyrics/", True)
        def lyricsSearchAndGet():
            title = request.args.get("title")
            artist = request.args.get("artist")

            if title is None:
                return self.errorResponse(400, "Missing title parameter")

            lyrics = Lyrics.searchForLyrics(title, artist)
            if len(lyrics) == 0:
                return self.errorResponse(404, "Query doesn't match any songs")

            return getLyricsData(lyrics[0]["id"])

        def getLyricsData(lyrics_id: str):
            lyrics = Lyrics.getLyrics(lyrics_id)

            if lyrics is None:
                return self.errorResponse(404, f"Failed to fetch lyrics data (id: {id})")

            ret = {
                "lyrics": lyrics.getWithFurigana(),
                "id": lyrics.getId(),
                "source": lyrics.getSource(),
                "sync": lyrics.getSyncType()
            }

            return jsonify(ret)

    def route(self, path: str, require_key: bool = False, disable_on_integrated: bool = False, **kwargs):
        if not path.endswith("/"):
            path += "/"
        if not path.startswith("/"):
            path = "/" + path

        def wrapper(func):
            @wraps(func)
            def decorated(*args, **kwargs):
                if self.integrated and disable_on_integrated:
                    utils.warn(f"Request received, but is disabled on integrated server. URL: {request.url}")
                    return self.errorResponse(501, "Endpoint disabled on integrated server")

                utils.log(f"{'Authenticated' if (require_key) else 'Unauthenticated'} request recieved with URL {request.url}")
                if not require_key or (request.args.get("key") and request.args.get("key") == self.api_key):
                    return func(*args, **kwargs)
                else:
                    return self.errorResponse(401, "Missing or invalid key parameter")

            decorated.__name__ = func.__name__
            return self.app.route(path, **kwargs)(decorated)
        return wrapper

    def cacheable(self, func):
        @wraps(func)
        def decorated(*args, **kwargs):
            params = dict(kwargs)
            params.update(request.args)
            key = json.dumps(params)

            if request.args.get("noCache", None) != "1":
                cached = self.getCache(request.path, key)
                if cached is not None:
                    utils.info("Using cached value")
                    return jsonify(cached)

            ret: Response = func(*args, **kwargs)
            if ret.status_code == 200:
                self.setCache(request.path, key, ret.get_json())

            return ret
        return decorated

    def start(self):
        self.start_time = time()
        # sys.stdout = open("/dev/null", "w")
        # sys.stderr = sys.stdout

        self.server = wserve(app = self.app, host = "0.0.0.0", port = self.port, threaded = True)

        if not self.integrated:
            import subprocess, multiprocessing
            ngrok_process = subprocess.Popen(f"exec ngrok http {self.port}", shell=True, stdout=open("/dev/null", "w"))
            q = multiprocessing.Queue()
        else:
            ngrok_process = None
            q = None

        def run(server, queue):
            self.restart_queue = queue
            server.serve_forever()

        thread = Thread(target=run, args=(self.server, q))
        thread.start()

        sleep(0.5)

        utils.info(f"* Running locally on http://localhost:{self.port}")
        if self.integrated:
            utils.info("* Running in integrated mode, no public URL available")
        else:
            utils.info(f"* Public URL is {getNgrokUrl(self.ngrok_headers)}")

        if not self.integrated:
            try:
                thread.join()
            except KeyboardInterrupt:
                utils.info("\nExiting...")
                exit()

            import psutil, subprocess
            psutil.Process(ngrok_process.pid).kill() # type: ignore

            if q.get(True): # type: ignore
                try:
                    subprocess.call([sys.executable] + sys.argv)
                except KeyboardInterrupt:
                    exit()

    def restart(self):
        if self.server:
            self.server.shutdown()
        if self.integrated:
            return
        if self.restart_queue is not None:
            self.restart_queue.put(True)
        utils.log("Restarting...")

    def stop(self):
        if self.server:
            self.server.shutdown()
        if self.restart_queue is not None:
            self.restart_queue.put(False)
        utils.log("Stopping...")

    def getCache(self, group: str, id):
        if not group in self._cache:
            return None

        cached = self._cache[group].get(id)
        if cached is None:
            return None

        if time() - cached["time"] > CACHE_LIFETIME:
            self._cache[group].pop(id)
            self.saveCache()
            return None

        return cached["data"]

    def setCache(self, group: str, id, data):
        if not group in self._cache:
            obj = {}
            self._cache[group] = obj
        else:
            obj = self._cache[group]
        obj[id] = {"data": data, "time": int(time())}
        self.saveCache()

    def saveCache(self):
        if self.integrated:
            return

        f = open(CACHE_PATH, "w")
        f.write(json.dumps(self._cache))
        f.close()

    def errorResponse(self, code: int, message = None):
        if message is None:
            message = f"An error occurred ({code})"

        ret =  Response(
            json.dumps(message, indent="\t") + "\n",
            mimetype="application/json"
        )
        ret.status_code = code
        return ret

    def getPort(self):
        return self.port

    def formatEndpoint(self, endpoint: str) -> str:
        if not endpoint.startswith("/"):
            endpoint = "/" + endpoint
        if not endpoint.endswith("/"):
            endpoint += "/"
        return endpoint

    def performRequest(self, endpoint: str, params, post_body: str | None) -> str | None:

        params = {entry.getKey(): entry.getValue() for entry in params.entrySet().toArray()}
        params["key"] = self.api_key

        client = self.app.test_client()

        if post_body is None:
            response = client.get(self.formatEndpoint(endpoint), query_string = urlencode(params))
        else:
            response = client.post(self.formatEndpoint(endpoint), query_string = urlencode(params), data = post_body)

        return response.data.decode("utf-8")
