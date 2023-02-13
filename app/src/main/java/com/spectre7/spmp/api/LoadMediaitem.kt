package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import okhttp3.Request
import java.io.BufferedReader
import java.io.Reader
import java.time.Duration

data class PlayerData(val videoDetails: VideoDetails? = null)

data class VideoDetails(
    val videoId: String,
    val title: String,
    val channelId: String,
)

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

        item.load_status = MediaItem.LoadStatus.LOADING
    }

    fun finish(cached: Boolean = false): DataApi.Result<MediaItem> {
        item.load_status = MediaItem.LoadStatus.LOADED
        synchronized(lock) {
            lock.notifyAll()
        }

        if (!cached) {
            item.saveToCache()
        }

        return DataApi.Result.success(item)
    }

    if (item is Artist && item.unknown) {
        return finish(true)
    }

    val cache_key = item.cache_key
    val cached = Cache.get(cache_key)
    if (cached != null) {
        if (MediaItem.fromJsonData(cached) != item) {
            throw RuntimeException()
        }
        cached.close()
        return finish(true)
    }

    val url = if (item is Song) "https://music.youtube.com/youtubei/v1/next" else "https://music.youtube.com/youtubei/v1/browse"
    val body =
        if (item is Song)
            """{
                "enablePersistentPlaylistPanel": true,
                "isAudioOnly": true,
                "videoId": "$item_id"
            }"""
        else """{ "browseId": "$item_id" }"""

    var request: Request = Request.Builder()
        .url(url)
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody(body))
        .build()

    var response = DataApi.client.newCall(request).execute()
    if (response.code != 200) {
        return DataApi.Result.failure(response)
    }

    var response_body: Reader = response.body!!.charStream()

    if (item is MediaItemWithLayouts) {
        val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(response_body)!!
        response_body.close()

        val header_renderer = parsed.header!!.getRenderer()
        val item_layouts: MutableList<MediaItemLayout> = mutableListOf()

        item.supplyTitle(header_renderer.title.first_text, true)

        for (row in parsed.contents.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents.withIndex()) {
            val desc = row.value.getDescription()
            if (desc != null) {
                item.supplyDescription(desc, true)
                continue
            }

            item_layouts.add(MediaItemLayout(
                row.value.title?.text,
                null,
                if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
                row.value.getMediaItems().toMutableList()
            ))
        }
        item.supplyFeedLayouts(item_layouts, true)

        if (item is Artist && header_renderer.subscriptionButton != null) {
            val subscribe_button = header_renderer.subscriptionButton.subscribeButtonRenderer
            item.supplySubscribeChannelId(subscribe_button.channelId, true)
            item.supplySubscriberCountText(subscribe_button.subscriberCountText.first_text, true)
            item.subscribed = subscribe_button.subscribed
        }

        item.supplyThumbnailProvider(MediaItem.ThumbnailProvider.fromThumbnails(header_renderer.getThumbnails()))
        return finish()
    }

    val buffered_reader = BufferedReader(response_body)
    buffered_reader.mark(Int.MAX_VALUE)

    var video_details = DataApi.klaxon.parse<PlayerData>(buffered_reader)?.videoDetails
    if (video_details != null) {
        buffered_reader.close()
        item.supplyTitle(video_details.title, true)
        item.supplyArtist(Artist.fromId(video_details.channelId))
        return finish()
    }

    buffered_reader.reset()
    val video = DataApi.klaxon.parse<YoutubeiNextResponse>(buffered_reader)!!
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
    buffered_reader.close()

    item.supplyTitle(video.title.first_text, true)

    for (run in video.longBylineText.runs!!) {
        if (run.navigationEndpoint?.browseEndpoint?.page_type != "MUSIC_PAGE_TYPE_ARTIST") {
            continue
        }

        val artist = Artist.fromId(run.navigationEndpoint.browseEndpoint.browseId).supplyTitle(run.text)
        item.supplyArtist(artist as Artist, true)

        return finish()
    }

    val menu_artist = video.menu.menuRenderer.getArtist()?.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId
    if (menu_artist != null) {
        item.supplyArtist(Artist.fromId(menu_artist))
        return finish()
    }

    for (run in video.longBylineText.runs) {
        if (run.navigationEndpoint?.browseEndpoint?.page_type != "MUSIC_PAGE_TYPE_ALBUM") {
            continue
        }
        
        val artist = Playlist.fromId(run.navigationEndpoint.browseEndpoint.browseId).loadData().artist
        if (artist != null) {
            item.supplyArtist(artist, true)
            return finish()
        }
    }

    // 'next' endpoint has no artist, use 'player' instead
    request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/player")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""{ "videoId": "$item_id" }"""))
        .build()

    response = DataApi.client.newCall(request).execute()
    if (response.code != 200) {
        return DataApi.Result.failure(response)
    }

    response_body = response.body!!.charStream()
    video_details = DataApi.klaxon.parse<PlayerData>(response_body)!!.videoDetails!!
    response_body.close()

    item.supplyTitle(video_details.title, true)
    item.supplyArtist(Artist.fromId(video_details.channelId), true)

    return finish()
}
