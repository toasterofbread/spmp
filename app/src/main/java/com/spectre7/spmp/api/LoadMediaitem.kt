package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.model.*
import okhttp3.Request
import java.io.BufferedReader
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

private data class ApiResponse(val items: List<MediaItem.YTApiDataResponse>)

data class VideoData(
    val videoDetails: VideoDetails,
//    val streamingData: StreamingData? = null
) {
    data class VideoDetails(
        val videoId: String,
        val title: String,
        val channelId: String,
        val thumbnail: Thumbnails,
        val lengthSeconds: String
    ) {
        data class Thumbnails(val thumbnails: List<MediaItem.ThumbnailProvider.Thumbnail>)
    }
//    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
}

data class BrowseData(
    val name: String,
    val description: String?,
    val feed_rows: List<FeedRow>,
    val id_replace: String? = null
) {
    data class FeedRow(val title: String?, var items: List<MediaItem.Serialisable>, val media_item: MediaItem.Serialisable? = null) {
        fun toMediaItemRow(): MediaItemRow {
            return MediaItemRow(title, null, MutableList(items.size) { i ->
                items[i].toMediaItem()
            })
        }
    }
}

private fun next(reader: JsonReader, key: String, is_array: Boolean?, action: () -> Unit) {
    return next(reader, listOf(key), is_array, action)
}

private fun next(reader: JsonReader, keys: List<String>, is_array: Boolean?, action: () -> Unit) {
    var found = false
    while (reader.hasNext()) {
        if (!found && keys.contains(reader.nextName())) {
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
        throw RuntimeException("No key within $keys found (array: $is_array)")
    }
}

private fun getActualArtistId(share_entity: String?): Result<String> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/share/get_share_panel")
        .headers(getYTMHeaders())
        .post(getYoutubeiRequestBody("""
            {
                "serializedSharedEntity": "$share_entity"
            }
        """))
        .build()

    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result.failure(response)
    }

    val stream = response.body!!.charStream()
    val reader = JsonReader(stream)

    var new_id: String? = null

    reader.beginObject()
    next(reader, "actions", true) {
        while (reader.hasNext()) {
            if (new_id != null) {
                reader.skipValue()
            }

            reader.beginObject()
            next(reader, "openPopupAction", false) {
                next(reader, "popup", false) {
                    next(reader, "unifiedSharePanelRenderer", false) {
                        next(reader, "contents", true) {
                            while (reader.hasNext()) {
                                if (new_id != null) {
                                    reader.skipValue()
                                    continue
                                }

                                reader.beginObject()
                                next(reader, "thirdPartyNetworkSection", false) {
                                    next(reader, "copyLinkContainer", false) {
                                        next(reader, "copyLinkRenderer", false) {
                                            next(reader, "shortUrl", null) {
                                                val url = reader.nextString()
                                                val start = url.lastIndexOf('/') + 1
                                                new_id = url.substring(start, url.indexOf('?', start))
                                            }
                                        }
                                    }
                                }
                                reader.endObject()
                            }
                        }
                    }
                }
            }
            reader.endObject()
        }
    }
    reader.endObject()

    if (new_id == null) {
        return Result.failure(RuntimeException("Could not get URL from share panel"))
    }

    return Result.success(new_id!!)
}

fun loadMediaItemData(item: MediaItem): Result<MediaItem> {
    val lock = item.loading_lock
    val item_id = item.id

    synchronized(lock) {
        if (item.load_status == MediaItem.LoadStatus.LOADED) {
            return Result.success(item.getOrReplacedWith())
        }

        if (item.load_status == MediaItem.LoadStatus.LOADING) {
            lock.wait()
            return if (item.load_status == MediaItem.LoadStatus.LOADED) Result.success(item.getOrReplacedWith()) else Result.failure(RuntimeException())
        }

        val cache_key = "MediaItemData/${item.type.name}/$item_id"
        val response_body: BufferedReader

        val cached = Cache.get(cache_key)
        if (cached != null) {
            response_body = cached
        }
        else {
            val key = if (item is Song) "videoId" else "browseId"
            val url = if (item is Song) "https://music.youtube.com/youtubei/v1/player" else "https://music.youtube.com/youtubei/v1/browse"

            val request: Request = Request.Builder()
                .url(url)
                .headers(getYTMHeaders())
                .post(getYoutubeiRequestBody("""
                    {
                        "$key": "$item_id"
                    }
                """))
                .build()

            val response = client.newCall(request).execute()
            if (response.code != 200) {
                return Result.failure(response)
            }

            val reader = BufferedReader(response.body!!.charStream())
            Cache.set(cache_key, reader, CACHE_LIFETIME)
            reader.close()
            response_body = Cache.get(cache_key)!!
        }

        val thumbnail_provider: MediaItem.ThumbnailProvider?
        val data: Any

        val ret_item: MediaItem

        if (item is Song) {
            data = klaxon.parse<VideoData>(response_body)!!
            thumbnail_provider = MediaItem.ThumbnailProvider.SetProvider(data.videoDetails.thumbnail.thumbnails)
            ret_item = item
        }
        else {
            val parser = BrowseResponseParser(response_body)
            parser.parse()
            data = parser.data!!
            thumbnail_provider = parser.thumbnail_provider

            if (data.id_replace != null) {
                ret_item = item.replaceWithItemWithId(data.id_replace)
            }
            else {
                ret_item = item
            }
        }

        response_body.close()

        ret_item.initWithData(data, thumbnail_provider)
        lock.notifyAll()

        return Result.success(ret_item)
    }
}

