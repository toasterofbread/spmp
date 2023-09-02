package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeSubscribersString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.getStream
import com.toasterofbread.spmp.youtubeapi.model.Header
import com.toasterofbread.spmp.youtubeapi.model.HeaderRenderer
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.io.InputStream

class InvalidRadioException: Throwable()

suspend fun processSong(song: SongData, response_body: InputStream, api: YoutubeApi): Result<Unit> {
    val tabs: List<YoutubeiNextResponse.Tab> = try {
        api.klaxon.parse<YoutubeiNextResponse>(response_body)!!
            .contents
            .singleColumnMusicWatchNextResultsRenderer
            .tabbedRenderer
            .watchNextTabbedResultsRenderer
            .tabs
    }
    catch (e: Throwable) {
        return Result.failure(e)
    }

    song.lyrics_browse_id = tabs.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint?.browseId
    song.related_browse_id = tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint?.browseId

    val video: YoutubeiNextResponse.PlaylistPanelVideoRenderer = try {
        tabs[0].tabRenderer.content!!.musicQueueRenderer.content.playlistPanelRenderer.contents.first().playlistPanelVideoRenderer!!
    }
    catch (e: Throwable) {
        return Result.failure(e)
    }

    song.title = video.title.first_text

    video.getArtist(song, api.context).fold(
        { artist ->
            song.artist = artist
            return Result.success(Unit)
        },
        { return Result.failure(it) }
    )
}

suspend fun processDefaultResponse(item: MediaItemData, response: Response, hl: String, api: YoutubeApi): Result<Unit> {
    return withContext(Dispatchers.IO) {
        val response_body = response.getStream(api)
        return@withContext response_body.use {
            if (item is SongData) {
                return@use processSong(item, response_body, api)
            }

            val parse_result: Result<YoutubeiBrowseResponse> = runCatching {
                response_body.use {
                    api.klaxon.parse(it)!!
                }
            }

            val parsed: YoutubeiBrowseResponse = parse_result.fold(
                { it },
                { return@use Result.failure(it) }
            )

            // Skip unneeded information for radios
            if (item is RemotePlaylistData && item.playlist_type == PlaylistType.RADIO) {
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
                    val data_item = data.toMediaItemData(hl)?.first
                    if (data_item is SongData) {
                        return@mapNotNull data_item
                    }
                    return@mapNotNull null
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
                val header_renderer: HeaderRenderer? = parsed.header?.getRenderer()
                if (header_renderer != null) {
                    item.title = header_renderer.title!!.first_text
                    item.description = header_renderer.description?.first_text
                    item.thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(header_renderer.getThumbnails())

                    header_renderer.subtitle?.runs?.also { subtitle ->
                        if (item is MediaItem.DataWithArtist) {
                            val artist_run = subtitle.firstOrNull {
                                it.navigationEndpoint?.browseEndpoint?.getMediaItemType() == MediaItemType.ARTIST
                            }
                            if (artist_run != null) {
                                item.artist = ArtistData(artist_run.navigationEndpoint!!.browseEndpoint!!.browseId).apply {
                                    title = artist_run.text
                                }
                            }
                        }

                        if (item is RemotePlaylistData) {
                            item.year = subtitle.lastOrNull { last_run ->
                                last_run.text.all { it.isDigit() }
                            }?.text?.toInt()
                        }
                    }

                    if (item is RemotePlaylistData) {
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

                val own_channel: Artist? = api.user_auth_state?.own_channel
                if (item is RemotePlaylistData && own_channel != null) {
                    val menu_buttons: List<Header.TopLevelButton>? =
                        parsed.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons

                    if (menu_buttons?.any { it.buttonRenderer?.icon?.iconType == "EDIT" } == true) {
                        item.owner = own_channel
                    }
                }

                val section_list_renderer = with (parsed.contents!!) {
                    if (singleColumnBrowseResultsRenderer != null) {
                        singleColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
                    }
                    else {
                        twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                    }
                }

                for (row in section_list_renderer?.contents.orEmpty().withIndex()) {
                    val description = row.value.description
                    if (description != null) {
                        item.description = description
                        continue
                    }

                    val items = row.value.getMediaItemsAndSetIds(hl)
                    val items_mapped = items.map {
                        val list_item = it.first
                        if (item is Artist && list_item is SongData && list_item.song_type == SongType.PODCAST) {
                            list_item.artist = item
                        }
                        list_item
                    }

                    val continuation_token =
                        row.value.musicPlaylistShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

                    if (item is RemotePlaylistData) {
                        item.items = items_mapped.filterIsInstance<SongData>()
                        item.continuation = continuation_token?.let {
                            MediaItemLayout.Continuation(
                                it,
                                MediaItemLayout.Continuation.Type.PLAYLIST
                            )
                        }
                        item.item_set_ids = if (items.all { it.second != null }) items.map { it.second!! } else null

                        // Playlists's don't display indices
                        if (row.value.musicShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.index != null) {
                            item.playlist_type = PlaylistType.ALBUM
                        }

                        break
                    }

                    check(item is ArtistData)

                    val layout_title = row.value.title?.text?.let {
                        if (item.isOwnChannel(api)) LocalisedYoutubeString.Type.OWN_CHANNEL.create(it)
                        else LocalisedYoutubeString.mediaItemPage(it, item.getType())
                    }

                    val view_more = row.value.getNavigationEndpoint()?.getViewMore()
                    if (view_more is MediaItemLayout.MediaItemViewMore) {
                        val view_more_item = view_more.media_item as MediaItemData
                        view_more_item.title = layout_title?.getString()
                        if (view_more_item is MediaItem.DataWithArtist) {
                            view_more_item.artist = item
                        }
                    }

                    val new_layout = ArtistLayout.create(item.id).also { layout ->
                        layout.items = items_mapped.toMutableList()
                        layout.title = layout_title
                        layout.type = if (row.index == 0) MediaItemLayout.Type.NUMBERED_LIST else MediaItemLayout.Type.GRID
                        layout.view_more = view_more
                        layout.playlist = continuation_token?.let {
                            RemotePlaylistRef(it)
                        }
                    }

                    if (item.layouts == null) {
                        item.layouts = mutableListOf()
                    }
                    item.layouts!!.add(new_layout)
                }
            }

            return@use Result.success(Unit)
        }
    }
}
