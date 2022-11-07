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

            def postRequest(ctoken: str | None) -> dict | Response:
                response =  requests.post(
                    "https://music.youtube.com/youtubei/v1/browse",
                    params = {"ctoken": ctoken, "continuation": ctoken, "type": "next"} if ctoken is not None else {},
                    headers = self.ytm_headers,
                    json = {
                        "context":{
                            "client":{
                                "hl": request.args.get("lang", "en"),
                                "platform":"DESKTOP",
                                "clientName":"WEB_REMIX",
                                "clientVersion":"1.20221031.00.00-canary_control",
                                "userAgent":"Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0,gzip(gfe)",
                                "acceptHeader":"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                            },
                            "user":{
                                "lockedSafetyMode": False
                            },
                            "request":{
                                "useSsl": True,
                                "internalExperimentFlags":[], "consistencyTokenJars":[]
                            },
                        }
                    }
                )

                if response.status_code != 200:
                    return self.errorResponse(response.status_code, response.text)

                if ctoken is not None:
                    return response.json()["continuationContents"]["sectionListContinuation"]
                else:
                    return response.json()["contents"]["singleColumnBrowseResultsRenderer"]["tabs"][0]["tabRenderer"]["content"]["sectionListRenderer"]

            def processRows(rows: list):
                ret = []
                for row in rows:
                    data = row["musicCarouselShelfRenderer"]

                    items = []
                    title = data["header"]["musicCarouselShelfBasicHeaderRenderer"]["title"]["runs"][0]
                    entry = {
                        "title": title["text"],
                        "browse_id": title["navigationEndpoint"]["browseEndpoint"]["browseId"] if "navigationEndpoint" in title else None,
                        "subtitle": title["strapline"]["runs"][0]["text"] if "strapline" in title else None,
                        "items": items,
                    }

                    for item in data["contents"]:
                        key = list(item.keys())[0]
                        item = item[key]
                        match key:
                            case "musicTwoRowItemRenderer":
                                item_entry = {}
                                items.append(item_entry)
                                try:
                                    item_entry["id"] = item["navigationEndpoint"]["watchEndpoint"]["videoId"]
                                    item_entry["type"] = "song"
                                    try:
                                        item_entry["playlist_id"] = item["navigationEndpoint"]["watchEndpoint"]["playlistId"]
                                    except KeyError:
                                        pass
                                except KeyError:
                                    try:
                                        item_entry["id"] = item["navigationEndpoint"]["browseEndpoint"]["browseId"]
                                        item_type = item["navigationEndpoint"]["browseEndpoint"]["browseEndpointContextSupportedConfigs"]["browseEndpointContextMusicConfig"]["pageType"]
                                        match item_type:
                                            case "MUSIC_PAGE_TYPE_ALBUM" | "MUSIC_PAGE_TYPE_PLAYLIST":
                                                item_entry["type"] = "playlist"
                                            case "MUSIC_PAGE_TYPE_ARTIST":
                                                item_entry["type"] = "artist"
                                            case _:
                                                raise RuntimeError(item_type)
                                    except KeyError:
                                        raise RuntimeError(json.dumps(item, indent="\t"))
                            case "musicResponsiveListItemRenderer":
                                items.append({
                                    "type": "song",
                                    "id": item["playlistItemData"]["videoId"],
                                    "playlist_id": None
                                })
                            case _:
                                raise RuntimeError(key)

                    if len(items) > 0:
                        ret.append(entry)

                return ret

            try:
                min_rows = int(request.args.get("minRows", -1))
            except ValueError:
                return self.errorResponse(400, "Invalid minRows parameter")

            data = postRequest(None)
            if isinstance(data, Response):
                return data

            rows = processRows(data["contents"])

            while len(rows) < min_rows:
                ctoken = data["continuations"][0]["nextContinuationData"]["continuation"]
                data = postRequest(ctoken)
                if isinstance(data, Response):
                    return data

                rows += processRows(data["contents"])

            return jsonify(rows)

        @self.app.route("/lyrics/")
        @self.requireKey
        def lyrics():
            title = request.args.get("title")
            artist = request.args.get("artist")

            if title is None:
                return self.errorResponse(400, "Missing title parameter")

            id = Lyrics.findLyricsId(title, artist)
            if id is None:
                return self.errorResponse(404, "Query doesn't match any songs")

            cached = self.getCache("lyrics", id)
            if cached is not None:
                return jsonify(cached)

            lyrics = {
                "lyrics": Lyrics.getLyrics(id).getWithFurigana(),
                "source": "PetitLyrics", # TODO
                "timed": True
            }
            self.setCache("lyrics", id, lyrics)
            return jsonify(lyrics)

    def requireKey(self, func):
        @wraps(func)
        def decoratedFunction(*args, **kwargs):
            utils.log(f"Request recieved with url {request.url}")
            if request.args.get("key") and request.args.get("key") == self.api_key:
                return func(*args, **kwargs)
            else:
                abort(Response("Missing or invalid key parameter", 401))
        return decoratedFunction

    def start(self,):
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

    def errorResponse(self, code: int, message: str):
        response = jsonify(message)
        response.status_code = code
        return response
