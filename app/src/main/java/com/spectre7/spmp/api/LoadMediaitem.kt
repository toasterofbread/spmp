package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.getString
import com.spectre7.utils.printJson
import okhttp3.Request
import java.io.BufferedReader
import java.io.Reader
import java.time.Duration

data class PlayerData(
    val videoDetails: VideoDetails? = null,
//    val streamingData: StreamingData? = null
) {
//    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
}

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

fun loadMediaItemData(item: MediaItem): Result<MediaItem> {
    val lock = item.loading_lock
    val item_id = item.id

    synchronized(lock) {
        if (item.loading) {
            lock.wait()
            return Result.success(item.getOrReplacedWith())
        }
        item.loading = true
    }

    fun finish(cached: Boolean = false): Result<MediaItem> {
        item.loading = false
        synchronized(lock) {
            lock.notifyAll()
        }

        if (!cached) {
            item.saveToCache()
        }

        return Result.success(item)
    }

    if (item is Artist && item.unknown) {
        return finish(true)
    }

    val cache_key = item.cache_key
    val cached = Cache.get(cache_key)
    if (cached != null) {
        val str = cached.readText()
        if (MediaItem.fromJsonData(str.reader()) != item) {
            throw RuntimeException()
        }
        cached.close()

        if (item.isFullyLoaded()) {
            return finish(true)
        }
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

    if (item_id == "_ZgZOaF04aE") println("BRUH 1")

    val response = DataApi.request(request).getOrNull()
    if (response != null) {
    if (item_id == "_ZgZOaF04aE") println("BRUH 2")

        val response_body: Reader = response.body!!.charStream()

        if (item is MediaItemWithLayouts) {
            val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(response_body)!!
            response_body.close()

            val header_renderer = parsed.header!!.getRenderer()
            val item_layouts: MutableList<MediaItemLayout> = mutableListOf()

            item.supplyTitle(header_renderer.title.first_text, true)
            item.supplyDescription(header_renderer.description?.first_text, true)

            val artist = header_renderer.subtitle?.runs?.firstOrNull {
                it.navigationEndpoint?.browseEndpoint?.page_type == "MUSIC_PAGE_TYPE_USER_CHANNEL"
            }
            if (artist != null) {
                item.supplyArtist(
                    Artist
                        .fromId(artist.navigationEndpoint!!.browseEndpoint!!.browseId)
                        .supplyTitle(artist.text, true) as Artist,
                    true
                )
            }

            for (row in parsed.contents!!.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!.withIndex()) {
                val description = row.value.description
                if (description != null) {
                    item.supplyDescription(description, true)
                    continue
                }

                val continuation: MediaItemLayout.Continuation? =
                    row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.PLAYLIST) }

                item_layouts.add(MediaItemLayout(
                    row.value.title?.text,
                    null,
                    if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
                    row.value.getMediaItems().toMutableList(),
                    continuation = continuation
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

        val video_details = DataApi.klaxon.parse<PlayerData>(buffered_reader)?.videoDetails
        if (video_details != null) {
        if (item_id == "_ZgZOaF04aE") println("BRUH 3")
            buffered_reader.close()
            item.supplyTitle(video_details.title, true)
            item.supplyArtist(Artist.fromId(video_details.channelId))
            return finish()
        }
        if (item_id == "_ZgZOaF04aE") println("BRUH 4")

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
        if (item_id == "_ZgZOaF04aE") println("BRUH 5")

        for (run in video.longBylineText.runs!!) {
        if (item_id == "_ZgZOaF04aE") println("BRUH 6")
            if (run.navigationEndpoint?.browseEndpoint?.page_type != "MUSIC_PAGE_TYPE_ARTIST") {
                continue
            }
        if (item_id == "_ZgZOaF04aE") println("BRUH 7")

            val artist = Artist.fromId(run.navigationEndpoint.browseEndpoint.browseId).supplyTitle(run.text)
            item.supplyArtist(artist as Artist, true)

            return finish()
        }
        if (item_id == "_ZgZOaF04aE") println("BRUH 8")

        val menu_artist = video.menu.menuRenderer.getArtist()?.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId
        if (menu_artist != null) {
        if (item_id == "_ZgZOaF04aE") println("BRUH 9")
            item.supplyArtist(Artist.fromId(menu_artist))
            return finish()
        }
        if (item_id == "_ZgZOaF04aE") println("BRUH 10")

        for (run in video.longBylineText.runs) {
        if (item_id == "_ZgZOaF04aE") println("BRUH 11")
            if (run.navigationEndpoint?.browseEndpoint?.page_type != "MUSIC_PAGE_TYPE_ALBUM") {
                continue
            }
        if (item_id == "_ZgZOaF04aE") println("BRUH 13")

            val playlist_result = Playlist.fromId(run.navigationEndpoint.browseEndpoint.browseId).loadData()
            if (playlist_result.isFailure) {
        if (item_id == "_ZgZOaF04aE") println("BRUH 14")
                return playlist_result
            }
        if (item_id == "_ZgZOaF04aE") println("BRUH 15")

            val artist = playlist_result.getOrThrowHere().artist
            if (artist != null) {
        if (item_id == "_ZgZOaF04aE") println("BRUH 16")
                item.supplyArtist(artist, true)
                return finish()
            }
        }
    }
    if (item_id == "_ZgZOaF04aE") println("BRUH 17")

    // 'next' endpoint has no artist, use 'player' instead
    request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/player")//?key=${getString(R.string.yt_i_api_key)}")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""{ "videoId": "$item_id" }"""))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
    if (item_id == "_ZgZOaF04aE") println("BRUH 18")
        return result.cast()
    }

    val response_body = result.getOrThrowHere().body!!.charStream()
    val video_data = DataApi.klaxon.parse<PlayerData>(response_body)!!
    response_body.close()

    item.supplyTitle(video_data.videoDetails!!.title, true)
    item.supplyArtist(Artist.fromId(video_data.videoDetails.channelId), true)

    if (item_id == "_ZgZOaF04aE") println("BRUH 19")
    return finish()
}
