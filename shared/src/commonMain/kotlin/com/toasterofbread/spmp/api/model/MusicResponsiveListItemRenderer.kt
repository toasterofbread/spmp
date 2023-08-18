package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString

data class MusicResponsiveListItemRenderer(
    val playlistItemData: RendererPlaylistItemData? = null,
    val flexColumns: List<FlexColumn>? = null,
    val fixedColumns: List<FixedColumn>? = null,
    val thumbnail: ThumbnailRenderer? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
    val menu: YoutubeiNextResponse.Menu? = null
) { 
    fun toMediaItemAndPlaylistSetVideoId(hl: String): Pair<MediaItemData, String?>? {
        var video_id: String? = playlistItemData?.videoId ?: navigationEndpoint?.watchEndpoint?.videoId
        var video_is_main: Boolean = true

        var title: String? = null
        var artist: ArtistData? = null
        var playlist: PlaylistData? = null
        var duration: Long? = null

        if (video_id == null) {
            val page_type = navigationEndpoint?.browseEndpoint?.getPageType()
            when (
                page_type?.let { type ->
                    MediaItemType.fromBrowseEndpointType(type)
                }
            ) {
                MediaItemType.PLAYLIST_ACC -> {
                    video_is_main = false
                    playlist = PlaylistData(navigationEndpoint!!.browseEndpoint!!.browseId).apply {
                        playlist_type = PlaylistType.fromBrowseEndpointType(page_type)
                    }
                }
                MediaItemType.ARTIST -> {
                    video_is_main = false
                    artist = ArtistData(navigationEndpoint!!.browseEndpoint!!.browseId)
                }
                else -> {}
            }
        }

        if (flexColumns != null) {
            for (column in flexColumns.withIndex()) {
                val text = column.value.musicResponsiveListItemFlexColumnRenderer.text
                if (text.runs == null) {
                    continue
                }

                if (column.index == 0) {
                    title = text.first_text
                }

                for (run in text.runs!!) {
                    if (run.navigationEndpoint == null) {
                        continue
                    }

                    if (run.navigationEndpoint.watchEndpoint != null) {
                        if (video_id == null) {
                            video_id = run.navigationEndpoint.watchEndpoint.videoId!!
                        }
                        continue
                    }

                    val browse_endpoint = run.navigationEndpoint.browseEndpoint
                    if (artist == null && browse_endpoint?.getMediaItemType() == MediaItemType.ARTIST) {
                        artist = ArtistData(browse_endpoint.browseId)
                        artist.title = run.text
                    }
                }
            }
        }

        if (fixedColumns != null) {
            for (column in fixedColumns) {
                val text = column.musicResponsiveListItemFixedColumnRenderer.text.first_text
                val parsed = parseYoutubeDurationString(text, hl)
                if (parsed != null) {
                    duration = parsed
                    break
                }
            }
        }

        val item_data: MediaItemData
        if (video_id != null) {
            item_data = SongData(video_id).also { data ->
                data.duration = duration
                thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.also {
                    data.song_type = if (it.height == it.width) SongType.SONG else SongType.VIDEO
                }
            }
        }
        else if (video_is_main) {
            return null
        }
        else {
            item_data = (playlist?.apply { total_duration = duration }) ?: artist ?: return null
        }

        // Handle songs with no artist (or 'Various artists')
        if (artist == null) {
            if (flexColumns != null && flexColumns.size > 1) {
                val text = flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text
                if (text.runs != null) {
                    artist = ArtistData.createForItem(item_data)
                    artist.title = text.first_text
                }
            }

            if (artist == null && menu != null) {
                for (item in menu.menuRenderer.items) {
                    val browse_endpoint = (item.menuNavigationItemRenderer ?: continue).navigationEndpoint.browseEndpoint ?: continue
                    if (browse_endpoint.getMediaItemType() == MediaItemType.ARTIST) {
                        artist = ArtistData(browse_endpoint.browseId)
                        break
                    }
                }
            }
        }

        item_data.title = title
        item_data.thumbnail_provider = thumbnail?.toThumbnailProvider()

        if (item_data is MediaItem.DataWithArtist) {
            item_data.artist = artist
        }

        return Pair(item_data, playlistItemData?.playlistSetVideoId)
    }
}

data class RendererPlaylistItemData(val videoId: String, val playlistSetVideoId: String? = null)
