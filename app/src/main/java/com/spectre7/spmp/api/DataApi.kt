package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.spectre7.ptl.Ptl
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.layout.ResourceType
import com.spectre7.utils.getString
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread

class DataApi {

    init {
        MainActivity.network.addRetryCallback {
            updateSongRequests()
        }
    }

    private class VideoInfoResponse(val items: List<VideoItem>) {
        class VideoItem(
            val contentDetails: ContentDetails,
            val localizations: Map<String, Localisation> = emptyMap(),
            val snippet: Snippet
        ) {
            class ContentDetails(val duration: String)
            class Localisation(val title: String, val description: String)
            class Snippet(val title: String, val description: String, val publishedAt: String, val channelId: String)
        }

        fun getVideo(): VideoItem {
            return items[0]
        }
    }

    private class ChannelInfoResponse(val items: List<ChannelItem>) {
        class ChannelItem(
            val snippet: Snippet,
            val statistics: Statistics,
            val localizations: Map<String, Localisation> = emptyMap()
        ) {
            class Snippet(val title: String, val description: String = "", val publishedAt: String, val defaultLanguage: String? = null, val country: String? = null, val thumbnails: Thumbnails)
            class Statistics(val viewCount: String, val subscriberCount: String, val hiddenSubscriberCount: Boolean, val videoCount: String)
            class Localisation(val title: String = "", val description: String = "")
        }

        fun getChannel(): ChannelItem {
            return items[0]
        }
    }

    class Thumbnails(val default: Thumbnail, val medium: Thumbnail, val high: Thumbnail)
    class Thumbnail(val url: String)

