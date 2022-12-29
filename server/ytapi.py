from functools import wraps
from urllib.parse import urlencode, urlparse, parse_qs
from flask import Flask, request, jsonify
from flask.wrappers import Response
import requests
import json
from ytmusicapi import YTMusic
from spectre7 import utils
from threading import Thread

USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"
DEFAULT_LANGUAGE = "en"

class ThreadWithReturnValue(Thread):
    def __init__(self, group=None, target=None, name=None, args=(), kwargs={}, Verbose=None):
        Thread.__init__(self, group, target, name, args, kwargs)
        self._return = None

    def run(self):
        try:
            if self._target is not None: # type: ignore
                self._return = self._target(*self._args, **self._kwargs) # type: ignore
        finally:
            del self._target, self._args, self._kwargs # type: ignore

    def join(self, *args):
        Thread.join(self, *args)
        return self._return

class YtApi:

    def __init__(self, app: Flask, server):

        self.server = server
        self.ytmusic = YTMusic()

        if not server.integrated:
            from yt_dlp import YoutubeDL
            self.ytd = YoutubeDL()
        else:
            self.ytd = None

        def localiseData(data: dict, language: str | None):
            if language is None or not "snippet" in data or not "localizations" in data:
                return

            language = language.replace("-", "_")
            loc_data = data["localizations"]

            code = None
            if language in loc_data:
                code = language
            else:
                language = language.split("_", 1)[0]
                if language in loc_data:
                    code = language
                else:
                    for loc in loc_data:
                        if loc.startswith(language):
                            code = loc
                            break
                    if code is None:
                        return

            loc_data = loc_data[code]
            for key in loc_data:
                data["snippet"][key] = loc_data[key]

        def requestVideoInfo(params: dict) -> dict | Response:
            language: str | None = params.get("dataLang", None)

            try:
                cache = server.getCache("requestVideoInfo", params["id"] + str(language))
            except KeyError:
                return server.errorResponse(400)

            if cache is not None:
                return cache

            params["key"] = server.ytapi_key

            remove_localisations = False
            if language is not None and "part" in params and not "localizations" in params["part"]:
                params["part"] += ",localizations"
                remove_localisations = True

            url = f"https://www.googleapis.com/youtube/v3/videos?" + urlencode(params)
            response = requests.get(url)
            if response.status_code != 200:
                message: dict = {"reason": response.reason}
                message.update(json.loads(response.text))
                return server.errorResponse(response.status_code, message)

            ret: dict = response.json()["items"][0]

            localiseData(ret, language)

            if remove_localisations:
                ret.pop("localizations", None)

            server.setCache("requestVideoInfo", params["id"] + str(language), ret)

            return ret

        @server.route("/yt/videos", True)
        def videos():
            result = requestVideoInfo(dict(request.args))
            if isinstance(result, Response):
                return result
            return jsonify(result)

        def requestChannelInfo(params: dict) -> dict | Response:
            if not "id" in params:
                return server.errorResponse(400)

            language: str | None = params.get("dataLang", None)

            original_id = params["id"]
            params["id"] = self.ensureCorrectChannelId(original_id)

            if params["id"] is None:
                raise RuntimeError()

            cache = server.getCache("requestChannelInfo", params["id"] + str(language))
            if cache is not None:
                utils.info("Using cached channel info")
                cache["original_id"] = original_id
                return cache

            params["key"] = server.ytapi_key

            remove_localisations = False
            if language is not None and "part" in params and not "localizations" in params["part"]:
                params["part"] += ",localizations"
                remove_localisations = True

            url = f"https://www.googleapis.com/youtube/v3/channels?" + urlencode(params)

            response = requests.get(url)
            if response.status_code != 200:
                message: dict = {"reason": response.reason}
                message.update(json.loads(response.text))
                return server.errorResponse(response.status_code, message)

            ret = response.json()["items"][0]
            localiseData(ret, language)

            if remove_localisations:
                ret.pop("localizations", None)

            server.setCache("requestChannelInfo", params["id"], ret)

            ret["original_id"] = original_id

            if "snippet" in ret:
                ret["snippet"].update(self.getMusicChannelInfo(params["id"], language or DEFAULT_LANGUAGE))

            return ret

        @server.route("/yt/channels", True)
        def channels():
            result = requestChannelInfo(dict(request.args))
            if isinstance(result, Response):
                return result
            return jsonify(result)

        def requestPlaylistInfo(params: dict) -> dict | Response:
            if not "id" in params:
                return server.errorResponse(400)

            language: str | None = params.get("dataLang", None)

            cache = server.getCache("requestPlaylistInfo", params["id"] + str(language))
            if cache is not None:
                utils.info("Using cached playlist info")
                return cache

            remove_localisations = False
            params["key"] = server.ytapi_key

            if "part" in params:
                part: list[str] = params["part"].split(",")
                for item in list(part):
                    if not item.strip() in ("contentDetails", "id", "localizations", "player", "snippet", "status"):
                        part.remove(item)
                params["part"] = ",".join(part)

                if language is not None and not "localizations" in params["part"]:
                    params["part"] += ",localizations"
                    remove_localisations = True

            url = f"https://www.googleapis.com/youtube/v3/playlists?" + urlencode(params)

            response = requests.get(url)
            if response.status_code != 200:
                message: dict = {"reason": response.reason}
                message.update(json.loads(response.text))
                return server.errorResponse(response.status_code, message)

            try:
                ret = response.json()["items"][0]
                localiseData(ret, language)

                if remove_localisations:
                    ret.pop("localizations", None)

                server.setCache("requestPlaylistInfo", params["id"], ret)

                return ret
            except IndexError:
                return {"id": params["id"], "error": "No playlist found"}

        @server.route("/yt/playlists")
        def playlists():
            result = requestPlaylistInfo(dict(request.args))
            if isinstance(result, Response):
                return result
            return jsonify(result)

        @server.route("/yt/<endpoint>/", True)
        @server.cacheable
        def other(endpoint: str):
            params = dict(request.args)
            params["key"] = server.ytapi_key
            url = f"https://www.googleapis.com/youtube/v3/{endpoint}?" + urlencode(params)

            response = requests.get(url)
            if response.status_code != 200:
                return server.errorResponse(response.status_code, f"{response.reason}\n{response.text}")

            return jsonify(response.json())

        def requestStreamUrl(id: str) -> str | Response:
            DEFAULT_CLIENT = {"clientName":"ANDROID","clientVersion":"16.50","visitorData":None,"hl":DEFAULT_LANGUAGE}
            NO_CONTENT_WARNING_CLIENT = {"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0","visitorData":None,"hl":DEFAULT_LANGUAGE}

            for client in (DEFAULT_CLIENT, NO_CONTENT_WARNING_CLIENT):
                response = requests.post(f"https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8", json = {
                        "context": {
                            "client": client
                        },
                        "videoId": id,
                        "playlistId": None
                    }
                )

                if response.status_code != 200:
                    return server.errorResponse(response.status_code, response.reason)

                data = response.json()
                if data["playabilityStatus"]["status"] == "OK":
                    for format in data["streamingData"]["adaptiveFormats"]:
                        if format["itag"] == 140:
                            return format["url"]

            if self.ytd is not None:
                info = self.ytd.extract_info(id, False)
                if info is not None:
                    for format in info["formats"]:
                        if format["format_id"] == "140":
                            return format["url"]

            return server.errorResponse(404)

        @server.route("/yt/streamurl", True)
        def streamUrl():
            id = request.args.get("id")
            if id is None:
                return server.errorResponse(400)

            result = requestStreamUrl(id)
            if isinstance(result, Response):
                return result
            return jsonify(result)

        @server.route("/yt/batch/", True, methods = ["POST"])
        def batch():
            try:
                data = json.loads(request.data)
            except json.JSONDecodeError as e:
                return server.errorResponse(400, e.msg)

            if not isinstance(data, list):
                return server.errorResponse(400, "Data must be a JSON list")

            def handleRequest(item: dict, params: dict):
                try:
                    params["id"] = item["id"]

                    match item["type"]:
                        case "video":
                            result = requestVideoInfo(params)
                        case "channel":
                            result = requestChannelInfo(params)
                        case "playlist":
                            result = requestPlaylistInfo(params)
                        case _:
                            return server.errorResponse(400, f"Invalid item type: {item['type']}")

                    if isinstance(result, Response):
                        return result

                    result["type"] = item["type"]

                    if item.get("get_stream_url"):
                        url = requestStreamUrl(item["id"])
                        result["stream_url"] = url if isinstance(url, str) else None

                    return result
                except KeyError as e:
                    return server.errorResponse(400, str(e))

            threads: list[ThreadWithReturnValue] = []
            ret = []

            for item in data:
                thread = ThreadWithReturnValue(target = handleRequest, args = (item, dict(request.args)))
                threads.append(thread)
                thread.start()

            error = None

            for thread in threads:
                result = thread.join()
                if isinstance(result, Response):
                    error = result
                else:
                    ret.append(result)

            if error is not None:
                return error

            return jsonify(ret)

        @server.route("/feed/", True)
        @server.cacheable
        def feed():

            def postRequest(ctoken: str | None) -> dict | Response:
                utils.log(request.args.get("interfaceLang", "bruh"))
                response =  requests.post(
                    "https://music.youtube.com/youtubei/v1/browse",
                    params = {"ctoken": ctoken, "continuation": ctoken, "type": "next"} if ctoken is not None else {},
                    headers = server.ytm_headers,
                    json = {
                        "context":{
                            "client":{
                                "hl": request.args.get("interfaceLang", DEFAULT_LANGUAGE),
                                "platform": "DESKTOP",
                                "clientName": "WEB_REMIX",
                                "clientVersion": "1.20221031.00.00-canary_control",
                                "userAgent": USER_AGENT,
                                "acceptHeader": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
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
                    return server.errorResponse(response.status_code, response.text)

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
                                try:
                                    item_entry["id"] = item["navigationEndpoint"]["watchEndpoint"]["videoId"]
                                    item_entry["type"] = "song"
                                    try:
                                        item_entry["playlist_id"] = item["navigationEndpoint"]["watchEndpoint"]["playlistId"]
                                    except KeyError:
                                        pass
                                except KeyError:
                                    try:
                                        if "watchPlaylistEndpoint" in item["navigationEndpoint"]:
                                            item_entry["type"] = "playlist"
                                            item_entry["id"] = item["navigationEndpoint"]["watchPlaylistEndpoint"]["playlistId"]
                                        else:
                                            item_type = item["navigationEndpoint"]["browseEndpoint"]["browseEndpointContextSupportedConfigs"]["browseEndpointContextMusicConfig"]["pageType"]
                                            match item_type:
                                                case "MUSIC_PAGE_TYPE_ALBUM" | "MUSIC_PAGE_TYPE_PLAYLIST":
                                                    item_entry["type"] = "playlist"
                                                case "MUSIC_PAGE_TYPE_ARTIST":
                                                    item_entry["type"] = "artist"
                                                case _:
                                                    raise RuntimeError(item_type)

                                            item_entry["id"] = item["navigationEndpoint"]["browseEndpoint"]["browseId"]

                                            # if item_entry["id"] == "LM" or "RDTMAK5uy_" in item_entry["id"]:
                                            #     item_entry = None
                                            if item_entry["id"].startswith("MPREb_"):
                                                r = requests.get(
                                                    f"https://music.youtube.com/browse/{item_entry['id']}",
                                                    headers = {
                                                        "Cookie": "CONSENT=YES+1",
                                                        "User-Agent": USER_AGENT
                                                    }
                                                )
                                                r.raise_for_status()

                                                target = "urlCanonical\\x22:\\x22https:\\/\\/music.youtube.com\\/playlist?list\\x3d"
                                                pos = r.text.find(target) + len(target)
                                                end = r.text.find("\\", pos + 1)

                                                item_entry["id"] = r.text[pos:end]

                                    except KeyError:
                                        raise RuntimeError(json.dumps(item, indent="\t"))

                                if item_entry is not None:
                                    items.append(item_entry)

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
                return server.errorResponse(400, "Invalid minRows parameter")

            data = postRequest(None)
            if isinstance(data, Response):
                return data

            rows = processRows(data["contents"])

            while len(rows) < min_rows:
                continuations = data.get("continuations", None)
                if continuations is None:
                    break
                ctoken = data["continuations"][0]["nextContinuationData"]["continuation"]
                data = postRequest(ctoken)
                if isinstance(data, Response):
                    return data

                rows += processRows(data["contents"])

            return jsonify(rows)

    def ensureCorrectChannelId(self, channel_id: str):
        try:
            return self.ytmusic.get_artist(channel_id)["channelId"]
        except Exception:
            return channel_id

    # def isChannelAutogenerated(self, channel_id: str):
    #     response = requests.get(f"https://www.youtube.com/channel/{channel_id}/about", headers={"Cookie": "CONSENT=YES+1"})
    #     response.raise_for_status()

    #     try:
    #         for script in BeautifulSoup(response.text).find_all("script"):
    #             if script.text.startswith("var ytInitialData = {"):
    #                 data = json.loads(script.text[20:-1])
    #                 url: str = data["contents"]["twoColumnBrowseResultsRenderer"]["tabs"][2]["tabRenderer"]["content"]["sectionListRenderer"]["contents"][0]["itemSectionRenderer"]["contents"][0]["channelAboutFullMetadataRenderer"]["primaryLinks"][0]["navigationEndpoint"]["urlEndpoint"]["url"]

    #                 if not url.startswith("https://www.youtube.com/redirect?event=channel_description&redir_token="):
    #                     return False

    #                 return parse_qs(urlparse(url).query)["q"][0] == "https://support.google.com/youtube/answer/2579942"
    #     except (IndexError, KeyError):
    #         return False

    def getMusicChannelInfo(self, channel_id: str, hl: str) -> dict:
        response = requests.post(
            "https://music.youtube.com/youtubei/v1/browse",
            json = {"browseId": channel_id, "context": {"client": {"clientName": "WEB_REMIX", "clientVersion": "1.20221228.01.00", "hl": hl}, "user": {}}},
            headers = {"Cookie": "CONSENT=YES+1"}
        )

        if response.status_code != 200:
            return {}

        try:
            data = response.json()["header"]["musicImmersiveHeaderRenderer"]
        except (requests.exceptions.JSONDecodeError, KeyError):
            return {}

        ret = {}
        for key in ("title", "description"):
            if key in data:
                try:
                    ret[key] = data[key]["runs"][0]["text"]
                except (KeyError, IndexError):
                    pass

        return ret
