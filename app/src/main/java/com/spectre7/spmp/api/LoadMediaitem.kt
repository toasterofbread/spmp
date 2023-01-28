package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.utils.getString
import okhttp3.Request
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

private data class ApiResponse(val items: List<MediaItem.YTApiDataResponse>)

data class VideoData(val videoDetails: VideoDetails, val streamingData: StreamingData) {
    data class VideoDetails(
        val videoId: String,
        val title: String,
        val channelId: String,
        val thumbnail: Thumbnails,
        val lengthSeconds: String
    ) {
        data class Thumbnails(val thumbnails: List<MediaItem.Thumbnail>)
    }
    data class StreamingData(val formats: List<Format>, val adaptiveFormats: List<Format>) {
        data class Format(
            val itag: Int,
            val mimeType: String,
            val bitrate: Int,
            val quality: String,
            val signatureCipher: String? = null,
            val url: String? = null,
            val averageBitrate: Int? = null,
            val qualityLabel: String? = null,
            val audioQuality: String? = null
        )
    }
}

data class ArtistData(
    val name: String,
    val description: String?,
    val feed_rows: List<FeedRow>
) {
    data class FeedRow(val title: String, var items: List<MediaItem.Serialisable>)
}

fun loadMediaItemData(item: MediaItem): Result<MediaItem> {
    synchronized(item.loading_lock) {
        if (item.load_status == MediaItem.LoadStatus.LOADED) {
            return Result.success(item)
        }

        if (item.load_status == MediaItem.LoadStatus.LOADING) {
            item.loading_lock.wait()
            return if (item.load_status == MediaItem.LoadStatus.LOADED) Result.success(item) else Result.failure(RuntimeException())
        }

        val cache_key = "d${item.id}${item.type.name}"
        val cached = Cache.get(cache_key)
        if (cached != null) {
            when (item) {
                is Song -> {
                    val parsed = klaxon.parse<VideoData>(cached)!!
                    item.initWithData(parsed, parsed.videoDetails.thumbnail.thumbnails)
                }
//                is Artist -> {
//                    val parsed = klaxon.parse<ArtistData>(cached)!!
//                    item.initWithData(parsed, null)
//                }
                else -> {
                    val parsed = klaxon.parse<ApiResponse>(cached)!!
                    if (parsed.items.isEmpty()) {
                        item.invalidate()
                    }
                    else {
                        val data = parsed.items.first()
                        item.initWithData(data, data.snippet!!.thumbnails.values.toList())
                    }
                }
            }

            item.loading_lock.notifyAll()
            return Result.success(item)
        }

        val request: Request
        when (item) {
            is Song -> {
                request = Request.Builder()
                    .url("https://music.youtube.com/youtubei/v1/player")
                    .headers(getYTMHeaders())
                    .post(getYoutubeiRequestBody("""
                        {
                            "videoId": "${item.id}"
                        }
                    """))
                    .build()
            }
//            is Artist -> {
//                request = Request.Builder()
//                    .url("https://music.youtube.com/youtubei/v1/browse")
//                    .headers(getYTMHeaders())
//                    .post(getYoutubeiRequestBody("""
//                        {
//                            "browseId": "${item.id}"
//                        }
//                    """))
//                    .build()
//            }
            else -> {
                val type: String
                val part: String
                when (item) {
                    is Artist -> {
                        type = "channels"
                        part = "contentDetails,snippet,statistics"
                    }
                    is Playlist -> {
                        type = "playlists"
                        part = "snippet,localizations"
                    }
                    else -> throw NotImplementedError()
                }

                request = Request.Builder()
                    .url("https://www.googleapis.com/youtube/v3/$type?part=$part&id=${item.id}&hl=${MainActivity.data_language}&key=${getString(R.string.yt_api_key)}")
                    .build()
            }
        }

        val response = client.newCall(request).execute()
        if (response.code != 200) {
            return Result.failure(response)
        }

        val response_body = response.body!!.string()
        if (item !is Artist) {
            Cache.set(cache_key, response_body, CACHE_LIFETIME)
        }

        val data: Any
        val thumbnails: List<MediaItem.Thumbnail>?

        when (item) {
            is Song -> {
                data = klaxon.parse<VideoData>(response_body)!!
                thumbnails = data.videoDetails.thumbnail.thumbnails
            }
//            is Artist -> {
//                val parsed = klaxon.parseJsonObject(response_body.reader())
//
//                val h = parsed.obj("header")!!
//                val header = h.obj("musicImmersiveHeaderRenderer") ?: h.obj("musicVisualHeaderRenderer")!!
//
//                val title = klaxon.parseFromJsonObject<TextRuns>(header.obj("title")!!)!!.first_text
//
//                val description_data = header.obj("description")
//                var description = if (description_data != null) klaxon.parseFromJsonObject<TextRuns>(description_data)!!.first_text else null
//
//                val rows = parsed
//                    .obj("contents")!!
//                    .obj("singleColumnBrowseResultsRenderer")!!
//                    .array<JsonObject>("tabs")!![0]
//                    .obj("tabRenderer")!!
//                    .obj("content")!!
//                    .obj("sectionListRenderer")!!
//                    .array<JsonObject>("contents")!!
//
//                val items = mutableListOf<ArtistData.FeedRow>()
//
//                for (row in klaxon.parseFromJsonArray<YoutubeiShelf>(rows)!!) {
//                    when (val renderer = row.getRenderer()) {
//                        is MusicDescriptionShelfRenderer -> {
//                            if (description == null) {
//                                description = renderer.description.first_text
//                            }
//                        }
//                        is MusicShelfRenderer, is MusicCarouselShelfRenderer ->
//                            items.add(
//                                ArtistData.FeedRow(
//                                    row.title.text,
//                                    List(row.contents.size) { j ->
//                                        row.contents[j].toMediaItem().toSerialisable()
//                                    }
//                                )
//                            )
//                        else -> throw NotImplementedError(renderer.javaClass.name)
//                    }
//                }
//
//                data = ArtistData(title, description, items)
//                thumbnails = null
//
//                Cache.set(cache_key, klaxon.toJsonString(data), CACHE_LIFETIME)
//            }
            else -> {
                val parsed = klaxon.parse<ApiResponse>(response_body)!!
                if (parsed.items.isEmpty()) {
                    item.invalidate()
                    return Result.success(item)
                }
                data = parsed.items.first()
                thumbnails = data.snippet!!.thumbnails.values.toList()
            }
        }

        item.initWithData(data, thumbnails)
        item.loading_lock.notifyAll()

        return Result.success(item)
    }
}
