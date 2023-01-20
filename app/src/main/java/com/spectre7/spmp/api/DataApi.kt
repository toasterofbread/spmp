package com.spectre7.spmp.api

import com.beust.klaxon.*
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.layout.ResourceType
import com.spectre7.utils.getString
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        private val klaxon: Klaxon = Klaxon()

        data class YtItemLoadQueueKey(val id: String, val type: String) {
            companion object {
                fun from(item: MediaItem): YtItemLoadQueueKey {
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
        private val ytitem_load_queue: MutableMap<YtItemLoadQueueKey, MutableList<(MediaItem.YTApiDataResponse?) -> Unit>> = mutableMapOf()
        private val ytitem_load_mutex = Mutex()

        fun queueYtItemDataLoad(item: MediaItem, callback: (MediaItem.YTApiDataResponse?) -> Unit) {
            runBlocking {
                synchronized(ytitem_load_queue) {
                    ytitem_load_queue.getOrPut(YtItemLoadQueueKey.from(item)) { mutableListOf() }.add(callback)
                }
            }
        }

        data class SearchResults(val items: List<Result>) {
            data class Result(val id: ResultId, val snippet: MediaItem.YTApiDataResponse.Snippet)
            data class ResultId(val kind: String, val videoId: String = "", val channelId: String = "", val playlistId: String = "")
        }

        fun search(request: String, type: ResourceType, max_results: Int = 10, channel_id: String? = null): List<SearchResults.Result> {
            throw NotImplementedError()
//            val parameters = mutableMapOf(
//                "part" to "snippet",
//                "type" to when (type) {
//                    ResourceType.SONG -> "video"
//                    ResourceType.ARTIST -> "channel"
//                    ResourceType.PLAYLIST -> "playlist"
//                },
//                "q" to request,
//                "maxResults" to max_results.toString(),
//                "safeSearch" to "none"
//            )
//            if (channel_id != null) {
//                parameters.put("channelId", channel_id)
//            }
//            val result = requestServer("/yt/search", parameters)
//            result.throwStatus()
//            return klaxon.parse<SearchResults>(result.body)!!.items
        }

        fun getLyrics(id: String): Song.Lyrics? {
            throw NotImplementedError()
//            val result = requestServer(
//                "/get_lyrics/",
//                mapOf("id" to id),
//                max_retries = 1,
//                throw_on_fail = false
//            )
//
//            if (!result.success) {
//                return null
//            }
//
//            return klaxon.converter(object : Converter {
//                override fun canConvert(cls: Class<*>): Boolean {
//                    return cls == Song.Lyrics.Source::class.java
//                }
//
//                override fun fromJson(jv: JsonValue): Any? {
//                    if (jv.string == null) {
//                        return null
//                    }
//                    return Song.Lyrics.Source.getFromString(jv.string!!.split(':', limit = 2)[0])
//                }
//
//                override fun toJson(value: Any): String {
//                    return (value as Song.Lyrics.Source).string_code
//                }
//
//            }).parse<Song.Lyrics>(result.body)
        }

        fun getSongLyrics(song: Song, callback: (Song.Lyrics?) -> Unit) {
            throw NotImplementedError()
//            thread {
//                val id = song.registry.overrides.lyrics_id
//                val ret: Song.Lyrics?
//
//                if (id != null) {
//                    ret = getLyrics(id)
//                }
//                else {
//                    val result = requestServer(
//                        "/search_and_get_lyrics/",
//                        mapOf("title" to song.title, "artist" to song.artist.name),
//                        max_retries = 1,
//                        throw_on_fail = false
//                    )
//                    if (!result.success) {
//                        callback(null)
//                        return@thread
//                    }
//
//                    ret = klaxon.parse<Song.Lyrics>(result.body)
//                }
//
//                song.registry.overrides.lyrics_id = ret?.id
//                callback(ret)
//            }
        }

        fun searchForLyrics(title: String, artist: String?, callback: (List<LyricsSearchResult>?) -> Unit) {
            throw NotImplementedError()
//            thread {
//                try {
//                    val params = mutableMapOf("title" to title)
//                    if (artist != null) {
//                        params["artist"] = artist
//                    }
//
//                    val result = requestServer(
//                        "/search_lyrics/",
//                        params,
//                        max_retries = 1
//                    )
//                    if (!result.success) {
//                        callback(null)
//                        return@thread
//                    }
//
//                    callback(klaxon.parseArray(result.body))
//                }
//                catch (e: Exception) {
//                    MainActivity.network.onError(e)
//                    callback(null)
//                }
//            }
        }

        fun getSongRelated(song: Song) {
            // TODO : https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_song_related
        }
    }
}