class BrowseResponseParser(private val response_body: BufferedReader) {
    private lateinit var reader: JsonReader

    var data: BrowseData? = null
    var thumbnail_provider: MediaItem.ThumbnailProvider? = null
    var id_replace: String? = null

    fun parse() {
        reader = JsonReader(response_body)
        var title: String? = null
        var description: String? = null
        val items = mutableListOf<BrowseData.FeedRow>()
        var share_entity: String? = null

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
                    val header = parseHeaderObject()
                    title = header.title!!
                    description = header.description
                    thumbnail_provider = header.thumbnail_provider
                    share_entity = header.share_entity
                }
                "microformat" -> {
                    reader.beginObject()
                    next(reader, "microformatDataRenderer", false) {
                        next(reader, "urlCanonical", null) {
                            val url = reader.nextString()
                            val id_start = url.indexOf("?list=") + 6
                            val id_end = url.indexOf('&', id_start)

                            id_replace = if (id_end == -1) url.substring(id_start) else url.substring(id_start, id_end)
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        reader.close()

        if (share_entity != null) {
            id_replace = getActualArtistId(share_entity).getDataOrThrow()
        }

        data = BrowseData(title!!, description, items, id_replace)
    }

    private fun textRunText(): String? {
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

    private data class HeaderObject(
        val title: String?,
        val description: String?,
        val thumbnail_provider: MediaItem.ThumbnailProvider?,
        val share_entity: String?
    )

    private fun parseHeaderObject(): HeaderObject {
        reader.beginObject()

        var title: String? = null
        var description: String? = null
        var thumbnail_provider: MediaItem.ThumbnailProvider? = null
        var share_entity: String? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "musicCarouselShelfBasicHeaderRenderer", "musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer", "musicDetailHeaderRenderer" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "title" -> title = textRunText()
                            "description" -> description = textRunText()
                            "thumbnail", "foregroundThumbnail" -> {
                                reader.beginObject()

                                next(reader, listOf("musicThumbnailRenderer", "croppedSquareThumbnailRenderer"), false) {
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
                                                    val w_index = url.lastIndexOf("w$width")
                                                    val h_index = url.lastIndexOf("-h$height")

                                                    if (w_index == -1 || h_index == -1) {
                                                        thumbnails = mutableListOf()
                                                        thumbnail_provider = MediaItem.ThumbnailProvider.SetProvider(thumbnails)
                                                    }
                                                    else {
                                                        val url_a = url.substring(0, w_index + 1)
                                                        val url_b = url.substring(h_index + 2 + height.toString().length)
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
                            "shareEndpoint" -> {
                                reader.beginObject()
                                next(reader, "shareEntityEndpoint", false) {
                                    next(reader, "serializedShareEntity", null) {
                                        share_entity = reader.nextString()
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

        return HeaderObject(title, description, thumbnail_provider, share_entity)
    }

    private fun parseShelf(rows: MutableList<BrowseData.FeedRow>): String? {
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
                        description = textRunText()
                    }
                    reader.endObject()
                }
                "musicShelfRenderer", "musicCarouselShelfRenderer", "musicPlaylistShelfRenderer" -> {
                    reader.beginObject()

                    var row_title: String? = null
                    val row_items: MutableList<MediaItem.Serialisable> = mutableListOf()
                    var media_item: MediaItem.Serialisable? = null

                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "title" -> {
                                row_title = textRunText()
                            }
                            "header" -> {
                                row_title = parseHeaderObject().title
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
                            "playlistId" -> {
                                media_item = Playlist.serialisable(reader.nextString())
                            }
                            else -> reader.skipValue()
                        }
                    }

                    rows.add(BrowseData.FeedRow(row_title, row_items, media_item))

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
                    ret = parseResponsiveListItemRenderer()
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

    private fun parseResponsiveListItemRenderer(): MediaItem.Serialisable {
        var ret: MediaItem.Serialisable? = null
        reader.beginObject()
        next(reader, "playlistItemData", false) {
            next(reader, "videoId", null) {
                ret = Song.serialisable(reader.nextString())
            }
        }
        reader.endObject()
        return ret!!
    }
}

