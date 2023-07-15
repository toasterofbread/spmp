package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.FixedColumn
import com.toasterofbread.spmp.api.FlexColumn
import com.toasterofbread.spmp.api.NavigationEndpoint
import com.toasterofbread.spmp.api.ThumbnailRenderer
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.data.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString

class MusicResponsiveListItemRenderer(
    val playlistItemData: RendererPlaylistItemData? = null,
    val flexColumns: List<FlexColumn>? = null,
    val fixedColumns: List<FixedColumn>? = null,
    val thumbnail: ThumbnailRenderer? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
    val menu: YoutubeiNextResponse.Menu? = null
) { 
    fun toMediaItemAndPlaylistSetVideoId(hl: String): Pair<MediaItem, String?>? {
        var video_id: String? = playlistItemData?.videoId ?: navigationEndpoint?.watchEndpoint?.videoId
        var video_is_main: Boolean = true

        var title: String? = null
        var artist: Artist? = null
        var playlist: AccountPlaylist? = null
        var duration: Long? = null

        if (video_id == null) {
            val page_type = navigationEndpoint?.browseEndpoint?.getPageType()
            when (page_type) {
                "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST" -> {
                    video_is_main = false
                    playlist = AccountPlaylist.fromId(navigationEndpoint!!.browseEndpoint!!.browseId)
                        .editPlaylistData {
                            supplyPlaylistType(PlaylistType.fromTypeString(page_type), true)
                        }
                }
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                    video_is_main = false
                    artist = Artist.fromId(navigationEndpoint!!.browseEndpoint!!.browseId)
                }
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
                    when (browse_endpoint?.getPageType()) {
                        "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                            if (artist == null) {
                                artist = Artist.fromId(browse_endpoint.browseId).editArtistData { supplyTitle(run.text) }
                            }
                        }
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
            item_data = Song.fromId(video_id).editSongDataManual {
                supplyDuration(duration, true)
                thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.also {
                    supplySongType(if (it.height == it.width) SongType.SONG else SongType.VIDEO)
                }
            }
        }
        else if (video_is_main) {
            return null
        }
        else {
            item_data = (playlist?.data?.apply { supplyTotalDuration(duration, true) }) ?: artist?.data ?: return null
        }

        // Handle songs with no artist (or 'Various artists')
        if (artist == null) {
            if (flexColumns != null && flexColumns.size > 1) {
                val text = flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text
                if (text.runs != null) {
                    artist = Artist.createForItem(item_data.data_item).editArtistData { supplyTitle(text.first_text) }
                }
            }

            if (artist == null && menu != null) {
                for (item in menu.menuRenderer.items) {
                    val browse_endpoint = (item.menuNavigationItemRenderer ?: continue).navigationEndpoint.browseEndpoint ?: continue
                    if (browse_endpoint.getMediaItemType() == MediaItemType.ARTIST) {
                        artist = Artist.fromId(browse_endpoint.browseId)
                        break
                    }
                }
            }
        }

        with(item_data) {
            supplyTitle(title)
            supplyArtist(artist)
            supplyThumbnailProvider(thumbnail?.toThumbnailProvider())
            save()
        }

        return Pair(item_data.data_item, playlistItemData?.playlistSetVideoId)
    }
}

class RendererPlaylistItemData(val videoId: String, val playlistSetVideoId: String? = null)
