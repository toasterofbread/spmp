package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse

data class MusicResponsiveListItemRenderer(
    val playlistItemData: RendererPlaylistItemData?,
    val flexColumns: List<FlexColumn>?,
    val fixedColumns: List<FixedColumn>?,
    val thumbnail: ThumbnailRenderer?,
    val navigationEndpoint: NavigationEndpoint?,
    val menu: YoutubeiNextResponse.Menu?,
    val index: TextRuns?,
    val badges: List<Badge>?
) {
    data class Badge(val musicInlineBadgeRenderer: MusicInlineBadgeRenderer?) {
        fun isExplicit(): Boolean = musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
    }
    data class MusicInlineBadgeRenderer(val icon: YoutubeiNextResponse.MenuIcon?)

    fun toMediaItemAndPlaylistSetVideoId(hl: String): Pair<MediaItemData, String?>? {
        var video_id: String? = playlistItemData?.videoId ?: navigationEndpoint?.watchEndpoint?.videoId
        val browse_id: String? = navigationEndpoint?.browseEndpoint?.browseId
        var video_is_main: Boolean = true

        var title: String? = null
        var artist: ArtistData? = null
        var playlist: RemotePlaylistData? = null
        var duration: Long? = null
        var album: RemotePlaylistData? = null

        if (video_id == null && browse_id != null) {
            val page_type: String? = navigationEndpoint!!.browseEndpoint!!.getPageType()
            when (
                page_type?.let { type ->
                    MediaItemType.fromBrowseEndpointType(type)
                }
            ) {
                MediaItemType.PLAYLIST_REM -> {
                    video_is_main = false
                    playlist = RemotePlaylistData(browse_id).apply {
                        playlist_type = PlaylistType.fromBrowseEndpointType(page_type)
                    }
                }
                MediaItemType.ARTIST -> {
                    video_is_main = false
                    artist = ArtistData(browse_id)
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

                    val browse_endpoint: BrowseEndpoint = run.navigationEndpoint.browseEndpoint ?: continue
                    if (artist == null && browse_endpoint.browseId != null && browse_endpoint.getMediaItemType() == MediaItemType.ARTIST) {
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
                data.explicit = badges?.any { it.isExplicit() } == true
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
                    artist = ArtistData(Artist.getForItemId(item_data))
                    artist.title = text.first_text
                }
            }
        }

        for (item in menu?.menuRenderer?.items ?: emptyList()) {
            val browse_endpoint: BrowseEndpoint = (item.menuNavigationItemRenderer ?: continue).navigationEndpoint.browseEndpoint ?: continue
            if (browse_endpoint.browseId == null) {
                continue
            }

            when (browse_endpoint.getMediaItemType()) {
                MediaItemType.ARTIST -> {
                    if (artist == null) {
                        artist = ArtistData(browse_endpoint.browseId)
                    }
                }
                MediaItemType.PLAYLIST_REM -> {
                    if (album == null) {
                        album = RemotePlaylistData(browse_endpoint.browseId)
                    }
                }
                else -> {}
            }
        }

        item_data.title = title
        item_data.thumbnail_provider = thumbnail?.toThumbnailProvider()

        if (item_data is MediaItem.DataWithArtist) {
            item_data.artist = artist
        }
        if (item_data is SongData) {
            item_data.album = album
        }

        return Pair(item_data, playlistItemData?.playlistSetVideoId)
    }
}

data class RendererPlaylistItemData(val videoId: String, val playlistSetVideoId: String?)
