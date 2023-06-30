package com.toasterofbread.spmp.api

import SpMp
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.data.AccountPlaylistItemData
import com.toasterofbread.spmp.model.mediaitem.data.ArtistItemData
import com.toasterofbread.spmp.model.mediaitem.data.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.data.SongItemData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeSubscribersString
import com.toasterofbread.spmp.ui.component.MediaItemLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response

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
        val hl = SpMp.data_language
        val request = Request.Builder()
            .ytUrl("/youtubei/v1/browse")
            .addYtHeaders()
            .post(Api.getYoutubeiRequestBody(
                mutableMapOf("browseId" to browse_id ).apply {
                    if (params != null) {
                        put("params", params)
                    }
                }
            ))
            .build()

        val result = Api.request(request)
        if (result.isFailure) {
            return@withContext result.cast()
        }

        val stream = result.getOrThrow().getStream()
        val parse_result: Result<YoutubeiBrowseResponse> = runCatching {
            Api.klaxon.parse(stream)!!
        }
        stream.close()
        
        val parsed = parse_result.fold(
            { it },
            { return@withContext Result.failure(it) }
        )

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

suspend fun processDefaultResponse(item: MediaItem, data: MediaItemData, response: Response, hl: String): Result<Unit>? {
    return withContext(Dispatchers.IO) {
        val response_body = response.getStream()

        val ret = run {
            if (data is MediaItemWithLayoutsData) {
                val str = response_body.reader().readText()
                val parse_result: Result<YoutubeiBrowseResponse> = runCatching {
                    Api.klaxon.parse(str)!!
                }
                response_body.close()
                
                val parsed: YoutubeiBrowseResponse = parse_result.fold(
                    { it },
                    { return@run Result.failure(it) }
                )

                // Skip unneeded information for radios
                if (item is Playlist && item.playlist_type == PlaylistType.RADIO) {
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
                            return@mapNotNull data.toMediaItem(hl)?.first.also { check(it is Song) }
                        }.toMutableList(),
                        continuation = continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.SONG, item.id) }
                    )

                    data.supplyFeedLayouts(listOf(layout), true)
                } else {
                    val header_renderer = parsed.header?.getRenderer()
                    if (header_renderer != null) {

                        data.supplyTitle(header_renderer.title.first_text, true)
                        data.supplyDescription(header_renderer.description?.first_text, true)
                        data.supplyThumbnailProvider(MediaItemThumbnailProvider.fromThumbnails(header_renderer.getThumbnails()))

                        header_renderer.subtitle?.runs?.also { subtitle ->
                            val artist_run = subtitle.firstOrNull {
                                it.navigationEndpoint?.browseEndpoint?.getPageType() == "MUSIC_PAGE_TYPE_USER_CHANNEL"
                            }
                            if (artist_run != null) {
                                data.supplyArtist(
                                    Artist.fromId(artist_run.navigationEndpoint!!.browseEndpoint!!.browseId).editArtistData {
                                        supplyTitle(artist_run.text, false)
                                    },
                                    false
                                )
                            }

                            if (data is AccountPlaylistItemData) {
                                data.supplyYear(
                                    subtitle.lastOrNull { last_run ->
                                        last_run.text.all { it.isDigit() }
                                    }?.text?.toInt(),
                                    true
                                )
                            }
                        }

                        if (data is AccountPlaylistItemData) {
                            header_renderer.secondSubtitle?.runs?.also { second_subtitle ->
                                for (run in second_subtitle.reversed().withIndex()) {
                                    when (run.index) {
                                        0 -> data.supplyTotalDuration(parseYoutubeDurationString(run.value.text, hl), true)
                                        1 -> data.supplyItemCount(run.value.text.filter { it.isDigit() }.toInt(), true)
                                    }
                                }
                            }
                        }

                        if (header_renderer.subscriptionButton != null && data is ArtistItemData) {
                            val subscribe_button = header_renderer.subscriptionButton.subscribeButtonRenderer
                            data.supplySubscribeChannelId(subscribe_button.channelId, true)
                            data.supplySubscriberCount(parseYoutubeSubscribersString(subscribe_button.subscriberCountText.first_text, hl), true)
                            (item as Artist).subscribed = subscribe_button.subscribed
                        }
                    }

                    val item_layouts: MutableList<MediaItemLayout> = mutableListOf()
                    for (row in parsed.contents!!.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!.withIndex()) {
                        val description = row.value.description
                        if (description != null) {
                            data.supplyDescription(description, true)
                            continue
                        }

                        val continuation: MediaItemLayout.Continuation? =
                            row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation?.let {
                                MediaItemLayout.Continuation(
                                    it,
                                    MediaItemLayout.Continuation.Type.PLAYLIST
                                )
                            }

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

                        val items = row.value.getMediaItemsAndSetIds(hl)

                        item_layouts.add(
                            MediaItemLayout(
                                layout_title,
                                null,
                                if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
                                items.map { it.first }.toMutableList(),
                                continuation = continuation,
                                view_more = view_more
                            )
                        )

                        if (item is AccountPlaylist) {
                            item.item_set_ids = if (items.all { it.second != null }) items.map { it.second!! } else null
                        }
                    }
                    data.supplyFeedLayouts(item_layouts, true)
                }

                return@run Result.success(Unit)
            } else null
        }

        if (ret != null) {
            return@withContext ret
        }

        check(item is Song)

        val tabs: List<YoutubeiNextResponse.Tab> = try {
            Api.klaxon.parse<YoutubeiNextResponse>(response_body)!!
                .contents
                .singleColumnMusicWatchNextResultsRenderer
                .tabbedRenderer
                .watchNextTabbedResultsRenderer
                .tabs
        }
        catch (e: Throwable) {
            return@withContext Result.failure(e)
        }
        finally {
            response_body.close()
        }

        if (data is SongItemData) {
            val related = tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint?.browseId
            data.supplyRelatedBrowseId(related, true)
        }

        val video: YoutubeiNextResponse.PlaylistPanelVideoRenderer = try {
            tabs[0].tabRenderer.content!!.musicQueueRenderer.content.playlistPanelRenderer.contents.first().playlistPanelVideoRenderer!!
        }
        catch (e: Throwable) {
            return@withContext Result.failure(e)
        }

        data.supplyTitle(video.title.first_text, true)

        val result = video.getArtist(item)
        if (result.isFailure) {
            return@withContext result.cast()
        }

        val (artist, certain) = result.getOrThrow()
        if (artist != null) {
            data.supplyArtist(artist, certain)
            return@withContext Result.success(Unit)
        }

        return@withContext null
    }
}

