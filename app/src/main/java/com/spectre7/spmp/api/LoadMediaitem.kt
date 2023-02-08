package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import okhttp3.Request
import java.io.BufferedReader
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
    var item_layouts: MutableList<MediaItemLayout> = mutableListOf()
    var subscribe_channel_id: String? = null
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
                item.updateSubscribed()
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

                for (row in parsed.contents.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents.withIndex()) {
                    val desc = row.value.getDescription()
                    if (desc != null) {
                        description = desc
                        continue
                    }

                    item_layouts.add(MediaItemLayout(
                        row.value.title?.text,
                        null,
                        if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
                        row.value.getMediaItems().toMutableList()
                    ))
                }
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
            val artist = Playlist.fromId(album_run.navigationEndpoint!!.browseEndpoint!!.browseId).loadData().artist
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
