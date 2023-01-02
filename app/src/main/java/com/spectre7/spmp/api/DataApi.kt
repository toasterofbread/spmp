package com.spectre7.spmp.api

import com.beust.klaxon.*
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.layout.ResourceType
import com.spectre7.utils.getString
import com.spectre7.utils.toInt
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread

class DataApi {

    data class LyricsSearchResult(
        val id: String,
        val name: String,
        val artist_id: String?,
        val artist_name: String?,
        val album_id: String?,
        val album_name: String?
    ) {
        val source: Song.Lyrics.Source = Song.Lyrics.Source.getFromString(id.split(':', limit = 2)[0])
    }

    companion object {
        init {
            MainActivity.network.addRetryCallback {
                thread {
                    processYtItemLoadQueue()
                }
            }
        }

        private var _ytapi: PyObject? = null
        private val ytapi: PyObject?
            get() {
                if (_ytapi != null) {
                    return _ytapi
                }

                try {
                    _ytapi = Python.getInstance().getModule("ytmusicapi").callAttr("YTMusic").apply { callAttr("setup", null, getString(R.string.yt_music_creds)) }
                }
                catch (_: PyException) {}

                return _ytapi
            }

        private val klaxon: Klaxon = Klaxon()
        private var server: ServerAccessPoint? = null

        data class YtItemLoadQueueKey(val id: String, val type: String) {
            var get_stream_url: Boolean = false
            companion object {
                fun from(item: YtItem, get_stream_url: Boolean): YtItemLoadQueueKey {
                    return YtItemLoadQueueKey(
                        item.id,
                        when (item) {
                            is Song -> "video"
                            is Artist -> "channel"
                            is Playlist -> "playlist"
                            else -> throw RuntimeException(item.toString())
                        }
                    ).also {
                        it.get_stream_url = get_stream_url
                    }
                }
            }
        }
        private val ytitem_load_queue: MutableMap<YtItemLoadQueueKey, MutableList<(YtItem.ServerInfoResponse?) -> Unit>> = mutableMapOf()
        private val ytitem_load_mutex = Mutex()

        fun queueYtItemDataLoad(item: YtItem, get_stream_url: Boolean, callback: (YtItem.ServerInfoResponse?) -> Unit) {
            runBlocking {
                synchronized(ytitem_load_queue) {
                    ytitem_load_queue.getOrPut(YtItemLoadQueueKey.from(item, get_stream_url)) { mutableListOf() }.add(callback)
                }
            }
        }

        fun processYtItemLoadQueue() {
            runBlocking {
                ytitem_load_mutex.withLock {

                    val items: MutableMap<YtItemLoadQueueKey, MutableList<(YtItem.ServerInfoResponse?) -> Unit>>
                    synchronized(ytitem_load_queue) {
                        if (ytitem_load_queue.isEmpty()) {
                            return@withLock
                        }
                        items = ytitem_load_queue.toMutableMap()
                        ytitem_load_queue.clear()
                    }

                    val result: String
                    try {
                        result = queryServer(
                            "/yt/batch",
                            mapOf("part" to "contentDetails,snippet,statistics"),
                            post_body = klaxon.toJsonString(items.keys),
                            throw_on_fail = true
                        )!!
                    }
                    catch (e: Exception) {
                        synchronized(ytitem_load_queue) {
                            ytitem_load_queue.putAll(items)
                        }
                        MainActivity.network.onError(e)
                        return@withLock
                    }

                    for (item in klaxon.parseArray<YtItem.ServerInfoResponse>(result)!!) {
                        for (callback in items.remove(YtItemLoadQueueKey(item.original_id!!, item.type))!!) {
                            callback(item)
                        }
                    }
                }
            }
        }

        data class SearchResults(val items: List<Result>) {
            data class Result(val id: ResultId, val snippet: YtItem.ServerInfoResponse.Snippet)
            data class ResultId(val kind: String, val videoId: String = "", val channelId: String = "", val playlistId: String = "")
        }

        fun search(query: String, type: ResourceType, max_results: Int = 10, channel_id: String? = null): List<SearchResults.Result> {

            val parameters = mutableMapOf(
                "part" to "snippet",
                "type" to when (type) {
                    ResourceType.SONG -> "video"
                    ResourceType.ARTIST -> "channel"
                    ResourceType.PLAYLIST -> "playlist"
                },
                "q" to query,
                "maxResults" to max_results.toString(),
                "safeSearch" to "none"
            )
            if (channel_id != null) {
                parameters.put("channelId", channel_id)
            }
            val result = queryServer("/yt/search", parameters)
            return klaxon.parse<SearchResults>(result!!)!!.items
        }

        class GetDownloadUrlResult(val playabilityStatus: PlayabilityStatus, val streamingData: StreamingData? = null) {
            class StreamingData(val adaptiveFormats: List<Format>)
            class Format(val itag: Int?, val url: String? = null)
            class PlayabilityStatus(val status: String, val reason: String? = null)

            fun isPlayable(): Boolean {
                return playabilityStatus.status == "OK"
            }

            fun getError(): String {
                return playabilityStatus.reason.toString()
            }
        }

        fun getStreamUrl(id: String, callback: (url: String?) -> Unit) {
            thread {
                val result = queryServer("/yt/streamurl", mapOf("id" to id))!!
                data class URL(val url: String)
                MainActivity.runInMainThread {
                    callback(klaxon.parse<URL>("{\"url\": $result}")?.url)
                }
            }
        }

        fun getLyrics(id: String): Song.Lyrics? {
            val result = queryServer(
                "/get_lyrics/",
                mapOf("id" to id),
                max_retries = 1,
                throw_on_fail = false
            ) ?: return null

            return klaxon.converter(object : Converter {
                override fun canConvert(cls: Class<*>): Boolean {
                    return cls == Song.Lyrics.Source::class.java
                }

                override fun fromJson(jv: JsonValue): Any? {
                    if (jv.string == null) {
                        return null
                    }
                    return Song.Lyrics.Source.getFromString(jv.string!!.split(':', limit = 2)[0])
                }

                override fun toJson(value: Any): String {
                    return (value as Song.Lyrics.Source).string_code
                }

            }).parse<Song.Lyrics>(result)
        }

        fun getSongLyrics(song: Song, callback: (Song.Lyrics?) -> Unit) {
            thread {
                val id = song.registry.overrides.lyrics_id
                val ret: Song.Lyrics?

                if (id != null) {
                    ret = getLyrics(id)
                }
                else {
                    val result = queryServer(
                        "/search_and_get_lyrics/",
                        mapOf("title" to song.title, "artist" to song.artist.name),
                        max_retries = 1,
                        throw_on_fail = false
                    )
                    if (result == null) {
                        callback(null)
                        return@thread
                    }

                    ret = klaxon.parse<Song.Lyrics>(result)
                }

                song.registry.overrides.lyrics_id = ret?.id
                callback(ret)
            }
        }

        fun searchForLyrics(title: String, artist: String?, callback: (List<LyricsSearchResult>?) -> Unit) {
            thread {
                try {
                    val params = mutableMapOf("title" to title)
                    if (artist != null) {
                        params["artist"] = artist
                    }

                    val result = queryServer(
                        "/search_lyrics/",
                        params,
                        max_retries = 1
                    )
                    if (result == null) {
                        callback(null)
                        return@thread
                    }

                    callback(klaxon.parseArray(result))
                }
                catch (e: Exception) {
                    MainActivity.network.onError(e)
                    callback(null)
                }
            }
        }

        fun getSongCounterpartId(song: Song): String? {
            if (ytapi == null) {
                return null
            }
            val counterpart = ytapi!!.callAttr("get_watch_playlist", song.id, null, 1).callAttr("get", "tracks").asList().first().callAttr("get", "conuterpart")
            if (counterpart == null) {
                return null
            }
            return counterpart.callAttr("get", "videoId").toString()
        }

        fun getSongRadio(song_id: String, include_first: Boolean = true): List<String> {
            val body = """
            {
                "enablePersistentPlaylistPanel": true,
                "isAudioOnly": true,
                "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
                "videoId": "${song_id}",
                "playlistId": "RDAMVM${song_id}",
                "watchEndpointMusicSupportedConfigs": {
                    "watchEndpointMusicConfig": {
                        "hasPersistentPlaylistPanel": true,
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
            """

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/next?alt=json&key=AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30")
                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0")
                .addHeader("accept", "*/*")
                .addHeader("accept-encoding", "gzip, deflate")
                .addHeader("content-encoding", "gzip")
                .addHeader("origin", "https://music.youtube.com")
                .addHeader("X-Goog-Visitor-Id", "CgtUYXUtLWtyZ3ZvTSj3pNWaBg%3D%3D")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val client = OkHttpClient().newBuilder()
                .addInterceptor(object : Interceptor {
                    override fun intercept(chain: Interceptor.Chain): Response {
                        val original = chain.request()
                        val authorized = original.newBuilder()
                            .addHeader("Cookie", "CONSENT=YES+1")
                            .build()
                        return chain.proceed(authorized)
                    }
                })
                .build()

            val response = client.newCall(request).execute()

            val songs = (Parser.default().parse(GZIPInputStream(response.body!!.byteStream())) as JsonObject)
                .obj("contents")!!
                .obj("singleColumnMusicWatchNextResultsRenderer")!!
                .obj("tabbedRenderer")!!
                .obj("watchNextTabbedResultsRenderer")!!
                .array<JsonObject>("tabs")!![0]
                .obj("tabRenderer")!!
                .obj("content")!!
                .obj("musicQueueRenderer")!!
                .obj("content")!!
                .obj("playlistPanelRenderer")!!
                .array<JsonObject>("contents")!!

            val offset = if (include_first) 0 else 1
            return List(songs.size - offset) {
                songs[it + offset].obj("playlistPanelVideoRenderer")!!.string("videoId")!!
            }
        }

        fun getSongRelated(song: Song) {
            // TODO : https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_watch_playlist
        }

        data class Language(val hl: String, val name: String)
        fun getLanguageList(): List<Language> {
            data class Item(val snippet: Language)
            data class LanguageResponse(val items: List<Item>)

            val response = klaxon.parse<LanguageResponse>(queryServer("/yt/i18nLanguages", mapOf("part" to "snippet"))!!)!!
            return List(response.items.size) { i ->
                response.items[i].snippet
            }
        }

        class ServerAccessPoint {
            private var url: String? = null
            private var integrated_server: PyObject? = null

            init {
                refresh()
            }

            private fun refresh() {
                integrated_server = PlayerServiceHost.service.getIntegratedServer()
                if (integrated_server != null) {
                    url = null
                    return
                }

                val request = Request.Builder()
                    .url("https://api.ngrok.com/tunnels")
                    .header("Authorization", "Bearer ${getString(R.string.ngrok_api_key)}")
                    .addHeader("Ngrok-Version", "2")
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                if (response.code != 200) {
                    throw RuntimeException()
                }

                val tunnels = klaxon.parse<NgrokTunnelListResponse>(response.body!!.string())?.tunnels
                url = tunnels?.get(0)?.public_url

                if (url == null) {
                    throw RuntimeException()
                }
            }

            private fun getRequest(endpoint: String, params: Map<String, String>, post_body: String?): Request {
                var formatted_endpoint = endpoint
                if (!formatted_endpoint.startsWith('/')) {
                    formatted_endpoint = "/$formatted_endpoint"
                }
                if (!formatted_endpoint.endsWith('/')) {
                    formatted_endpoint += '/'
                }

                var request_url = "$url$formatted_endpoint?key=${getString(R.string.server_api_key)}"
                for (param in params) {
                    request_url += "&${URLEncoder.encode(param.key, "UTF-8")}=${URLEncoder.encode(param.value, "UTF-8")}"
                }

                val request = Request.Builder().url(request_url)
                if (post_body != null) {
                    request.post(post_body.toRequestBody("application/json".toMediaType()))
                }

                return request.build()
            }

            fun performRequest(
                endpoint: String,
                params: Map<String, String>,
                post_body: String? = null,
                throw_on_fail: Boolean = true,
                max_retries: Int = 5,
                timeout: Long = 10,
                allow_refresh: Boolean = true
            ): String? {

                if (integrated_server != null) {
                    var formatted_endpoint = endpoint
                    if (!formatted_endpoint.startsWith('/')) {
                        formatted_endpoint = "/$formatted_endpoint"
                    }
                    if (!formatted_endpoint.endsWith('/')) {
                        formatted_endpoint += '/'
                    }

                    val ret = integrated_server!!.callAttr("performRequest", formatted_endpoint, params, post_body)?.toString()
                    if (ret == null && throw_on_fail) {
                        throw RuntimeException()
                    }
                    return ret
                }

                var error: String? = null
                var request: Request = getRequest(endpoint, params, post_body)
                var refreshed: Boolean = false

                fun request(): String? {
                    val ret: Response = OkHttpClient.Builder().readTimeout(timeout, TimeUnit.SECONDS).build().newCall(request).execute()
                    if (ret.code == 401) {
                        error = "Server API key is invalid (401)"
                    }
                    else if (ret.code == 404) {
                        if (allow_refresh && !refreshed) {
                            refresh()
                            refreshed = true
                            request = getRequest(endpoint, params, post_body)
                            return null
                        }
                        else {
                            error = "Ngrok tunnel may be invalid (404)"
                            return null
                        }
                    }
                    else if (ret.code != 200) {
                        error = "${ret.body?.string()} (${ret.code})"
                    }

                    return ret.body!!.string()
                }

                var result: String? = null
                for (i in 0 until max_retries + 1) {
                    error = null
                    result = request()
                    if (result != null) {
                        break
                    }
                }

                if (error != null && throw_on_fail) {
                    throw RuntimeException("Request to server failed. $error. Request URL: ${request.url}")
                }

                return result
            }
        }

        data class NgrokTunnelListResponse(val tunnels: List<Tunnel>) {
            data class Tunnel(val id: String, val public_url: String, val started_at: String, val tunnel_session: Session, var from_cache: Boolean = false)
            data class Session(val id: String)
        }

        fun queryServer(
            endpoint: String,
            params: Map<String, String> = mapOf(),
            post_body: String? = null,
            throw_on_fail: Boolean = true,
            max_retries: Int = 5,
            timeout: Long = 20
        ): String? {

            if (server == null) {
                try {
                    server = ServerAccessPoint()
                }
                catch (e: Exception) {
                    if (throw_on_fail) {
                        throw e
                    }
                    return null
                }
            }

            val localised_params: MutableMap<String, String> = HashMap(params)
            for (key in listOf(Pair(Settings.KEY_LANG_DATA, "dataLang"), Pair(Settings.KEY_LANG_UI, "interfaceLang"))) {
                if (!localised_params.containsKey(key.second)) {
                    localised_params[key.second] = MainActivity.languages.keys.elementAt(Settings.get(key.first))
                }
            }

            return server!!.performRequest(endpoint, localised_params, post_body, throw_on_fail, max_retries, timeout)

//            var server = server_access_point ?: getServer()
//            if (server == null) {
//                if (throw_on_fail) {
//                    throw RuntimeException("Failed to get Ngrok tunnel. Is the server running?")
//                }
//                return null
//            }
//
//            var formatted_endpoint = endpoint
//            if (!formatted_endpoint.startsWith('/')) {
//                formatted_endpoint = "/$formatted_endpoint"
//            }
//            if (!formatted_endpoint.endsWith('/')) {
//                formatted_endpoint += '/'
//            }
//
//            var url_suffix = "$formatted_endpoint?key=${getString(R.string.server_api_key)}"
//
//            for (key in listOf(Pair(Settings.KEY_LANG_DATA, "dataLang"), Pair(Settings.KEY_LANG_UI, "interfaceLang"))) {
//                if (!parameters.containsKey(key.second)) {
//                    val value = MainActivity.languages.keys.elementAt(Settings.get(key.first))
//                    url_suffix += "&${key.second}=$value"
//                }
//            }
//
//            for (param in parameters) {
//                url_suffix += "&${URLEncoder.encode(param.key, "UTF-8")}=${URLEncoder.encode(param.value, "UTF-8")}"
//            }
//
//            fun getRequest(server: ServerAccessPoint): Request {
//                val url = "${server.url}$url_suffix"
//
//                val request = Request.Builder().url(url)
//                if (post_body != null) {
//                    request.post(post_body.toRequestBody("application/json".toMediaType()))
//                }
//                if (server.integrated) {
//                    request.header("Connection", "close")
//                }
//                return request.build()
//            }
//
//            var request = getRequest(server)
//            var result: Response? = null
//            var cancel: Boolean = false
//            var exception: Exception? = null
//
//            fun getResult(): Response {
//                val ret: Response = OkHttpClient.Builder().readTimeout(timeout, TimeUnit.SECONDS).build().newCall(request).execute()
//                if (ret.code == 401) {
//                    cancel = true
//                    throw RuntimeException("Server API key is invalid (401)")
//                }
//                else if (ret.code == 404 && server!!.from_cache) {
//                    server = getServer(true)
//                    if (server != null) {
//                        request = getRequest(server!!)
//                    }
//                    cancel = true
//                    throw RuntimeException("Ngrok tunnel may be invalid (404)")
//                }
//                else if (ret.code != 200) {
//                    throw RuntimeException("${ret.body?.string()} (${ret.code})")
//                }
//                return ret
//            }
//
//            for (i in 0 until max_retries + 1) {
//                try {
//                    result = getResult()
//                }
//                catch(e: Exception) {
//                    exception = e
//                }
//                if (result != null || server == null || cancel) {
//                    break
//                }
//            }
//
//            if (result == null && throw_on_fail) {
//                throw RuntimeException("Request to server failed. ${exception?.message}. Request URL: ${request.url}.")
//            }
//
//            return result?.body
        }

        data class RecommendedFeedRow(val title: String, val subtitle: String?, val browse_id: String?, val items: List<Item>) {
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

        fun getRecommendedFeed(allow_cached: Boolean = true): List<RecommendedFeedRow> {
            val data = queryServer("/feed", mapOf("noCache" to (!allow_cached).toInt().toString()), timeout=30)!!
            return klaxon.parseArray(data)!!
        }
    }
}

