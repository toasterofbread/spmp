package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemViewMore
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeLocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeSubscribersString
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.getReader
import com.toasterofbread.spmp.youtubeapi.model.Header
import com.toasterofbread.spmp.youtubeapi.model.HeaderRenderer
import com.toasterofbread.spmp.youtubeapi.model.TextRun
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.io.Reader

class InvalidRadioException: Throwable()

suspend fun processSong(song: SongData, response_body: Reader, api: YoutubeApi): Result<Unit> {
    val tabs: List<YoutubeiNextResponse.Tab> = try {
        api.gson.fromJson<YoutubeiNextResponse>(response_body)
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
        tabs[0].tabRenderer.content!!.musicQueueRenderer.content!!.playlistPanelRenderer.contents.first().playlistPanelVideoRenderer!!
    }
    catch (e: Throwable) {
        return Result.failure(e)
    }

    song.title = video.title.first_text
    song.explicit = video.badges?.any { it.isExplicit() } == true

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
        val response_reader: Reader = response.getReader(api)
        return@withContext response_reader.use { reader ->
            if (item is SongData) {
                return@use processSong(item, response_reader, api)
            }

            val parse_result: Result<YoutubeiBrowseResponse> = runCatching {
                api.gson.fromJson(reader)
            }

            val parsed: YoutubeiBrowseResponse = parse_result.fold(
                { it },
                { return@use Result.failure(it) }
            )

            // Skip unneeded information for radios
            if (item is RemotePlaylistData && item.playlist_type == PlaylistType.RADIO) {
                try {
                    val playlist_shelf = parsed
                        .contents!!
                        .singleColumnBrowseResultsRenderer!!
                        .tabs[0]
                        .tabRenderer
                        .content!!
                        .sectionListRenderer!!
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
                }
                catch (e: Throwable) {
                    return@withContext Result.failure(e)
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

                    if (item !is Song) {
                        item.thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(header_renderer.getThumbnails())
                    }

                    header_renderer.subtitle?.runs?.also { subtitle ->
                        if (item is MediaItem.DataWithArtist) {
                            val artist_run: TextRun? = subtitle.firstOrNull {
                                it.navigationEndpoint?.browseEndpoint?.let { endpoint ->
                                    endpoint.browseId != null && endpoint.getMediaItemType() == MediaItemType.ARTIST
                                } ?: false
                            }

                            if (artist_run != null) {
                                item.artist = ArtistData(artist_run.navigationEndpoint!!.browseEndpoint!!.browseId!!).apply {
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

                    if (item is ArtistData) {
                        if (header_renderer.subscriptionButton != null) {
                            val subscribe_button = header_renderer.subscriptionButton.subscribeButtonRenderer
                            item.subscribe_channel_id = subscribe_button.channelId
                            item.subscriber_count = parseYoutubeSubscribersString(subscribe_button.subscriberCountText.first_text, hl)
                            item.subscribed = subscribe_button.subscribed
                        }
                        if (header_renderer.playButton?.buttonRenderer?.icon?.iconType == "MUSIC_SHUFFLE") {
                            item.shuffle_playlist_id = header_renderer.playButton.buttonRenderer.navigationEndpoint.watchEndpoint?.playlistId
                        }
                    }
                }

                if (item is RemotePlaylistData) {
                    val menu_buttons: List<Header.TopLevelButton>? =
                        parsed.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons

                    if (menu_buttons?.any { it.buttonRenderer?.icon?.iconType == "EDIT" } == true) {
                        item.owner = api.user_auth_state?.own_channel
                        item.playlist_type = PlaylistType.PLAYLIST
                    }
                }

                val section_list_renderer: YoutubeiBrowseResponse.SectionListRenderer? = with (parsed.contents!!) {
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

                    val items: List<Pair<MediaItemData, String?>> = row.value.getMediaItemsAndSetIds(hl)
                    val items_mapped: List<MediaItemData> = items.map {
                        val list_item: MediaItemData = it.first
                        if (item is Artist && list_item is SongData && list_item.song_type == SongType.PODCAST) {
                            list_item.artist = item
                        }
                        list_item
                    }

                    val continuation_token: String? =
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
                        if (item.isOwnChannel(api)) YoutubeLocalisedString.Type.OWN_CHANNEL.createFromKey(it, api.context)
                        else YoutubeLocalisedString.mediaItemPage(it, item.getType(), api.context)
                    }

                    val view_more = row.value.getNavigationEndpoint()?.getViewMore(item)
                    if (view_more is MediaItemViewMore) {
                        val view_more_item = view_more.media_item as MediaItemData
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
