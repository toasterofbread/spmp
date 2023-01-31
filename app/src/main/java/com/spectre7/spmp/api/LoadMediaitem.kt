package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.utils.getString
import okhttp3.Request
import okio.Path.Companion.toPath
import java.io.BufferedReader
import java.nio.file.Path
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

private data class ApiResponse(val items: List<MediaItem.YTApiDataResponse>)

data class VideoData(val videoDetails: VideoDetails, val streamingData: StreamingData? = null) {
    data class VideoDetails(
        val videoId: String,
        val title: String,
        val channelId: String,
        val thumbnail: Thumbnails,
        val lengthSeconds: String
    ) {
        data class Thumbnails(val thumbnails: List<MediaItem.ThumbnailProvider.Thumbnail>)
    }
    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
}

data class ArtistData(
    val name: String,
    val description: String?,
    val feed_rows: List<FeedRow>
) {
    data class FeedRow(val title: String, var items: List<MediaItem.Serialisable>) {
        fun toMediaItemRow(): MediaItemRow {
            return MediaItemRow(title, null, MutableList(items.size) { i ->
                items[i].toMediaItem()
            })
        }
    }
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

        val cache_key = "MediaItemData/${item.type.name}/${item.id}"
        val response_body: BufferedReader

        val cached = Cache.get(cache_key)
        if (cached != null) {
            response_body = cached
        }
        else {
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
                is Artist -> {
                    request = Request.Builder()
                        .url("https://music.youtube.com/youtubei/v1/browse")
                        .headers(getYTMHeaders())
                        .post(getYoutubeiRequestBody("""
                            {
                                "browseId": "${item.id}"
                            }
                        """))
                        .build()
                }
                is Playlist -> {
                    val type: String = "playlists"
                    val part: String = "snippet,localizations"

                    request = Request.Builder()
                        .url("https://www.googleapis.com/youtube/v3/$type?part=$part&id=${item.id}&hl=${MainActivity.data_language}&key=${getString(R.string.yt_api_key)}")
                        .build()
                }
                else -> throw NotImplementedError()
            }

            val response = client.newCall(request).execute()
            if (response.code != 200) {
                return Result.failure(response)
            }

            val reader = BufferedReader(response.body!!.charStream())
            Cache.set(cache_key, reader, CACHE_LIFETIME)
            reader.close()
            response_body = Cache.get(cache_key)!!
        }

        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var thumbnail_provider: MediaItem.ThumbnailProvider? = null
        val data: Any

        when (item) {
            is Song -> {
                data = klaxon.parse<VideoData>(response_body)!!
                thumbnail_provider = MediaItem.ThumbnailProvider.SetProvider(data.videoDetails.thumbnail.thumbnails)
            }
            is Artist -> {
                val parser = ArtistBrowseResponseParser(response_body)
                parser.parse()
                data = parser.data!!
                thumbnail_provider = parser.thumbnail_provider
            }
            else -> {
                val parsed = klaxon.parse<ApiResponse>(response_body)!!
                if (parsed.items.isEmpty()) {
                    item.invalidate()
                    return Result.success(item)
                }
                data = parsed.items.first()
                thumbnail_provider = MediaItem.ThumbnailProvider.SetProvider(data.snippet!!.thumbnails.values.toList())
            }
        }

        response_body.close()

        item.initWithData(data, thumbnail_provider!!)
        item.loading_lock.notifyAll()

        return Result.success(item)
    }
}

class ArtistBrowseResponseParser(private val response_body: BufferedReader) {
    private lateinit var reader: JsonReader

    var data: ArtistData? = null
    var thumbnail_provider: MediaItem.ThumbnailProvider? = null

