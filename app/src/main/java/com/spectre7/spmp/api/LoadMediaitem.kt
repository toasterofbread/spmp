package com.spectre7.spmp.api

import android.util.JsonReader
import com.beust.klaxon.KlaxonException
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import okhttp3.Request
import java.io.BufferedReader
import java.io.Reader
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

data class PlayerData(val videoDetails: VideoDetails? = null)

data class VideoDetails(
    val videoId: String,
    val title: String,
    val channelId: String,
)

class BrowseData {
    var name: String? = null
    var description: String? = null
    var feed_rows: MutableList<FeedRow> = mutableListOf()
    var subscribe_channel_id: String? = null
    data class FeedRow(val title: String?, var items: List<MediaItem.Serialisable>, val media_item: MediaItem.Serialisable? = null) {
        fun toMediaItemLayout(): MediaItemLayout {
            return MediaItemLayout(title, null, items = MutableList(items.size) { i ->
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
    return next(listOf(key), is_array, allow_none, action)
}

fun JsonReader.first(is_array: Boolean?, allow_none: Boolean = false, action: (key: String) -> Unit) {
    return next(null, is_array, allow_none, action)
}

fun loadMediaItemData(item: MediaItem): DataApi.Result<MediaItem> {
    val lock = item.loading_lock
    val item_id = item.id

    synchronized(lock) {
        if (item.load_status == MediaItem.LoadStatus.LOADED) {
            return DataApi.Result.success(item.getOrReplacedWith())
        }

        if (item.load_status == MediaItem.LoadStatus.LOADING) {
            lock.wait()
            return if (item.load_status == MediaItem.LoadStatus.LOADED) DataApi.Result.success(item.getOrReplacedWith()) else DataApi.Result.failure(RuntimeException())
        }

        var response_body: BufferedReader
        val cache_key = "MediaItemData/${item.type.name}/$item_id"

        val cached = Cache.get(cache_key)
        if (cached != null) {
            response_body = cached
        }
        else {
            val url = if (item is Song) "https://music.youtube.com/youtubei/v1/next" else "https://music.youtube.com/youtubei/v1/browse"
            val body =
                if (item is Song)
                    """{
                        "enablePersistentPlaylistPanel": true,
                        "isAudioOnly": true,
                        "videoId": "$item_id"
                    }"""
                else """{ "browseId": "$item_id" }"""

            val request: Request = Request.Builder()
                .url(url)
                .headers(DataApi.getYTMHeaders())
                .post(DataApi.getYoutubeiRequestBody(body))
                .build()

            val response = DataApi.client.newCall(request).execute()
            if (response.code != 200) {
                return DataApi.Result.failure(response)
            }

            val reader = BufferedReader(response.body!!.charStream())
            Cache.set(cache_key, reader, CACHE_LIFETIME)
            reader.close()
            response_body = Cache.get(cache_key)!!
        }

        fun finish(data: Any, thumbnail_provider: MediaItem.ThumbnailProvider? = null): DataApi.Result<MediaItem> {
            if (item is Artist) {
                item.subscribed = isSubscribedToArtist(item).getNullableDataOrThrow()
            }

            item.initWithData(data, thumbnail_provider)
            lock.notifyAll()

            return DataApi.Result.success(item)
        }

        if (item !is Song) {
            val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(response_body)!!
            response_body.close()

            val header_renderer = parsed.header!!.getRenderer()
            return finish(BrowseData().apply {
                name = header_renderer.title.first_text

                if (header_renderer.subtitle?.runs != null) {
                    for (run in header_renderer.subtitle.runs) {
                        if (run.navigationEndpoint?.browseEndpoint != null) {
                            subscribe_channel_id = run.navigationEndpoint.browseEndpoint.browseId
                            break
                        }
                    }
                }

                feed_rows.add(BrowseData.FeedRow(
                    null,
                    parsed.contents.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents.first().getSerialisableMediaItems(),
                    item.toSerialisable()
                ))
            }, MediaItem.ThumbnailProvider.fromThumbnails(header_renderer.getThumbnails()))
        }

        var video_details = DataApi.klaxon.parse<PlayerData>(response_body)?.videoDetails
        response_body.close()
        if (video_details != null) {
            return finish(video_details)
        }

        response_body = Cache.get(cache_key)!!
        val video = DataApi.klaxon.parse<YoutubeiNextResponse>(response_body)!!
            .contents
            .singleColumnMusicWatchNextResultsRenderer
            .tabbedRenderer
            .watchNextTabbedResultsRenderer
            .tabs
            .first()
            .tabRenderer
            .content!!
            .musicQueueRenderer
            .content
            .playlistPanelRenderer
            .contents
            .first()
            .playlistPanelVideoRenderer!!
        response_body.close()

        val artist_run = video.longBylineText.runs!!.firstOrNull {
            it.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == "MUSIC_PAGE_TYPE_ARTIST"
        }

        if (artist_run != null) {
            return finish(VideoDetails(video.videoId, video.title.first_text, artist_run.navigationEndpoint!!.browseEndpoint!!.browseId))
        }

        val album_run = video.longBylineText.runs.firstOrNull {
            it.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == "MUSIC_PAGE_TYPE_ALBUM"
        }
        if (album_run != null) {
            val artist = Playlist.fromId(album_run.navigationEndpoint!!.browseEndpoint!!.browseId).loadData().getAssociatedArtist()
            if (artist != null) {
                return finish(VideoDetails(video.videoId, video.title.first_text, artist.id))
            }
        }

        val menu_artist = video.menu.menuRenderer.getArtist()?.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId
        if (menu_artist != null) {
            return finish(VideoDetails(video.videoId, video.title.first_text, menu_artist))
        }

        // 'next' endpoint has no artist, use 'player' instead
        val request: Request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/player")
            .headers(DataApi.getYTMHeaders())
            .post(DataApi.getYoutubeiRequestBody("""{ "videoId": "$item_id" }"""))
            .build()

        val response = DataApi.client.newCall(request).execute()
        if (response.code != 200) {
            return DataApi.Result.failure(response)
        }

        val reader = BufferedReader(response.body!!.charStream())
        Cache.set(cache_key, reader, CACHE_LIFETIME)
        reader.close()
        response_body = Cache.get(cache_key)!!

        video_details = DataApi.klaxon.parse<PlayerData>(response_body)!!.videoDetails!!
        response_body.close()
        return finish(video_details)
    }
}

class BrowseResponseParser(private val response_body: Reader) {
    private lateinit var reader: JsonReader

    lateinit var data: BrowseData
    var thumbnail_provider: MediaItem.ThumbnailProvider? = null

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
//                "microformat" -> {
//                    reader.beginObject()
//                    reader.next("microformatDataRenderer", false) {
//                        reader.next("urlCanonical", null) {
//                            val url = reader.nextString()
//                            val id_start = url.indexOf("?list=") + 6
//                            val id_end = url.indexOf('&', id_start)
//
//                            data.id_replace = if (id_end == -1) url.substring(id_start) else url.substring(id_start, id_end)
//                        }
//                    }
//                    reader.endObject()
//                }
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
                                                    thumbnail_provider = MediaItem.ThumbnailProvider.DynamicProvider.fromDynamicUrl(url, width, height)

                                                    if (thumbnail_provider == null) {
                                                        thumbnails = mutableListOf()
                                                        thumbnail_provider = MediaItem.ThumbnailProvider.SetProvider(thumbnails)
                                                    }
                                                    else {
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