    companion object {

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
        private val ptl = Ptl()
        private var ngrok_tunnel: NgrokTunnelListResponse.Tunnel? = null

        private val song_request_queue = mutableListOf<Pair<String, MutableList<(Song?) -> Unit>>>()
        private var song_request_count = 0
        private val max_song_requests = 5
        private val song_request_mutex = Mutex()

        // TODO | Song and artist cache
        fun getSong(song_id: String, callback: (Song?) -> Unit) {
            for (request in song_request_queue) {
                if (request.first == song_id) {
                    request.second.add(callback)
                    return
                }
            }

            song_request_queue.add(Pair(song_id, mutableListOf(callback)))

            updateSongRequests()
        }

        fun batchGetSongs(song_ids: List<String>, callback: (Int, Song?) -> Unit) {
            var added = false
            runBlocking {
                song_request_mutex.withLock {

                    val buffer = mutableMapOf<Int, Song?>()
                    var head = 0

                    fun getCallback(index: Int): (Song?) -> Unit {
                        return {
                            buffer.put(index, it)
                            while (buffer.containsKey(head)) {
                                callback(head, buffer[head++])
                            }
                        }
                    }

                    for (i in song_ids.indices) {
                        val song_id = song_ids[i]
                        var skip = false
                        for (request in song_request_queue) {
                            if (request.first == song_id) {
                                request.second.add(getCallback(i))
                                skip = true
                                break
                            }
                        }

                        if (!skip) {
                            song_request_queue.add(Pair(song_id, mutableListOf(getCallback(i))))
                            added = true
                        }
                    }
                }
            }

            if (added) {
                updateSongRequests()
            }
        }

        private fun updateSongRequests() {
            runBlocking {
                song_request_mutex.withLock {
                    while (song_request_count < max_song_requests && song_request_queue.isNotEmpty()) {
                        song_request_count++

                        val song_request = song_request_queue.removeFirst()
                        thread {
                            var ret: Song? = null

                            try {
                                val result = queryServer("/youtubeapi/videos", mapOf(
                                    "part" to "contentDetails,snippet,localizations",
                                    "id" to song_request.first
                                ), throw_on_fail = true)!!
                                val video = klaxon.parse<VideoInfoResponse>(result)?.getVideo()
                                if (video != null) {
                                    ret = Song(song_request.first, SongData("", video.snippet.title, video.snippet.description), getArtist(video.snippet.channelId)!!, Date.from(Instant.parse(video.snippet.publishedAt)), java.time.Duration.parse(video.contentDetails.duration))
                                }
                            }
                            catch (e: Exception) {
                                MainActivity.network.onError(e)
                                song_request_queue.add(song_request)
                                song_request_count--
                                return@thread
                            }

                            for (callback in song_request.second) {
                                callback(ret)
                            }

                            MainActivity.runInMainThread {
                                song_request_count--
                                updateSongRequests()
                            }
                        }
                    }
                }
            }
        }

        private val artistCache: MutableMap<String, Artist> = mutableMapOf()

        fun getArtist(channelId: String): Artist? {

            if (artistCache.containsKey(channelId)) {
                return artistCache.getValue(channelId)
            }

            val result = queryServer("/youtubeapi/channels", mapOf(
                "part" to "contentDetails,snippet,localizations,statistics",
                "id" to channelId
            ))
            if (result == null) {
                return null
            }

            val channel = klaxon.parse<ChannelInfoResponse>(result)?.getChannel()
            if (channel == null) {
                return null
            }

            val ret = Artist(channelId, ArtistData(channel.snippet.defaultLanguage, channel.snippet.title, channel.snippet.description),
                Date.from(Instant.parse(channel.snippet.publishedAt)),
                channel.snippet.thumbnails.default.url,
                channel.snippet.thumbnails.high.url,
                channel.statistics.viewCount,
                channel.statistics.subscriberCount,
                channel.statistics.hiddenSubscriberCount,
                channel.statistics.videoCount
            )

            artistCache[channelId] = ret

            return ret
        }

        data class SearchResults(val items: List<Result>) {
            data class Result(val id: ResultId, val snippet: Snippet)
            data class ResultId(val kind: String, val videoId: String = "", val channelId: String = "", val playlistId: String = "")
            data class Snippet(val publishedAt: String, val channelId: String, val title: String, val description: String, val thumbnails: Thumbnails)
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
            val result = queryServer("/youtubeapi/search", parameters)
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

        val ytd = Python.getInstance().getModule("yt_dlp").callAttr("YoutubeDL")

        fun getDownloadUrl(id: String, callback: (url: String?) -> Unit) {
            thread {

                val DEFAULT_CLIENT = """{"clientName":"ANDROID","clientVersion":"16.50","visitorData":null,"hl":"en"}"""
                val NO_CONTENT_WARNING_CLIENT = """{"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0","visitorData":null,"hl":"en"}"""

                val http_client = OkHttpClient()

                // TODO | Use own API key
                fun attemptGetDownloadUrl(client: String): String? {
                    val body = """{"context":{"client":$client},"videoId":"$id","playlistId":null}""".toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8&prettyPrint=false")
                        .post(body)
                        .build()
                    val response = http_client.newCall(request).execute()
                    if (response.code != 200) {
                        throw RuntimeException(response.body!!.string())
                    }

                    val parsed = klaxon.parse<GetDownloadUrlResult>(response.peekBody(1000000).string())!!

                    if (!parsed.isPlayable()) {
                        return null
                    }

                    for (format in parsed.streamingData!!.adaptiveFormats) {
                        if (format.itag == 140) {
                            return format.url
                        }
                    }

                    return null
                }

                var url = attemptGetDownloadUrl(DEFAULT_CLIENT)
                if (url == null) {
                    url = attemptGetDownloadUrl(NO_CONTENT_WARNING_CLIENT)
                    if (url == null) {
                        // Use yt-dlp as a catch-all backup (it's too slow to use all the time)
                        val formats = ytd.callAttr("extract_info", id, false).callAttr("get", "formats").asList()
                        for (format in formats) {
                            if (format.callAttr("get", "format_id").toString() == "140") {
                                url = format.callAttr("get", "url").toString()
                                break
                            }
                        }
                    }
                }

                MainActivity.runInMainThread {
                    callback(url)
                }
            }
        }

        fun getSongPTLyricsId(song: Song): Int? {
            return ptl.findLyricsId(song.title, song.artist.nativeData.name)
        }

        fun getSongYTLyricsId(song: Song): String? {
            fun getLyricsId(video_id: String?): String? {
                val lyrics = ytapi?.callAttr("get_watch_playlist", video_id, null, 1)?.callAttr("get", "lyrics")
                if (lyrics != null) {
                    return lyrics.toString()
                }
                return null
            }

            val ret = getLyricsId(song.getId())
            if (ret != null) {
                return ret
            }

            return getLyricsId(song.getCounterpartId())
        }

        fun getSongLyrics(song: Song, callback: (Song.Lyrics?) -> Unit) {
            thread {
                val params = mapOf("title" to song.title, "artist" to song.artist.nativeData.name)
                val result = queryServer("/lyrics", parameters = params)

                if (result == null) {
                    callback(null)
                }
                else {
                    callback(klaxon.parse<Song.Lyrics>(result))
                }
            }
        }

        fun getSongCounterpartId(song: Song): String? {
            if (ytapi == null) {
                return null
            }
            val counterpart = ytapi!!.callAttr("get_watch_playlist", song.getId(), null, 1).callAttr("get", "tracks").asList().first().callAttr("get", "conuterpart")
            if (counterpart == null) {
                return null
            }
            return counterpart.callAttr("get", "videoId").toString()
        }

        fun getArtistCounterpartId(artist: Artist): String? {
            if (ytapi == null) {
                return null
            }
            val results = ytapi!!.callAttr("search", artist.nativeData.name, "artists", null, 1, true).asList()
            return results.first().callAttr("get", "browseId").toString()
        }

        fun getSongRadio(song_id: String, include_first: Boolean = true, limit: Int = 25): List<String> {
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

        data class NgrokTunnelListResponse(val tunnels: List<Tunnel>) {
            data class Tunnel(val id: String, val public_url: String, val started_at: String, val tunnel_session: Session, var from_cache: Boolean = false)
            data class Session(val id: String)
        }

        fun getNgrokTunnel(no_cache: Boolean = false): NgrokTunnelListResponse.Tunnel? {
            if (ngrok_tunnel != null && !no_cache) {
                ngrok_tunnel!!.from_cache = true
                return ngrok_tunnel
            }

            val request = Request.Builder()
                .url("https://api.ngrok.com/tunnels")
                .header("Authorization", "Bearer ${getString(R.string.ngrok_api_key)}")
                .addHeader("Ngrok-Version", "2")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            if (response.code != 200) {
                ngrok_tunnel = null
                return null
            }

            val tunnels = klaxon.parse<NgrokTunnelListResponse>(response.body!!.string())?.tunnels
            ngrok_tunnel = tunnels?.getOrNull(0)
            return ngrok_tunnel
        }

        fun queryServer(endpoint: String, parameters: Map<String, String> = mapOf(), throw_on_fail: Boolean = true, tunnel: NgrokTunnelListResponse.Tunnel? = null, max_retries: Int = 5, timeout: Long = 10): String? {
            var _tunnel = tunnel ?: getNgrokTunnel()
            if (_tunnel == null) {
                if (throw_on_fail) {
                    throw RuntimeException("Failed to get Ngrok tunnel. Is the server running?")
                }
                return null
            }

            fun getRequest(tunnel: NgrokTunnelListResponse.Tunnel): Request {
                var url = "${tunnel.public_url}$endpoint?key=${getString(R.string.server_api_key)}"

                if (!parameters.containsKey("localisation")) {
                    val loc = MainActivity.prefs.getString("localisation_code", null)
                    if (loc != null) {
                        url += "&localisation=$loc"
                    }
                }

                for (param in parameters) {
                    url += "&${URLEncoder.encode(param.key, "UTF-8")}=${URLEncoder.encode(param.value, "UTF-8")}"
                }
                return Request.Builder().url(url).build()
            }

            var request = getRequest(_tunnel)
            var result: Response? = null
            var cancel: Boolean = false
            var exception: Exception? = null

            fun getResult(): Response {
                val ret: Response = OkHttpClient.Builder().readTimeout(timeout, TimeUnit.SECONDS).build().newCall(request).execute()
                if (ret.code == 401) {
                    cancel = true
                    throw RuntimeException("Server API key is invalid (401)")
                }
                else if (ret.code == 404 && _tunnel!!.from_cache) {
                    _tunnel = getNgrokTunnel(true)
                    if (_tunnel != null) {
                        request = getRequest(_tunnel!!)
                    }
                    cancel = true
                    throw RuntimeException("Ngrok tunnel may be invalid (404)")
                }
                else if (ret.code != 200) {
                    throw RuntimeException("${ret.body?.string()} (${ret.code})")
                }
                return ret
            }

            for (i in 0 until max_retries + 1) {
                try {
                    result = getResult()
                }
                catch(e: Exception) {
                    exception = e
                }
                if (result != null || _tunnel == null || cancel) {
                    break
                }
            }

            if (result == null && throw_on_fail) {
                throw RuntimeException("Request to server failed. ${exception?.message}. Request URL: ${request.url}.")
            }

            return result?.body?.string()
        }

        data class RecommendedFeedRow(val title: String, val subtitle: String?, val items: List<Item>) {
            data class Item(val type: String, val id: String, val playlist_id: String? = null) {
                fun getPreviewable(callback: (Previewable?) -> Unit) {
                    when (type) {
                        "song" -> Song.fromId(id, callback)
                        "artist" -> callback(Artist.fromId(id))
                        "playlist" -> {} // TODO
                        else -> throw RuntimeException(type)
                    }
                }
            }
        }

        fun getRecommendedFeed(): List<RecommendedFeedRow> {
            val data = queryServer("/feed", timeout=30, throw_on_fail = true)!!
            return klaxon.parseArray(data)!!
        }
    }
}

