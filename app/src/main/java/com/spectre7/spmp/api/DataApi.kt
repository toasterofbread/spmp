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

        fun getSongRelated(song: Song) {
            // TODO : https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_watch_playlist
        }

        data class Language(val hl: String, val name: String)
    }
}