    fun parse() {
        reader = JsonReader(response_body)
        var title: String? = null
        var description: String? = null
        val items = mutableListOf<ArtistData.FeedRow>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "contents" -> {
                    reader.beginObject()

                    next(reader, "singleColumnBrowseResultsRenderer", false) {
                        next(reader, "tabs", true) {
                            assert(reader.hasNext())
                            reader.beginObject()

                            next(reader, "tabRenderer", false) {
                                next(reader, "content", false) {
                                    next(reader, "sectionListRenderer", false) {
                                        next(reader, "contents", true) {

                                            while (reader.hasNext()) {
                                                reader.beginObject()

                                                val new_description = parseShelf(items)
                                                if (description == null) {
                                                    description = new_description
                                                }

                                                reader.endObject()
                                            }
                                        }}}}

                            reader.endObject()
                        }}
                    reader.endObject()
                }
                "header" -> {
                    val header = parseHeaderObject(reader)
                    title = header.first!!
                    description = header.second
                    thumbnail_provider = header.third!!
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        reader.close()

        data = ArtistData(title!!, description, items)
    }

    private fun next(reader: JsonReader, key: String, is_array: Boolean?, action: () -> Unit) {
        var found = false
        while (reader.hasNext()) {
            if (reader.nextName() == key) {
                found = true

                when (is_array) {
                    true -> reader.beginArray()
                    false -> reader.beginObject()
                    else -> {}
                }

                action()

                when (is_array) {
                    true -> reader.endArray()
                    false -> reader.endObject()
                    else -> {}
                }
            }
            else {
                reader.skipValue()
            }
        }
        if (!found) {
            throw RuntimeException("Key '$key' not found (array: $is_array)")
        }
    }
    private fun textRunText(reader: JsonReader): String? {
        var ret: String? = null
        reader.beginObject()
        next(reader, "runs", true) {
            reader.beginObject()
            next(reader, "text", null) {
                ret = reader.nextString()
            }
            reader.endObject()
        }
        reader.endObject()
        return ret
    }
    private fun parseHeaderObject(reader: JsonReader): Triple<String?, String?, MediaItem.ThumbnailProvider?> {
        reader.beginObject()

        var title: String? = null
        var description: String? = null
        var thumbnail_provider: MediaItem.ThumbnailProvider? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "musicCarouselShelfBasicHeaderRenderer", "musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "title" -> title = textRunText(reader)
                            "description" -> description = textRunText(reader)
                            "thumbnail", "foregroundThumbnail" -> {
                                reader.beginObject()
                                next(reader, "musicThumbnailRenderer", false) {
                                    next(reader, "thumbnail", false) {
                                        next(reader, "thumbnails", true) {
                                            var thumbnails: MutableList<MediaItem.ThumbnailProvider.Thumbnail>? = null

                                            while (reader.hasNext()) {
                                                reader.beginObject()

                                                lateinit var url: String
                                                var width: Int = 0
                                                var height: Int = 0

                                                while (reader.hasNext()) {
                                                    when (reader.nextName()) {
                                                        "url" -> url = reader.nextString()
                                                        "width" -> width = reader.nextInt()
                                                        "height" -> height = reader.nextInt()
                                                        else -> reader.skipValue()
                                                    }
                                                }

                                                reader.endObject()

                                                if (thumbnails == null) {
                                                    val height_str = height.toString()

                                                    val w_index = url.lastIndexOf("w$width") + 1
                                                    val h_index = url.lastIndexOf("-h$height_str") + 2

                                                    if (w_index == -1 || h_index == -1) {
                                                        thumbnails = mutableListOf()
                                                        thumbnail_provider = MediaItem.ThumbnailProvider.SetProvider(thumbnails)
                                                    }
                                                    else {
                                                        val url_a = url.substring(0, w_index)
                                                        val url_b = url.substring(h_index + height_str.length)
                                                        thumbnail_provider = MediaItem.ThumbnailProvider.DynamicProvider { w, h ->
                                                            return@DynamicProvider "$url_a$w-h$h$url_b"
                                                        }
                                                        while (reader.hasNext()) {
                                                            reader.skipValue()
                                                        }
                                                        break
                                                    }
                                                }
                                                else {
                                                    thumbnails.add(MediaItem.ThumbnailProvider.Thumbnail(url, width, height))
                                                }
                                            }
                                        }
                                    }
                                }
                                reader.endObject()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return Triple(title, description, thumbnail_provider)
    }

    private fun parseShelf(rows: MutableList<ArtistData.FeedRow>): String? {
        if (!reader.hasNext()) {
            throw RuntimeException("Shelf has no renderer")
        }

        var description: String? = null

        while (reader.hasNext()) {
            when (val renderer = reader.nextName()) {
                "musicDescriptionShelfRenderer" -> {
                    if (description != null) {
                        reader.skipValue()
                    }

                    reader.beginObject()
                    next(reader, "description", null) {
                        description = textRunText(reader)
                    }
                    reader.endObject()
                }
                "musicShelfRenderer", "musicCarouselShelfRenderer" -> {
                    reader.beginObject()

                    var row_title: String? = null
                    val row_items: MutableList<MediaItem.Serialisable> = mutableListOf()

                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "title" -> {
                                row_title = textRunText(reader)
                            }
                            "header" -> {
                                row_title = parseHeaderObject(reader).first
                            }
                            "contents" -> {
                                reader.beginArray()

                                while (reader.hasNext()) {
                                    reader.beginObject()
                                    row_items.add(parseContentsItem() ?: throw RuntimeException("Could not parse ContentsItem"))
                                    reader.endObject()
                                }

                                reader.endArray()
                            }
                            else -> reader.skipValue()
                        }
                    }

                    rows.add(ArtistData.FeedRow(row_title!!, row_items))

                    reader.endObject()
                }
                else -> throw NotImplementedError(renderer)
            }
        }

        return description
    }

    private fun parseContentsItem(): MediaItem.Serialisable? {
        var ret: MediaItem.Serialisable? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "musicTwoRowItemRenderer" -> {
                    reader.beginObject()

                    next(reader, "navigationEndpoint", false) {
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "watchEndpoint" -> {
                                    reader.beginObject()
                                    next(reader, "videoId", null) {
                                        ret = Song.serialisable(reader.nextString())
                                    }
                                    reader.endObject()
                                }
                                "browseEndpoint" -> {
                                    reader.beginObject()
                                    ret = parseBrowseEndpoint()
                                    reader.endObject()
                                }
                                else -> reader.skipValue()
                            }
                        }
                    }

                    reader.endObject()
                }
                "musicResponsiveListItemRenderer" -> {
                    reader.beginObject()
                    next(reader, "playlistItemData", false) {
                        next(reader, "videoId", null) {
                            ret = Song.serialisable(reader.nextString())
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }

        return ret
    }

    private fun parseBrowseEndpoint(): MediaItem.Serialisable {
        var browse_id: String? = null
        var page_type: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "browseId" -> browse_id = reader.nextString()
                "browseEndpointContextSupportedConfigs" -> {
                    reader.beginObject()
                    next(reader, "browseEndpointContextMusicConfig", false) {
                        next(reader, "pageType", null) {
                            page_type = reader.nextString()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }

        return when (page_type) {
            "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> Playlist.serialisable(browse_id!!)
            "MUSIC_PAGE_TYPE_ARTIST" -> Artist.serialisable(browse_id!!)
            else -> throw NotImplementedError("$browse_id: $page_type")
        }
    }
}
