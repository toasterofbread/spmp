package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.getStream
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.printJson
import okhttp3.Request
import java.io.BufferedReader
import java.io.Reader

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

fun loadBrowseId(browse_id: String, params: String? = null): Result<List<MediaItemLayout>> {
    val params_str = if (params == null) "" else """, "params": "$params" """
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody("""{ "browseId": "$browse_id"$params_str }"""))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(stream)!!
    stream.close()

    val ret: MutableList<MediaItemLayout> = mutableListOf()
    for (row in parsed.contents!!.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!.withIndex()) {
        if (row.value.description != null) {
            continue
        }

        val continuation: MediaItemLayout.Continuation? =
            row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.PLAYLIST) }

        val view_more = row.value.getNavigationEndpoint()?.getViewMore()
        view_more?.layout_type = MediaItemLayout.Type.LIST

        ret.add(MediaItemLayout(
            row.value.title?.text,
            null,
            if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
            row.value.getMediaItems().toMutableList(),
            continuation = continuation,
            view_more = view_more
        ))
    }

    return Result.success(ret)
}

fun loadMediaItemData(item: MediaItem): Result<MediaItem?> {
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

    val is_radio = item is Playlist && item.playlist_type == Playlist.PlaylistType.RADIO
    val url = if (item is Song || is_radio) "/youtubei/v1/next" else "/youtubei/v1/browse"
    val body =
        if (item is Song)
            """{
                "enablePersistentPlaylistPanel": true,
                "isAudioOnly": true,
                "videoId": "$item_id"
            }"""
        else if (is_radio)
            """{ "playlistId": "$item_id", "params": "wAEB" }"""
        else """{ "browseId": "$item_id" }"""

    var request: Request = Request.Builder()
        .ytUrl(url)
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody(body))
        .build()

    val response = DataApi.request(request).getOrNull()
    if (response != null) {
        val response_body = response.getStream()

        if (is_radio) {
            check(item is Playlist)

            val parsed = DataApi.klaxon.parse<YoutubeiNextResponse>(response_body)!!
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
            response_body.close()

            val continuation = parsed.continuations?.firstOrNull()?.nextRadioContinuationData?.continuation

            val layout = MediaItemLayout(
                null, null,
                MediaItemLayout.Type.LIST,
                parsed.contents.mapNotNull { data ->
                    val renderer = data.playlistPanelVideoRenderer ?: return@mapNotNull null

                    val song = Song.fromId(renderer.videoId)
                    song.supplyTitle(renderer.title.first_text, true)
                    song.supplyThumbnailProvider(MediaItem.ThumbnailProvider.fromThumbnails(renderer.thumbnail.thumbnails))

                    val artist_result = renderer.getArtist(song)
                    if (artist_result.isFailure) {
                        return artist_result.cast()
                    }

                    val (artist, certain) = artist_result.getOrThrow()
                    song.supplyArtist(artist, certain)

                    return@mapNotNull song
                }.toMutableList(),
                continuation = continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.SONG, item_id) }
            )

            item.supplyFeedLayouts(listOf(layout), true)

            return finish()
        }

        if (item is MediaItemWithLayouts) {
            val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(response_body)!!
            response_body.close()

            val header_renderer: HeaderRenderer?
            if (parsed.header != null) {
                header_renderer = parsed.header.getRenderer()
                item.supplyTitle(header_renderer.title.first_text, true)
                item.supplyDescription(header_renderer.description?.first_text, true)
                item.supplyThumbnailProvider(MediaItem.ThumbnailProvider.fromThumbnails(header_renderer.getThumbnails()))

                val artist = header_renderer.subtitle?.runs?.firstOrNull {
                    it.navigationEndpoint?.browseEndpoint?.getPageType() == "MUSIC_PAGE_TYPE_USER_CHANNEL"
                }
                if (artist != null) {
                    item.supplyArtist(
                        Artist
                            .fromId(artist.navigationEndpoint!!.browseEndpoint!!.browseId)
                            .supplyTitle(artist.text, true) as Artist,
                        true
                    )
                }
            }
            else {
                header_renderer = null
            }

            val item_layouts: MutableList<MediaItemLayout> = mutableListOf()
            for (row in parsed.contents!!.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!.withIndex()) {
                val description = row.value.description
                if (description != null) {
                    item.supplyDescription(description, true)
                    continue
                }

                val continuation: MediaItemLayout.Continuation? =
                    row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.PLAYLIST) }

                val view_more = row.value.getNavigationEndpoint()?.getViewMore()
                view_more?.layout_type = MediaItemLayout.Type.LIST

                item_layouts.add(MediaItemLayout(
                    row.value.title?.text,
                    null,
                    if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
                    row.value.getMediaItems().toMutableList(),
                    continuation = continuation,
                    view_more = view_more
                ))
            }
            item.supplyFeedLayouts(item_layouts, true)

            if (item is Artist && header_renderer?.subscriptionButton != null) {
                val subscribe_button = header_renderer.subscriptionButton.subscribeButtonRenderer
                item.supplySubscribeChannelId(subscribe_button.channelId, true)
                item.supplySubscriberCountText(subscribe_button.subscriberCountText.first_text, true)
                item.subscribed = subscribe_button.subscribed
            }

            return finish()
        }

        check(item is Song)

        val buffered_reader = BufferedReader(response_body.reader())
        buffered_reader.mark(Int.MAX_VALUE)

        val video_details = DataApi.klaxon.parse<PlayerData>(buffered_reader)?.videoDetails
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

        val result = video.getArtist(item)
        if (result.isFailure) {
            return result.cast()
        }

        val (artist, certain) = result.getOrThrow()
        if (artist != null) {
            item.supplyArtist(artist, certain)
            return finish()
        }
    }

    // 'next' endpoint has no artist, use 'player' instead
    request = Request.Builder()
        .ytUrl("/youtubei/v1/player")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody("""{ "videoId": "$item_id" }"""))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrowHere().getStream()
    val video_data = DataApi.klaxon.parse<PlayerData>(stream)!!
    stream.close()

    if (video_data.videoDetails == null) {
        return Result.success(null)
    }

    item.supplyTitle(video_data.videoDetails.title, true)
    item.supplyArtist(Artist.fromId(video_data.videoDetails.channelId), true)

    return finish()
}
