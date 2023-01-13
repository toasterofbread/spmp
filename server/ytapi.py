from urllib.parse import urlencode
from flask import Flask, request, jsonify, redirect
from flask.wrappers import Response
import requests
import json
from ytmusicapi import YTMusic
from spectre7 import utils
from threading import Thread

# TODO | Should probably split this

USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"
DEFAULT_LANGUAGE = "en"

class ThreadWithReturnValue(Thread):
    def __init__(self, group=None, target=None, name=None, args=(), kwargs={}):
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
            cache_key = params["id"] + str(language)

            try:
                cache = server.getCache("requestVideoInfo", cache_key)
            except KeyError:
                return server.errorResponse(400)

            if cache is not None:
                utils.info("requestVideoInfo using cached value")
                return cache

            params["key"] = server.ytapi_key

            remove_localisations = False

            if not "part" in params:
                params["part"] = "contentDetails,snippet,statistics"
            elif language is not None and not "localizations" in params["part"]:
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

            server.setCache("requestVideoInfo", cache_key, ret)

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
            cache_key = params["id"] + str(language)

            original_id = params["id"]
            params["id"] = self.ensureCorrectChannelId(original_id)

            if params["id"] is None:
                raise RuntimeError()

            cache = server.getCache("requestChannelInfo", cache_key)
            if cache is not None:
                utils.info("requestChannelInfo using cached value")
                cache["original_id"] = original_id
                return cache

            params["key"] = server.ytapi_key

            remove_localisations = False
            if not "part" in params:
                params["part"] = "contentDetails,snippet,statistics"
            elif language is not None and not "localizations" in params["part"]:
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

            server.setCache("requestChannelInfo", cache_key, ret)

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
            cache_key = params["id"] + str(language)

            cache = server.getCache("requestPlaylistInfo", cache_key)
            if cache is not None:
                utils.info("requestPlaylistInfo using cached value")
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
            else:
                params["part"] = "contentDetails,snippet"

            if not "playlistId" in params and "id" in params:
                params["playlistId"] = params["id"]

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

                server.setCache("requestPlaylistInfo", cache_key, ret)

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

            redirect_to_stream = request.args.get("redirect", "0") == "1"

            result = requestStreamUrl(id)
            if isinstance(result, Response):
                return result

            return redirect(result) if redirect_to_stream else jsonify(result)

        @server.route("/yt/batch/", True, methods = ["POST"])
        def batch():

            try:
                data = json.loads(request.data)
            except json.JSONDecodeError as e:
                print("AAAAAAAAA")
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

                    # if item.get("get_stream_url"):
                    #     url = requestStreamUrl(item["id"])
                    #     result["stream_url"] = url if isinstance(url, str) else None

                    return result
                except KeyError as e:
                    print("BBBBBBBB")
                    return server.errorResponse(400, str(e))

            threads: list[ThreadWithReturnValue] = []
            ret = []

            print(data)

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

        @server.route("/yt/radio/", False)
        def radio():
            video_id = request.args.get("id")
            load_data: bool = request.args.get("loadData", "0") == "1"

            body = {
                "enablePersistentPlaylistPanel": True,
                "isAudioOnly": True,
                "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
                "videoId": video_id,
                "playlistId": f"RDAMVM{video_id}",
                "watchEndpointMusicSupportedConfigs": {
                    "watchEndpointMusicConfig": {
                        "hasPersistentPlaylistPanel": True,
                        "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                    }

                },
                "context" : {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20221023.01.00",
                        "hl": "ja"
                    },
                    "user": {}
                }
            }

            response = requests.post(
                "https://music.youtube.com/youtubei/v1/next",
                params = {"alt": "json", "key": "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"},
                headers = {
                    "accept": "*/*",
                    "accept-encoding": "gzip, deflate",
                    "content-encoding": "gzip",
                    "origin": "https://music.youtube.com",
                    "X-Goog-Visitor-Id": "CgtUYXUtLWtyZ3ZvTSj3pNWaBg%3D%3D",
                    "Content-type": "application/json"
                },
                cookies = {"CONSENT": "YES+1"},
                data = json.dumps(body)
            )

            response.raise_for_status()

            def get(cont, keys: tuple):
                ret = cont
                for key in keys:
                    if key is None:
                        ret = next(iter(ret.values()))
                    else:
                        try:
                            ret = ret[key]
                        except (IndexError, KeyError):
                            return None
                return ret

            radio = get(response.json(), (
                "contents",
                None, # singleColumnMusicWatchNextResultsRenderer
                None, # tabbedRenderer
                None, # watchNextTabbedResultsRenderer
                None, # tabs
                0,
                None, # tabRenderer
                "content",
                None, # musicQueueRenderer
                None, # content
                None, # playlistPanelRenderer
                "contents"
            ))

            if not load_data or radio is None:
                return jsonify({"radio": radio})

            request_params = {"dataLang": request.args.get("dataLang", None), "part": "contentDetails,snippet,statistics"}

            videos = {}
            channels = {}

            for video in radio:
                request_params["id"] = video["playlistPanelVideoRenderer"]["videoId"]

                thread = ThreadWithReturnValue(target = requestVideoInfo, args = (request_params.copy(),))
                thread.start()
                videos[request_params["id"]] = thread

                # Get video artist
                for item in get(video, (None, "menu", None, "items")) or (): # Menu
                    item = get(item, (None, "navigationEndpoint", "browseEndpoint"))

                    if item is not None and item["browseEndpointContextSupportedConfigs"]["browseEndpointContextMusicConfig"]["pageType"] == "MUSIC_PAGE_TYPE_ARTIST":

                        if not item["browseId"] in channels:
                            request_params["id"] = item["browseId"]

                            thread = ThreadWithReturnValue(target = requestChannelInfo, args = (request_params.copy(),))
                            thread.start()
                            channels[item["browseId"]] = thread

                        break

            error = None

            for cont in (videos, channels):
                for key in cont:
                    result = cont[key].join()
                    if isinstance(result, Response):
                        error = result
                    else:
                        cont[key] = result

            if error is not None:
                return error

            return jsonify({"radio": radio, "videos": videos, "channels": channels})

        @server.route("/feed/", True)
        @server.cacheable
        def feed():

            load_data: bool = request.args.get("loadData", "0") == "1"

            try:
                min_rows = int(request.args.get("minRows", -1))
            except ValueError:
                return server.errorResponse(400, "Invalid minRows parameter")

            def postRequest(ctoken: str | None) -> dict | Response:
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

            ret = {"feed": rows}

            if not load_data:
                return jsonify(ret)

            videos = []
            channels = []
            playlists = []

            for row in rows:
                for item in row["items"]:
                    match item["type"]:
                        case "song":
                            videos.append(item["id"])
                        case "artist":
                            channels.append(item["id"])
                        case _:
                            playlists.append(item["id"])

            data = fetchData(videos, channels, playlists)

            if isinstance(data, Response):
                return data

            ret.update(data)
            return jsonify(ret)

        def fetchData(video_ids: list[str], channel_ids: list[str], playlist_ids: list[str]) -> dict | Response:
            request_params = {"dataLang": request.args.get("dataLang", None)}

            containers = {}

            def fetchIds(container_key: str, ids: list[str], requester):
                if len(ids) == 0:
                    return

                if not container_key in containers:
                    container = {}
                    containers[container_key] = container
                else:
                    container = containers[container_key]

                for id in ids:
                    if id in container:
                        continue

                    request_params["id"] = id
                    thread = ThreadWithReturnValue(target = requester, args = (request_params.copy(),))
                    thread.start()
                    container[id] = thread

            def joinThreads():
                error = None
                for container in containers.values():
                    for id, thread in container.items():
                        if not isinstance(thread, ThreadWithReturnValue):
                            continue
                        result = thread.join()
                        if isinstance(result, Response):
                            error = result
                        else:
                            container[id] = result
                return error

            fetchIds("videos", video_ids, requestVideoInfo)
            fetchIds("channels", channel_ids, requestChannelInfo)
            fetchIds("playlists", playlist_ids, requestPlaylistInfo)

            error = joinThreads()
            if error is not None:
                return error

            video_channel_ids = []

            for video in containers.get("videos", {}).values():
                snippet = video.get("snippet")
                if snippet is None:
                    continue
                video_channel_ids.append(snippet["channelId"])

            fetchIds("channels", video_channel_ids, requestChannelInfo)

            error = joinThreads()
            if error is not None:
                return error

            return containers

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