suspend fun loadMediaItemData(
    item: MediaItem, 
    item_id: String = item.id, 
    browse_params: String? = null
): Result<Unit> {
    if (item is Artist && item.is_for_item) {
        return Result.success(Unit)
    }

    return withContext(Dispatchers.IO) {
        val url = if (item is Song) "/youtubei/v1/next" else "/youtubei/v1/browse"
        val body =
            if (item is Song)
                mapOf(
                    "enablePersistentPlaylistPanel" to true,
                    "isAudioOnly" to true,
                    "videoId" to item_id,
                )
            else 
                mutableMapOf(
                    "browseId" to item_id
                ).apply {
                    if (browse_params != null) {
                        put("params", browse_params)
                    }
                }

        val hl = SpMp.data_language
        var request: Request = Request.Builder()
            .ytUrl(url)
            .addYtHeaders()
            .post(Api.getYoutubeiRequestBody(
                body,
                if (item is Artist) Api.Companion.YoutubeiContextType.MOBILE
                else Api.Companion.YoutubeiContextType.BASE
            ))
            .build()

        val response = Api.request(request).getOrNull()
        coroutineContext.job.invokeOnCompletion {
            response?.close()
        }

        return@withContext item.data.run {
            if (response != null) {
                val result = processDefaultResponse(item, this, response, hl)
                if (result != null) {
                    return@run result
                }
            }

            // 'next' endpoint has no artist, use 'player' instead
            request = Request.Builder()
                .ytUrl("/youtubei/v1/player")
                .addYtHeaders()
                .post(Api.getYoutubeiRequestBody(mapOf("videoId" to item_id)))
                .build()

            val result = Api.request(request)
            if (result.isFailure) {
                return@run result.cast()
            }

            val stream = result.getOrThrowHere().getStream()
            val video_data = Api.klaxon.parse<PlayerData>(stream)!!
            stream.close()

            if (video_data.videoDetails == null) {
                return@run Result.failure(NotImplementedError("videoDetails is null ($item_id)"))
            }

            supplyTitle(video_data.videoDetails.title, true)
            supplyArtist(Artist.fromId(video_data.videoDetails.channelId), true)

            return@run Result.success(Unit)
        }
    }
}
