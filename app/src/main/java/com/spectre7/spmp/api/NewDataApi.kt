package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.*
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class NewDataApi {

    data class Result<T>(
        private val _data: T?,
        private val error: Exception? = null
    ) {
        val data: T get() = _data!!
        val exception: Exception get() = error!!
        val success: Boolean = error == null
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"
        private val client = OkHttpClient()
        private val klaxon: Klaxon = Klaxon()

        private fun getYTMHeaders(): Headers {
            val headers = Headers.Builder()
            headers.add("user-agent", USER_AGENT)
            headers.add("accept", "*/*")
            headers.add("accept-language", "en")
            headers.add("content-type", "application/json")
            headers.add("x-goog-visitor-id", "Cgt1TjR0ckUtOVNXOCiKnOmaBg%3D%3D")
            headers.add("x-youtube-client-name", "67")
            headers.add("x-youtube-client-version", "1.20221019.01.00")
            headers.add("authorization", "SAPISIDHASH 1666862603_ad3286857ed8177c1e0f0f16fc678aaff93ad310")
            headers.add("x-goog-authuser", "1")
            headers.add("x-origin", "https://music.youtube.com")
            headers.add("origin", "https://music.youtube.com")
            headers.add("alt-used", "music.youtube.com")
            headers.add("connection", "keep-alive")
            headers.add("cookie", "PREF=tz=Europe.London&f6=40000400&f5=30000&f7=1&repeat=NONE&volume=60&f3=8&autoplay=true; __Secure-3PSIDCC=AIKkIs1HzUCBLGiDGnM7upTqnkIuJFGKsO09NZKhr-6HF3VwRiHeeGNYNNo2Lhk1dduN8P27ZXy9; s_gl=GB; LOGIN_INFO=AFmmF2swRAIgZ035p6PjI532M15GF53l6UlfPen5HwkDpu7ZEle29vACIGNtXbi8xtRJ7Y8pT1tqah7SqKR_GnzcwOryhVxgUeXF:QUQ3MjNmel9JRGpUeGowRmpmM3picUpNalFleGFibkRYV1dubXdXenQyam9Ib3RWY3MtTVhUZmxDb1pFMUhoVElZUEdqS2JPcW5kT0dpaTN3emRUUUo5SU9ZRFFyVnlyZW9aYlF5dmVCQ1puYjRMRkd4OXFXb0s2Nlk4a1NtNVlfb3QydENNZDJ4bWlfSDVlZnZONHNSRk95dGxyeWZpV1dn; CONSENT=PENDING+281; __Secure-YEC=Cgt2ZlpYajN4dVdLZyjSmpuaBg%3D%3D; SIDCC=AIKkIs2pSVZXshn1zeCzrzL3mlIC6VAAgWfoULSkTBWcrht_9EMrkr8D9EQZYcCiKRDa8ejUTw; __Secure-1PSIDCC=AIKkIs0_imP3kQ3wfQWUyWhD_IKDL_QYExRxV4Ou7EpSO75uDq-4J6t3VhJOJGx1dM0zGdI3cpc; VISITOR_INFO1_LIVE=uN4trE-9SW8; wide=1; __Secure-3PSID=PwhomEhQTZ77kJmEhSDm0D3ui-d5WWiRyRhTGsP7BAyxF_dlxCTncdVXtBbp04fUJlDtPw.; __Secure-3PAPISID=qMwfMfR_YyoT3NGb/AzFZpb4NqFXud3Nwr; YSC=8LddSzq-F84; SID=PwhomEhQTZ77kJmEhSDm0D3ui-d5WWiRyRhTGsP7BAyxF_dlBdrU3vl6GPsCr1ylPTr4KQ.; __Secure-1PSID=PwhomEhQTZ77kJmEhSDm0D3ui-d5WWiRyRhTGsP7BAyxF_dlKcjSgx1HUrI2I9zInQMtxw.; HSID=Aco1DxTh4I1ySKm8Q; SSID=A1vzE52cm5ko7nyff; APISID=W6YIE8FP4wiEER0O/AbLbtnGAFqeU0gqza; SAPISID=qMwfMfR_YyoT3NGb/AzFZpb4NqFXud3Nwr; __Secure-1PAPISID=qMwfMfR_YyoT3NGb/AzFZpb4NqFXud3Nwr")
            headers.add("sec-fetch-dest", "empty")
            headers.add("sec-fetch-mode", "same-origin")
            headers.add("sec-fetch-site", "same-origin")
            headers.add("pragma", "no-cache")
            headers.add("cache-control", "no-cache")
            headers.add("te", "trailers")
            return headers.build()
        }

        data class HomeFeedRow(val title: String, val subtitle: String?, val browse_id: String?, val items: List<Item>) {
            data class Item(val type: String, val id: String, val playlist_id: String? = null) {
                fun getPreviewable(): YtItem {
                    when (type) {
                        "song" -> return Song.fromId(id)
                        "artist" -> return Artist.fromId(id)
                        "playlist" -> return Playlist.fromId(id)
                    }
                    throw RuntimeException(type)
                }
            }
        }

        private class HomeFeedRowRaw(
            val musicCarouselShelfRenderer: MusicCarouselShelfRenderer
        ) {
            class MusicCarouselShelfRenderer(val header: Header, val contents: List<ContentsItem>)

            class Header(val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer)
            class MusicCarouselShelfBasicHeaderRenderer(val title: Runs)
            class Runs(val runs: List<Title>)
            class Title(val text: String, val strapline: Runs? = null, val navigationEndpoint: NavigationEndpoint? = null)

            class ContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null)

            class MusicTwoRowItemRenderer(val navigationEndpoint: NavigationEndpoint)
            class NavigationEndpoint(val watchEndpoint: WatchEndpoint? = null, val browseEndpoint: BrowseEndpoint? = null)
            class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
            class BrowseEndpoint(val browseId: String, val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null)
            class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
            class BrowseEndpointContextMusicConfig(val pageType: String)

            class MusicResponsiveListItemRenderer(val playlistItemData: PlaylistItemData)
            class PlaylistItemData(val videoId: String)

            val title: Title get() = musicCarouselShelfRenderer.header.musicCarouselShelfBasicHeaderRenderer.title.runs[0]
            val items get() = musicCarouselShelfRenderer.contents
        }

        fun getHomeFeed(min_rows: Int = -1): Result<List<HomeFeedRow>> {

            var error_response: Response? = null
            fun postRequest(ctoken: String?): JsonObject? {
                val url = "https://music.youtube.com/youtubei/v1/browse"
                val request = Request.Builder()
                    .url(if (ctoken == null) url else "$url?ctoken=$ctoken&continuation=$ctoken&type=next")
                    .headers(getYTMHeaders())
                    .post("""
                        {
                            "context":{
                                "client":{
                                    "hl": "${MainActivity.languages.keys.elementAt(Settings.get(Settings.KEY_LANG_UI))}",
                                    "platform": "DESKTOP",
                                    "clientName": "WEB_REMIX",
                                    "clientVersion": "1.20221031.00.00-canary_control",
                                    "userAgent": "$USER_AGENT",
                                    "acceptHeader": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                                },
                                "user":{
                                    "lockedSafetyMode": false
                                },
                                "request":{
                                    "useSsl": true,
                                    "internalExperimentFlags": [],
                                    "consistencyTokenJars": []
                                }
                            }
                        }
                    """.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.code != 200) {
                    error_response = response
                    return null
                }

                val parsed = klaxon.parseJsonObject(response.body!!.charStream())
                if (ctoken != null) {
                    return parsed.obj("continuationContents")!!.obj("sectionListContinuation")!!
                }
                else {
                    return parsed
                        .obj("contents")!!
                        .obj("singleColumnBrowseResultsRenderer")!!
                        .array<JsonObject>("tabs")!![0]
                        .obj("tabRenderer")!!
                        .obj("content")!!
                        .obj("sectionListRenderer")!!
                }
            }

            fun processRows(rows: List<HomeFeedRowRaw>): List<HomeFeedRow> {

                fun getItem(item: HomeFeedRowRaw.ContentsItem): HomeFeedRow.Item {
                    if (item.musicTwoRowItemRenderer != null) {
                        val _item = item.musicTwoRowItemRenderer

                        // Video
                        if (_item.navigationEndpoint.watchEndpoint?.videoId != null) {
                            return HomeFeedRow.Item(
                                "song",
                                _item.navigationEndpoint.watchEndpoint.videoId,
                                _item.navigationEndpoint.watchEndpoint.playlistId
                            )
                        }

                        // Playlist or artist
                        val item_type = when (_item.navigationEndpoint.browseEndpoint!!.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType) {
                            "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST" -> "playlist"
                            "MUSIC_PAGE_TYPE_ARTIST" -> "artist"
                            else -> throw NotImplementedError(_item.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType)
                        }

                        var item_id = _item.navigationEndpoint.browseEndpoint.browseId
                        if (item_id.startsWith("MPREb_")) {

                            val request = Request.Builder()
                                .url("https://music.youtube.com/browse/$item_id")
                                .header("Cookie", "CONSENT=YES+1")
                                .header("User-Agent", USER_AGENT)
                                .build()

                            val result = client.newCall(request).execute()
                            if (result.code != 200) {
                                throw RuntimeException("${result.message} | ${result.body?.string()}")
                            }

                            val text = result.body!!.string()

                            val target = "urlCanonical\\x22:\\x22https:\\/\\/music.youtube.com\\/playlist?list\\x3d"
                            val start = text.indexOf(target) + target.length
                            val end = text.indexOf("\\", start + 1)

                            item_id = text.substring(start, end)
                        }

                        return HomeFeedRow.Item(
                            item_type,
                            item_id,
                            null
                        )
                    }
                    else if (item.musicResponsiveListItemRenderer != null) {
                        return HomeFeedRow.Item(
                            "song",
                            item.musicResponsiveListItemRenderer.playlistItemData.videoId,
                            null
                        )
                    }
                    else {
                        throw NotImplementedError()
                    }
                }

                return List(rows.size) { i ->
                    val row = rows[i]

                    HomeFeedRow(
                        row.title.text,
                        row.title.strapline?.runs?.get(0)?.text,
                        row.title.navigationEndpoint?.browseEndpoint?.browseId,
                        List(row.items.size) { j -> getItem(row.items[j]) }
                    )
                }
            }

            var data = postRequest(null) ?: return Result(null, RuntimeException(error_response?.message))
            val rows = processRows(klaxon.parseFromJsonArray(data.array<JsonObject>("contents")!!)!!).toMutableList()

            while (min_rows >= 1 && rows.size < min_rows) {
                val ctoken = data
                    .array<JsonObject>("continuations")
                    ?.get(0)
                    ?.obj("nextContinuationData")
                    ?.string("continuation")
                    ?: break

                data = postRequest(ctoken) ?: return Result(null, RuntimeException(error_response?.message))
                rows.addAll(processRows(klaxon.parseFromJsonArray(data.array<JsonObject>("contents")!!)!!))
            }

            return Result(rows)
        }
    }
}