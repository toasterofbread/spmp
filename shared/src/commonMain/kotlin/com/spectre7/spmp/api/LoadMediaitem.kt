package com.spectre7.spmp.api

import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.getStream
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import okhttp3.Request
import java.util.regex.Pattern

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
            row.value.title?.text?.let { LocalisedYoutubeString.raw(it) },
            null,
            if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
            row.value.getMediaItems().toMutableList(),
            continuation = continuation,
            view_more = view_more
        ))
    }

    return Result.success(ret)
}

private fun unescape(input: String): String {
    val regex = "\\\\x([0-9a-fA-F]{2})"
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(input)
    val sb = StringBuffer()
    while (matcher.find()) {
        val hex = matcher.group(1)
        val decimal = Integer.parseInt(hex, 16)
        matcher.appendReplacement(sb, decimal.toChar().toString())
    }
    matcher.appendTail(sb)
    return sb.toString()
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
    val url = if (item is Song) "/youtubei/v1/next" else if (is_radio) "/browse/$item_id" else "/youtubei/v1/browse"
    val body =
        if (item is Song)
            """{
                "enablePersistentPlaylistPanel": true,
                "isAudioOnly": true,
                "videoId": "$item_id"
            }"""
        else if (is_radio)
            null
        else """{ "browseId": "$item_id" }"""

    var request: Request = Request.Builder()
        .ytUrl(url)
        .addYtHeaders(body == null)
        .apply {
            if (body != null) post(DataApi.getYoutubeiRequestBody(body))
        }
        .build()

    val response = DataApi.request(request).getOrNull()
    if (response != null) {
        val response_body = response.getStream()

        if (is_radio) {
            check(item is Playlist)

            val string = response_body.reader().readText()
            response_body.close()

            val start_str = "JSON.parse('\\x7b\\x22browseId\\x22:\\x22$item_id\\x22\\x7d'), data:"
            val start = string.indexOf(start_str)
            check(start != -1)

            val end = string.indexOf('}', start + start_str.length)
            check(end != -1)

            val json_reader = unescape(string.substring(start + start_str.length, end).trim().trim('\'')).reader()
            val parsed = DataApi.klaxon.parse<YoutubeiBrowseResponse>(json_reader)!!
                .contents!!
                .singleColumnBrowseResultsRenderer
                .tabs[0]
                .tabRenderer
                .content!!
                .sectionListRenderer
                .contents!![0]
                .musicPlaylistShelfRenderer!!
            json_reader.close()

            val continuation = parsed.continuations?.firstOrNull()?.nextRadioContinuationData?.continuation

            val layout = MediaItemLayout(
                null, null,
                MediaItemLayout.Type.LIST,
                parsed.contents.mapNotNull { data ->
                    return@mapNotNull data.toMediaItem().also { check(it is Song) }
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
                    row.value.title?.text?.let {
                        if (item is Artist && item.is_own_channel) LocalisedYoutubeString.ownChannel(it)
                        else LocalisedYoutubeString.mediaItemPage(it, item.type)
                    },
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

//        val buffered_reader = BufferedReader(response_body.reader())
//        buffered_reader.mark(Int.MAX_VALUE)
//
//        val video_details = DataApi.klaxon.parse<PlayerData>(buffered_reader)?.videoDetails
//        if (video_details != null) {
//            buffered_reader.close()
//            item.supplyTitle(video_details.title, true)
//            item.supplyArtist(Artist.fromId(video_details.channelId))
//            return finish()
//        }

//        buffered_reader.reset()
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
