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

class BrowseData {
    var name: String? = null
    var description: String? = null
    var feed_rows: MutableList<FeedRow> = mutableListOf()
    var subscribe_channel_id: String? = null
    var id_replace: String? = null
    data class FeedRow(val title: String?, var items: List<MediaItem.Serialisable>, val media_item: MediaItem.Serialisable? = null) {
        fun toMediaItemRow(): MediaItemRow {
            return MediaItemRow(title, null, MutableList(items.size) { i ->
                items[i].toMediaItem()
            })
        }
    }
}

fun JsonReader.next(keys: List<String>?, is_array: Boolean?, allow_none: Boolean = false, action: (key: String) -> Unit) {
    var found = false

    while (hasNext()) {
        val name = nextName()
        if (!found && (keys == null || keys.isEmpty() || keys.contains(name))) {
            found = true

            when (is_array) {
                true -> beginArray()
                false -> beginObject()
                else -> {}
            }

            action(name)

            when (is_array) {
                true -> endArray()
                false -> endObject()
                else -> {}
            }
        }
        else {
            skipValue()
        }
    }

    if (!allow_none && !found) {
        throw RuntimeException("No key within $keys found (array: $is_array)")
    }
}

fun JsonReader.next(key: String, is_array: Boolean?, allow_none: Boolean = false, action: (key: String) -> Unit) {
    return reader.next(listOf(key), is_array, allow_none, action)
}

fun JsonReader.first(is_array: Boolean?, allow_none: Boolean = false, action: (key: String) -> Unit) {
    return reader.next(null, is_array, allow_none, action)
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
    reader.next("actions", true) {
        while (reader.hasNext()) {
            if (new_id != null) {
                reader.skipValue()
            }

            reader.beginObject()
            reader.next("openPopupAction", false) {
                reader.next("popup", false) {
                    reader.next("unifiedSharePanelRenderer", false) {
                        reader.next("contents", true) {
                            while (reader.hasNext()) {
                                if (new_id != null) {
                                    reader.skipValue()
                                    continue
                                }

                                reader.beginObject()
                                reader.next("thirdPartyNetworkSection", false) {
                                    reader.next("copyLinkContainer", false) {
                                        reader.next("copyLinkRenderer", false) {
                                            reader.next("shortUrl", null) {
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

    reader.close()
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
            data = parser.data
            thumbnail_provider = parser.thumbnail_provider

            if (data.id_replace != null) {
                ret_item = item.replaceWithItemWithId(data.id_replace!!)
            }
            else {
                ret_item = item
            }
        }

        response_body.close()

        if (ret_item is Artist) {
            ret_item.subscribed = isSubscribedToArtist(ret_item).getNullableDataOrThrow()
        }

        ret_item.initWithData(data, thumbnail_provider)
        lock.notifyAll()

        return Result.success(ret_item)
    }
}

class BrowseResponseParser(private val response_body: BufferedReader) {
    private lateinit var reader: JsonReader

    lateinit var data: BrowseData
    var thumbnail_provider: MediaItem.ThumbnailProvider? = null
    var id_replace: String? = null

    fun parse() {
        reader = JsonReader(response_body)
        data = BrowseData()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "contents" -> {
                    reader.beginObject()

                    reader.next("singleColumnBrowseResultsRenderer", false) {
                        reader.next("tabs", true) {
                            assert(reader.hasNext())
                            reader.beginObject()

                            reader.next("tabRenderer", false) {
                                reader.next("content", false) {
                                    reader.next("sectionListRenderer", false) {
                                        reader.next("contents", true) {

                                            while (reader.hasNext()) {
                                                reader.beginObject()

                                                val new_description = parseShelf(data.feed_rows)
                                                if (data.description == null) {
                                                    data.description = new_description
                                                }

                                                reader.endObject()
                                            }
                                        }}}}

                            reader.endObject()
                        }}
                    reader.endObject()
                }
                "header" -> {
                    parseHeaderObject(data)
                }
                "microformat" -> {
                    reader.beginObject()
                    reader.next("microformatDataRenderer", false) {
                        reader.next("urlCanonical", null) {
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
    }

    private fun textRunText(): String? {
        var ret: String? = null
        reader.beginObject()
        reader.next("runs", true) {
            reader.beginObject()
            reader.next("text", null) {
                ret = reader.nextString()
            }
            reader.endObject()
        }
        reader.endObject()
        return ret
    }

    private fun parseHeaderObject(data: BrowseData): BrowseData {
        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "musicCarouselShelfBasicHeaderRenderer", "musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer", "musicDetailHeaderRenderer" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "title" -> data.name = textRunText()
                            "description" -> data.description = textRunText()
                            "thumbnail", "foregroundThumbnail" -> {
                                reader.beginObject()

                                reader.next(listOf("musicThumbnailRenderer", "croppedSquareThumbnailRenderer"), false) {
                                    reader.next("thumbnail", false) {
                                        reader.next("thumbnails", true) {
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
                                reader.next("shareEntityEndpoint", false) {
                                    reader.next("serializedShareEntity", null) {
                                        data.id_replace = getActualArtistId(reader.nextString()).getDataOrThrow()
                                    }
                                }
                                reader.endObject()
                            }
                            "subscriptionButton" -> {
                                reader.beginObject()
                                reader.next("subscribeButtonRenderer", false) {
                                    reader.next("serviceEndpoints", true) {
                                        while (reader.hasNext()) {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                if (reader.nextName() == "subscribeEndpoint") {
                                                    reader.beginObject()
                                                    reader.next("channelIds", true) {
                                                        while (reader.hasNext()) {
                                                            if (data.subscribe_channel_id == null) {
                                                                data.subscribe_channel_id = reader.nextString()
                                                            }
                                                            else {
                                                                reader.skipValue()
                                                            }
                                                        }
                                                    }
                                                    reader.endObject()
                                                }
                                                else {
                                                    reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
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

        return data
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
                    reader.next("description", null) {
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
                                row_title = parseHeaderObject(BrowseData()).name
                            }
                            "contents" -> {
                                reader.beginArray()

                                while (reader.hasNext()) {
                                    reader.beginObject()
                                    val item = parseContentsItem()
                                    if (item != null) {
                                        row_items.add(item)
                                    }
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

                    reader.next("navigationEndpoint", false) {
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "watchEndpoint" -> {
                                    reader.beginObject()
                                    reader.next("videoId", null) {
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
                    reader.next("browseEndpointContextMusicConfig", false) {
                        reader.next("pageType", null) {
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

    private fun parseResponsiveListItemRenderer(): MediaItem.Serialisable? {
        var ret: MediaItem.Serialisable? = null
        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "playlistItemData" -> {
                    reader.beginObject()
                    reader.next("videoId", null) {
                        ret = Song.serialisable(reader.nextString())
                    }
                    reader.endObject()
                }
                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()
        return ret
    }
}

