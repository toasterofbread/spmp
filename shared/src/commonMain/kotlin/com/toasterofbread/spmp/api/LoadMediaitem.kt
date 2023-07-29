package com.toasterofbread.spmp.api

import SpMp
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayoutData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeSubscribersString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream

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

suspend fun loadMediaItemData(
    item: MediaItemData,
    db: Database,
    browse_params: String? = null
): Result<Unit> {
    val item_id = item.id

    val result =
        if (item is Artist && item.is_for_item) Result.success(Unit)
        else withContext(Dispatchers.IO) {
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

            if (response != null) {
                val result = processDefaultResponse(item, response, hl, db)
                if (result != null) {
                    return@withContext result
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
                return@withContext result.cast()
            }

            val stream = result.getOrThrowHere().getStream()
            val video_data = Api.klaxon.parse<PlayerData>(stream)!!
            stream.close()

            if (video_data.videoDetails == null) {
                return@withContext Result.success(Unit)
            }

            item.title = video_data.videoDetails.title
            if (item is MediaItem.DataWithArtist) {
                item.artist = ArtistData(video_data.videoDetails.channelId)
            }

            return@withContext Result.success(Unit)
        }

    result.onSuccess {
        item.saveToDatabase(db)
    }

    return result
}

class InvalidRadioException: Throwable()

suspend fun processSong(song: SongData, response_body: InputStream, db: Database): Result<Unit>? {
    val tabs: List<YoutubeiNextResponse.Tab> = try {
        Api.klaxon.parse<YoutubeiNextResponse>(response_body)!!
            .contents
            .singleColumnMusicWatchNextResultsRenderer
            .tabbedRenderer
            .watchNextTabbedResultsRenderer
            .tabs
    }
    catch (e: Throwable) {
        return Result.failure(e)
    }

    song.related_browse_id = tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint?.browseId

    val video: YoutubeiNextResponse.PlaylistPanelVideoRenderer = try {
        tabs[0].tabRenderer.content!!.musicQueueRenderer.content.playlistPanelRenderer.contents.first().playlistPanelVideoRenderer!!
    }
    catch (e: Throwable) {
        return Result.failure(e)
    }

    song.title = video.title.first_text

    video.getArtist(song, db).fold(
        { artist ->
            song.artist = artist
            return Result.success(Unit)
        },
        { return Result.failure(it) }
    )
}

suspend fun processDefaultResponse(item: MediaItemData, response: Response, hl: String, db: Database): Result<Unit>? {
    return withContext(Dispatchers.IO) {
        val response_body = response.getStream()
        return@withContext response_body.use {
            if (item is SongData) {
                return@use processSong(item, response_body, db)
            }

            val parse_result: Result<YoutubeiBrowseResponse> = runCatching {
                response_body.use {
                    Api.klaxon.parse(it)!!
                }
            }

            val parsed: YoutubeiBrowseResponse = parse_result.fold(
                { it },
                { return@use Result.failure(it) }
            )

            // Skip unneeded information for radios
            if (item is PlaylistData && item.playlist_type == PlaylistType.RADIO) {
                val playlist_shelf = parsed
                    .contents!!
                    .singleColumnBrowseResultsRenderer!!
                    .tabs[0]
                    .tabRenderer
                    .content!!
                    .sectionListRenderer
                    .contents!![0]
                    .musicPlaylistShelfRenderer!!

                item.items = playlist_shelf.contents!!.mapNotNull { data ->
                    return@mapNotNull data.toMediaItem(hl)?.first
                }

                val continuation = playlist_shelf.continuations?.firstOrNull()?.nextRadioContinuationData?.continuation
                if (continuation != null) {
                    item.continuation = MediaItemLayout.Continuation(continuation, MediaItemLayout.Continuation.Type.SONG, item.id)
                }

                val header_renderer = parsed.header?.getRenderer()
                if (header_renderer != null) {
                    item.thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(header_renderer.getThumbnails())
                }
            }
            else {
                val header_renderer = parsed.header?.getRenderer()
                if (header_renderer != null) {
                    item.title = header_renderer.title!!.first_text
                    item.description = header_renderer.description?.first_text
                    item.thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(header_renderer.getThumbnails())

                    header_renderer.subtitle?.runs?.also { subtitle ->

                        if (item is MediaItem.DataWithArtist) {
                            val artist_run = subtitle.firstOrNull {
                                it.navigationEndpoint?.browseEndpoint?.getPageType() == "MUSIC_PAGE_TYPE_USER_CHANNEL"
                            }
                            if (artist_run != null) {
                                item.artist = ArtistData(artist_run.navigationEndpoint!!.browseEndpoint!!.browseId).apply {
                                    title = artist_run.text
                                }
                            }
                        }

                        if (item is PlaylistData) {
                            item.year = subtitle.lastOrNull { last_run ->
                                last_run.text.all { it.isDigit() }
                            }?.text?.toInt()
                        }
                    }

                    if (item is PlaylistData) {
                        header_renderer.secondSubtitle?.runs?.also { second_subtitle ->
                            for (run in second_subtitle.reversed().withIndex()) {
                                when (run.index) {
                                    0 -> item.total_duration = parseYoutubeDurationString(run.value.text, hl)
                                    1 -> item.item_count = run.value.text.filter { it.isDigit() }.toInt()
                                }
                            }
                        }
                    }

                    if (header_renderer.subscriptionButton != null && item is ArtistData) {
                        val subscribe_button = header_renderer.subscriptionButton.subscribeButtonRenderer
                        item.subscribe_channel_id = subscribe_button.channelId
                        item.subscriber_count = parseYoutubeSubscribersString(subscribe_button.subscriberCountText.first_text, hl)
                        item.subscribed = subscribe_button.subscribed
                    }
                }

                val rows = with (parsed.contents!!) {
                    if (singleColumnBrowseResultsRenderer != null) {
                        singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!
                    }
                    else {
                        twoColumnBrowseResultsRenderer!!.secondaryContents.sectionListRenderer.contents!!
                    }
                }

                for (row in rows.withIndex()) {
                    val description = row.value.description
                    if (description != null) {
                        item.description = description
                        continue
                    }

                    val items = row.value.getMediaItemsAndSetIds(hl)
                    val items_mapped = items.map {
                        if (it.first is SongData && item is Artist) {
                            val song_data = it.first as SongData
                            if (song_data.song_type == SongType.PODCAST) {
                                song_data.artist = item
                            }
                        }

                        it.first
                    }

                    val continuation_playlist_id = row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

                    if (item is PlaylistData) {
                        item.items = items_mapped
                        item.continuation = continuation_playlist_id?.let {
                            MediaItemLayout.Continuation(
                                it,
                                MediaItemLayout.Continuation.Type.PLAYLIST
                            )
                        }
                        item.item_set_ids = if (items.all { it.second != null }) items.map { it.second!! } else null
                        break
                    }

                    val layout_title = row.value.title?.text?.let {
                        if (item is Artist && item.isOwnChannel()) LocalisedYoutubeString.Type.OWN_CHANNEL.create(it)
                        else LocalisedYoutubeString.mediaItemPage(it, item)
                    }

                    val view_more = row.value.getNavigationEndpoint()?.getViewMore()
                    if (item is ArtistData && view_more is MediaItemLayout.MediaItemViewMore) {
                        val view_more_item = view_more.media_item as MediaItemData
                        view_more_item.title = layout_title?.getString()
                        if (view_more_item is MediaItem.DataWithArtist) {
                            view_more_item.artist = item
                        }
                    }

                    check(item is ArtistData)

                    item.layouts.add(
                        ArtistLayoutData(
                            items = items_mapped.toMutableList(),
                            title = layout_title,
                            subtitle = null,
                            type = if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID,
                            view_more = view_more,
                            playlist = continuation_playlist_id?.let {
                                AccountPlaylistRef(it)
                            }
                        )
                    )

                }

                when (item) {
                    is ArtistData -> TODO()
                    is PlaylistData -> TODO()
                    is SongData -> TODO()
                }
            }

            return@use Result.success(Unit)
        }
    }
}
