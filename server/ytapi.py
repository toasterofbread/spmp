from functools import wraps
from urllib.parse import urlencode, urlparse, parse_qs
from flask import Flask, request, jsonify
from flask.wrappers import Response
import requests
import json
from bs4 import BeautifulSoup
from ytmusicapi import YTMusic
from spectre7 import utils

class YtApi:

    def __init__(self, app: Flask, server):

        def ytApiEndpoint(endpoint: str, make_route: bool = False):
            def wrapper(func):

                def function(*args, **kwargs):
                    params = dict(request.args)
                    params["key"] = server.ytapi_key
                    url = f"https://www.googleapis.com/youtube/v3/{endpoint}?" + urlencode(params)

                    cached = server.getCache("yt", url)
                    if cached is not None:
                        return jsonify(cached)

                    ret: Response = func(*args, **kwargs)
                    if ret.status_code == 200:
                        server.setCache("yt", url, ret.get_json())

                    return ret

                if make_route:
                    @server.route(f"/yt/{endpoint}/", True)
                    @server.cacheable
                    @wraps(func)
                    def decorated(*args, **kwargs):
                        return function(*args, **kwargs)
                else:
                    @server.cacheable
                    @wraps(func)
                    def decorated(*args, **kwargs):
                        return function(*args, **kwargs)

                return decorated
            return wrapper

        @ytApiEndpoint("videos")
        def videos():
            params = dict(request.args)
            # loc = params.pop("localisation", None)

            params["key"] = server.ytapi_key
            url = f"https://www.googleapis.com/youtube/v3/videos?" + urlencode(params)

            response = requests.get(url)
            if response.status_code != 200:
                return server.errorResponse(response.status_code, f"{response.reason}\n{response.text}")

            data = response.json()
            # if loc:
            #     for item in data["items"]:
            #         if "snippet" in item and "localizations" in item and loc in item["localizations"]:
            #             localisation = item["localizations"][loc]
            #             for key in localisation:
            #                 item["snippet"][key] = localisation[key]

            return jsonify(data)

        @ytApiEndpoint("channels")
        def channels():
            params = dict(request.args)
            params["key"] = server.ytapi_key

            # if "id" in params:
            #     params["id"] = self.ensureCorrectChannelId(params["id"])

            url = f"https://www.googleapis.com/youtube/v3/channels?" + urlencode(params)

            response = requests.get(url)
            if response.status_code != 200:
                return server.errorResponse(response.status_code, f"{response.reason}\n{response.text}")

            return jsonify(response.json())

        @server.route("/yt/<endpoint>/", True)
        def other(endpoint: str):

            @ytApiEndpoint(endpoint, False)
            def wrapped():
                params = dict(request.args)
                params["key"] = server.ytapi_key
                url = f"https://www.googleapis.com/youtube/v3/{endpoint}?" + urlencode(params)

                response = requests.get(url)
                if response.status_code != 200:
                    return server.errorResponse(response.status_code, f"{response.reason}\n{response.text}")

                return jsonify(response.json())

            return wrapped()

        @server.route("/feed/", True)
        @server.cacheable
        def feed():

            def postRequest(ctoken: str | None) -> dict | Response:
                response =  requests.post(
                    "https://music.youtube.com/youtubei/v1/browse",
                    params = {"ctoken": ctoken, "continuation": ctoken, "type": "next"} if ctoken is not None else {},
                    headers = server.ytm_headers,
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
            if not self.isChannelAutogenerated(channel_id):
                return channel_id
            return YTMusic().get_artist(channel_id)["channelId"]
        except Exception:
            return channel_id

    def isChannelAutogenerated(self, channel_id: str):
        response = requests.get(f"https://www.youtube.com/channel/{channel_id}/about", headers={"Cookie": "CONSENT=YES+1"})
        response.raise_for_status()

        try:
            for script in BeautifulSoup(response.text).find_all("script"):
                if script.text.startswith("var ytInitialData = {"):
                    data = json.loads(script.text[20:-1])
                    url: str = data["contents"]["twoColumnBrowseResultsRenderer"]["tabs"][2]["tabRenderer"]["content"]["sectionListRenderer"]["contents"][0]["itemSectionRenderer"]["contents"][0]["channelAboutFullMetadataRenderer"]["primaryLinks"][0]["navigationEndpoint"]["urlEndpoint"]["url"]

                    if not url.startswith("https://www.youtube.com/redirect?event=channel_description&redir_token="):
                        return False

                    return parse_qs(urlparse(url).query)["q"][0] == "https://support.google.com/youtube/answer/2579942"
        except (IndexError, KeyError):
            return False
