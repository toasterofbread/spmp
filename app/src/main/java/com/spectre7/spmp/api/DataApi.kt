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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TimedReceiver(val start: Long, val label: String) {
    fun finishTiming(extra: String = "") {
        val end = System.currentTimeMillis()
        println("$label $extra took ${(end - start) / 1000.0} seconds")
    }
}

fun <T> Timed(label: String, uses_callback: Boolean = false, body: TimedReceiver.() -> T): T {
    val start = System.currentTimeMillis()
    val receiver = TimedReceiver(start, label)

    val ret = body(receiver)
    if (!uses_callback) {
        receiver.finishTiming()
    }

    return ret
}

class DataApi {

    data class LyricsSearchResult(
        val id: String,
        val name: String,
        val sync: Int,
        val artist_id: String? = null,
        val artist_name: String? = null,
        val album_id: String? = null,
        val album_name: String? = null
    ) {
        val source: Song.Lyrics.Source = Song.Lyrics.Source.getFromString(id.split(':', limit = 2)[0])
        val sync_type: Song.Lyrics.SyncType
            get() = Song.Lyrics.SyncType.values()[sync]
    }

    data class RadioResult(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer) {
        data class PlaylistPanelVideoRenderer(val videoId: String)
    }

    companion object {
        init {
            MainActivity.network.addRetryCallback {
                thread {
                    processYtItemLoadQueue()
                }
            }
        }

        private fun loadMediaItemsFromDataResult(data: String, onFinished: (data: Any) -> Unit) {
            val klaxon = Klaxon()
            val result = klaxon.parseJsonObject(klaxon.toReader(data.byteInputStream()))

            val videos = result.getOrDefault("videos", JsonObject()) as JsonObject
            val channels = result.getOrDefault("channels", JsonObject()) as JsonObject
            val playlists = result.getOrDefault("playlists", JsonObject()) as JsonObject

            var loading = videos.size + channels.size + playlists.size
            val onLoaded = {
                if (--loading <= 0) {
                    for (item in result) {
                        if (item.key == "videos" || item.key == "channels" || item.key == "playlists") {
                            continue
                        }
                        onFinished(item.value!!)
                        break
                    }
                }
            }

            if (loading == 0) {
                onLoaded()
                return
            }

            // Video init depends on channel, so init channels first
            for (channel in channels) {
                Artist.fromId(channel.key).initWithData(klaxon.parseFromJsonObject(channel.value as JsonObject)!!, false, onLoaded)
            }

            for (video in videos) {
                Song.fromId(video.key).initWithData(klaxon.parseFromJsonObject(video.value as JsonObject)!!, false, onLoaded)
            }

            for (playlist in playlists) {
                Playlist.fromId(playlist.key).initWithData(klaxon.parseFromJsonObject(playlist.value as JsonObject)!!, false, onLoaded)
            }

            thread {
                processYtItemLoadQueue()
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
            companion object {
                fun from(item: YtItem): YtItemLoadQueueKey {
                    return YtItemLoadQueueKey(
                        item.id,
                        when (item) {
                            is Song -> "video"
                            is Artist -> "channel"
                            is Playlist -> "playlist"
                            else -> throw RuntimeException(item.toString())
                        }
                    )
                }
            }
        }
        private val ytitem_load_queue: MutableMap<YtItemLoadQueueKey, MutableList<(YtItem.ServerInfoResponse?) -> Unit>> = mutableMapOf()
        private val ytitem_load_mutex = Mutex()

        fun queueYtItemDataLoad(item: YtItem, callback: (YtItem.ServerInfoResponse?) -> Unit) {
            runBlocking {
                synchronized(ytitem_load_queue) {
                    ytitem_load_queue.getOrPut(YtItemLoadQueueKey.from(item)) { mutableListOf() }.add(callback)
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

                    val result = requestServer(
                        "/yt/batch",
                        mapOf("part" to "contentDetails,snippet,statistics"),
                        post_body = klaxon.toJsonString(items.keys)
                    )

                    if (!result.success) {
                        synchronized(ytitem_load_queue) {
                            ytitem_load_queue.putAll(items)
                        }
                        MainActivity.network.onError(RuntimeException(result.getErrorMessage()))
                        return@withLock
                    }

                    for (item in klaxon.parseArray<YtItem.ServerInfoResponse>(result.body)!!) {
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

        fun search(request: String, type: ResourceType, max_results: Int = 10, channel_id: String? = null): List<SearchResults.Result> {
            val parameters = mutableMapOf(
                "part" to "snippet",
                "type" to when (type) {
                    ResourceType.SONG -> "video"
                    ResourceType.ARTIST -> "channel"
                    ResourceType.PLAYLIST -> "playlist"
                },
                "q" to request,
                "maxResults" to max_results.toString(),
                "safeSearch" to "none"
            )
            if (channel_id != null) {
                parameters.put("channelId", channel_id)
            }
            val result = requestServer("/yt/search", parameters)
            result.throwStatus()
            return klaxon.parse<SearchResults>(result.body)!!.items
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

        fun getStreamUrl(id: String): String? {
            val result = requestServer("/yt/streamurl", mapOf("id" to id))!!
            data class URL(val url: String)
            return klaxon.parse<URL>("{\"url\": $result}")?.url
        }

        fun getLyrics(id: String): Song.Lyrics? {
            val result = requestServer(
                "/get_lyrics/",
                mapOf("id" to id),
                max_retries = 1,
                throw_on_fail = false
            )

            if (!result.success) {
                return null
            }

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

            }).parse<Song.Lyrics>(result.body)
        }

        fun getSongLyrics(song: Song, callback: (Song.Lyrics?) -> Unit) {
            thread {
                val id = song.registry.overrides.lyrics_id
                val ret: Song.Lyrics?

                if (id != null) {
                    ret = getLyrics(id)
                }
                else {
                    val result = requestServer(
                        "/search_and_get_lyrics/",
                        mapOf("title" to song.title, "artist" to song.artist.name),
                        max_retries = 1,
                        throw_on_fail = false
                    )
                    if (!result.success) {
                        callback(null)
                        return@thread
                    }

                    ret = klaxon.parse<Song.Lyrics>(result.body)
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

                    val result = requestServer(
                        "/search_lyrics/",
                        params,
                        max_retries = 1
                    )
                    if (!result.success) {
                        callback(null)
                        return@thread
                    }

                    callback(klaxon.parseArray(result.body))
                }
                catch (e: Exception) {
                    MainActivity.network.onError(e)
                    callback(null)
                }
            }
        }

        fun getSongRadio(
            song_id: String,
            include_first: Boolean = true,
            load_data: Boolean = false,
            callback: (ServerRequestDataResult<List<String>>) -> Unit
        ) {
            val result = requestServer("/yt/radio/", mapOf("id" to song_id, "load_data" to load_data.toInt().toString()), timeout=30)

            if (!result.success) {
                callback(ServerRequestDataResult(result, null))
                return
            }

            loadMediaItemsFromDataResult(result.body) { data ->
                val items = klaxon.parseFromJsonArray<RadioResult>(data as JsonArray<*>)!!
                val offset = if (include_first) 0 else 1
                val radio = List(items.size - offset) {
                    items[it + offset].playlistPanelVideoRenderer.videoId
                }
                callback(ServerRequestDataResult(result, radio))
            }
        }

        fun getSongRelated(song: Song) {
            // TODO : https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_watch_playlist
        }

        data class Language(val hl: String, val name: String)

        open class ServerRequestResult(
            val body: String,
            val status: Int,
            val endpoint: String?
        ) {
            val success: Boolean
                get() = status == 200
            
            fun getErrorMessage(): String {
                return "Server request failed ($status, $endpoint):\n$body"
            }

            fun getException(): Exception {
                return RuntimeException(getErrorMessage())
            }

            fun throwStatus() {
                if (!success) {
                    throw getException()
                }
            }

            companion object {
                val NO_SERVER = ServerRequestResult("No server available", 404, null)
            }
        }

        class ServerRequestDataResult<DataType>(
            result: ServerRequestResult,
            data: DataType?
        ): ServerRequestResult(result.body, result.status, result.endpoint) {
            private var _data: DataType? = data
            val result: DataType
                get() = _data!!

            init {
                if (success && _data == null) {
                    throw IllegalArgumentException()
                }
            }
        }

        class ServerAccessPoint {
            private var url: String? = null
            private var integrated_server: PyObject? = null

            init {
                refresh()
            }

            fun isIntegrated(): Boolean {
                return integrated_server != null
            }

            fun getExternalRequestUrl(endpoint: String, params: Map<String, String>? = null): String {
                var formatted_endpoint = endpoint
                if (!formatted_endpoint.startsWith('/')) {
                    formatted_endpoint = "/$formatted_endpoint"
                }
                if (!formatted_endpoint.endsWith('/')) {
                    formatted_endpoint += '/'
                }

                var ret = "$url$formatted_endpoint?key=${getString(R.string.server_api_key)}"
                if (params != null) {
                    for (param in params) {
                        ret += "&${URLEncoder.encode(param.key, "UTF-8")}=${URLEncoder.encode(param.value, "UTF-8")}"
                    }
                }
                return ret
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

                val request = Request.Builder().url(getExternalRequestUrl(endpoint, params))
                if (post_body != null) {
                    request.post(post_body.toRequestBody("application/json".toMediaType()))
                }

                return request.build()
            }

            fun performRequest(
                endpoint: String,
                params: Map<String, String>,
                post_body: String? = null,
                max_retries: Int = 5,
                timeout: Long = 10,
                allow_refresh: Boolean = true
            ): ServerRequestResult {

                if (isIntegrated()) {
                    var formatted_endpoint = endpoint
                    if (!formatted_endpoint.startsWith('/')) {
                        formatted_endpoint = "/$formatted_endpoint"
                    }
                    if (!formatted_endpoint.endsWith('/')) {
                        formatted_endpoint += '/'
                    }

                    val result = integrated_server!!.callAttr("performRequest", formatted_endpoint, params, post_body)?.toString()
                    return ServerRequestResult(result ?: "Request to integrated server failed", if (result != null) 200 else 500, endpoint)
                }

                var status: Int = 200
                var error_msg: String? = null

                var request: Request = getRequest(endpoint, params, post_body)
                var refreshed: Boolean = false

                fun request(): String? {
                    val ret: Response
                    try {
                        ret = OkHttpClient.Builder().readTimeout(timeout, TimeUnit.SECONDS).build().newCall(request).execute()
                    }
                    catch (e: SocketTimeoutException) {
                        status = 408
                        error_msg = "Request timed out"
                        return null
                    }

                    status = ret.code
                    if (ret.code == 401) {
                        error_msg = "Server API key is invalid"
                    }
                    else if (ret.code == 404) {
                        if (allow_refresh && !refreshed) {
                            refresh()
                            refreshed = true
                            request = getRequest(endpoint, params, post_body)
                            return null
                        }
                        else {
                            error_msg = "Ngrok tunnel may be invalid (404)"
                            return null
                        }
                    }
                    else if (ret.code != 200) {
                        error_msg = "${ret.body?.string()} (${ret.code})"
                        return null
                    }

                    return ret.body!!.string()
                }

                var result: String? = null
                for (i in 0 until max_retries + 1) {
                    error_msg = null
                    status = 200

                    result = request()
                    if (result != null) {
                        break
                    }
                }

                return ServerRequestResult(error_msg ?: result ?: "Failed after $max_retries retries", status, endpoint)
            }
        }

        data class NgrokTunnelListResponse(val tunnels: List<Tunnel>) {
            data class Tunnel(val id: String, val public_url: String, val started_at: String, val tunnel_session: Session, var from_cache: Boolean = false)
            data class Session(val id: String)
        }

        fun getServer(): ServerAccessPoint? {
            if (server == null) {
                try {
                    server = ServerAccessPoint()
                }
                catch (e: Exception) {
                    return null
                }
            }
            return server
        }
        
        fun requestServer(
            endpoint: String,
            params: Map<String, String> = mapOf(),
            post_body: String? = null,
            throw_on_fail: Boolean = true,
            max_retries: Int = 5,
            timeout: Long = 20
        ): ServerRequestResult = Timed("requestServer", true) {

            server = getServer()
            if (server == null) {
                return@Timed ServerRequestResult.NO_SERVER
            }

            val localised_params: MutableMap<String, String> = HashMap(params)
            for (key in listOf(Pair(Settings.KEY_LANG_DATA, "dataLang"), Pair(Settings.KEY_LANG_UI, "interfaceLang"))) {
                if (!localised_params.containsKey(key.second)) {
                    localised_params[key.second] = MainActivity.languages.keys.elementAt(Settings.get(key.first))
                }
            }

            val ret = server!!.performRequest(endpoint, localised_params, post_body, max_retries, timeout)
            finishTiming(endpoint)

            if (throw_on_fail) {
                ret.throwStatus()
            }

            return@Timed ret

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

        fun getRecommendedFeed(allow_cached: Boolean = true, load_data: Boolean = true, callback: (ServerRequestDataResult<List<RecommendedFeedRow>>) -> Unit) = Timed("getRecommendedFeed", true) {
            val result = requestServer("/feed", mapOf(
                "noCache" to (!allow_cached).toInt().toString(),
                "loadData" to load_data.toInt().toString()
            ), timeout=30)

            if (!result.success) {
                callback(ServerRequestDataResult(result, null))
                return@Timed
            }

            loadMediaItemsFromDataResult(result.body) {
                val ret = klaxon.parseFromJsonArray<RecommendedFeedRow>(it as JsonArray<*>)!!
                finishTiming()
                callback(ServerRequestDataResult(result, ret))
            }
        }
    }
}

