package com.spectre7.spmp.api

import SpMp
import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.getStream
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.Request
import java.util.regex.Pattern

// TODO Organise and split

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

suspend fun loadBrowseId(browse_id: String, params: String? = null): Result<List<MediaItemLayout>> {
    return withContext(Dispatchers.IO) {
        val params_str = if (params == null) "" else """, "params": "$params" """
        val hl = SpMp.data_language
        val request = Request.Builder()
            .ytUrl("/youtubei/v1/browse")
            .addYtHeaders()
            .post(DataApi.getYoutubeiRequestBody("""{ "browseId": "$browse_id"$params_str }"""))
            .build()

        val result = DataApi.request(request)
        if (result.isFailure) {
            return@withContext result.cast()
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
                row.value.getMediaItems(hl).toMutableList(),
                continuation = continuation,
                view_more = view_more
            ))
        }

        return@withContext Result.success(ret)
    }
}

class InvalidRadioException: Throwable()

suspend fun loadMediaItemData(item: MediaItem): Result<MediaItem?> {
    val item_id = item.id

    if (item is Artist && item.is_for_item) {
        return Result.success(item)
    }

    return withContext(Dispatchers.IO) {

        val url = if (item is Song) "/youtubei/v1/next" else "/youtubei/v1/browse"
        val body =
            if (item is Song)
                """{
                "enablePersistentPlaylistPanel": true,
                "isAudioOnly": true,
                "videoId": "$item_id"
            }"""
            else """{ "browseId": "$item_id" }"""

        val hl = SpMp.data_language
        var request: Request = Request.Builder()
            .ytUrl(url)
            .addYtHeaders()
            .post(DataApi.getYoutubeiRequestBody(
                body,
                if (item is Artist) DataApi.Companion.YoutubeiContextType.MOBILE
                else DataApi.Companion.YoutubeiContextType.BASE
            ))
            .build()

        val response = DataApi.request(request).getOrNull()
        return@withContext item.editDataSuspend {
            if (response != null) {
                val response_body = response.getStream()

                if (this is MediaItemWithLayoutsData) {
                    val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(response_body)!!
                    response_body.close()

                    // Skip unneeded information for radios
                    if (item is Playlist && item.playlist_type == Playlist.PlaylistType.RADIO) {
                        val playlist_shelf = parsed
                            .contents!!
                            .singleColumnBrowseResultsRenderer
                            .tabs[0]
                            .tabRenderer
                            .content!!
                            .sectionListRenderer
                            .contents!![0]
                            .musicPlaylistShelfRenderer!!

                        val continuation = playlist_shelf.continuations?.firstOrNull()?.nextRadioContinuationData?.continuation

                        val layout = MediaItemLayout(
                            null, null,
                            MediaItemLayout.Type.LIST,
                            playlist_shelf.contents!!.mapNotNull { data ->
                                return@mapNotNull data.toMediaItem(hl).also { check(it is Song) }
                            }.toMutableList(),
                            continuation = continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.SONG, item_id) }
                        )

                        supplyFeedLayouts(listOf(layout), true)
                    }
                    else {
                        if (parsed.header != null) {
                            val header_renderer = parsed.header.getRenderer()

                            supplyTitle(header_renderer.title.first_text, true)
                            supplyDescription(header_renderer.description?.first_text, true)
                            supplyThumbnailProvider(MediaItemThumbnailProvider.fromThumbnails(header_renderer.getThumbnails()))

                            header_renderer.subtitle?.runs?.also { subtitle ->
                                val artist_run = subtitle.firstOrNull {
                                    it.navigationEndpoint?.browseEndpoint?.getPageType() == "MUSIC_PAGE_TYPE_USER_CHANNEL"
                                }
                                if (artist_run != null) {
                                    supplyArtist(
                                        Artist.fromId(artist_run.navigationEndpoint!!.browseEndpoint!!.browseId).editArtistData {
                                            supplyTitle(artist_run.text, true)
                                        },
                                        false
                                    )
                                }

                                if (this is PlaylistItemData) {
                                    supplyYear(subtitle.lastOrNull { it.text.all { it.isDigit() } }?.text?.toInt(), true)
                                }
                            }

                            if (this is PlaylistItemData) {
                                header_renderer.secondSubtitle?.runs?.also { second_subtitle ->
                                    check(second_subtitle.size == 2) { second_subtitle.toString() }

                                    supplyItemCount(second_subtitle[0].text.filter { it.isDigit() }.toInt(), true)
                                    supplyTotalDuration(parseYoutubeDurationString(second_subtitle[1].text, hl), true)
                                }
                            }

                            if (header_renderer.subscriptionButton != null && this is ArtistItemData) {
                                val subscribe_button = header_renderer.subscriptionButton.subscribeButtonRenderer
                                supplySubscribeChannelId(subscribe_button.channelId, true)
                                supplySubscriberCount(parseYoutubeSubscribersString(subscribe_button.subscriberCountText.first_text, hl), true)
                                (item as Artist).subscribed = subscribe_button.subscribed
                            }
                        }

                        val item_layouts: MutableList<MediaItemLayout> = mutableListOf()
                        for (row in parsed.contents!!.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!.withIndex()) {
                            val description = row.value.description
                            if (description != null) {
                                supplyDescription(description, true)
                                continue
                            }

                            val continuation: MediaItemLayout.Continuation? =
                                row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.PLAYLIST) }

                            val layout_title = row.value.title?.text?.let {
                                if (item is Artist && item.is_own_channel) LocalisedYoutubeString.ownChannel(it)
                                else LocalisedYoutubeString.mediaItemPage(it, item.type)
                            }

                            val view_more = row.value.getNavigationEndpoint()?.getViewMore()
                            view_more?.layout_type = MediaItemLayout.Type.LIST
                            if (item is Artist) {
                                view_more?.media_item?.editData {
                                    supplyArtist(item, true)
                                    supplyTitle(layout_title?.getString(), false)
                                }
                            }

                            item_layouts.add(MediaItemLayout(
                                layout_title,
                                null,
                                if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
                                row.value.getMediaItems(hl).toMutableList(),
                                continuation = continuation,
                                view_more = view_more
                            ))
                        }
                        supplyFeedLayouts(item_layouts, true)
                    }

                    return@editDataSuspend Result.success(item)
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

                supplyTitle(video.title.first_text, true)

                val result = video.getArtist(item)
                if (result.isFailure) {
                    return@editDataSuspend result.cast()
                }

                val (artist, certain) = result.getOrThrow()
                if (artist != null) {
                    supplyArtist(artist, certain)
                    return@editDataSuspend Result.success(item)
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
                return@editDataSuspend result.cast()
            }

            val stream = result.getOrThrowHere().getStream()
            val video_data = DataApi.klaxon.parse<PlayerData>(stream)!!
            stream.close()

            if (video_data.videoDetails == null) {
                return@editDataSuspend Result.success(null)
            }

            supplyTitle(video_data.videoDetails.title, true)
            supplyArtist(Artist.fromId(video_data.videoDetails.channelId), true)

            return@editDataSuspend Result.success(item)
        }
    }
}
