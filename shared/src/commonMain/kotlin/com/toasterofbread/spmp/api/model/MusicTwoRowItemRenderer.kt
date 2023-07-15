package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.NavigationEndpoint
import com.toasterofbread.spmp.api.TextRuns
import com.toasterofbread.spmp.api.ThumbnailRenderer
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType

class MusicTwoRowItemRenderer(
    val navigationEndpoint: NavigationEndpoint,
    val title: TextRuns,
    val subtitle: TextRuns? = null,
    val thumbnailRenderer: ThumbnailRenderer,
    val menu: YoutubeiNextResponse.Menu? = null
) {
    fun getArtist(host_item: MediaItem): Artist? {
        for (run in subtitle?.runs ?: emptyList()) {
            val browse_endpoint = run.navigationEndpoint?.browseEndpoint

            val endpoint_type = browse_endpoint?.getMediaItemType()
            if (endpoint_type == MediaItemType.ARTIST) {
                return Artist.fromId(browse_endpoint.browseId).editArtistData { supplyTitle(run.text) }
            }
        }

        if (host_item is Song) {
            val index = if (host_item.song_type == SongType.VIDEO) 0 else 1
            subtitle?.runs?.getOrNull(index)?.also {
                return Artist.createForItem(host_item).editArtistData { supplyTitle(it.text) }
            }
        }

        return null
    }
    
    fun toMediaItem(hl: String): MediaItem? {
        // Video
        if (navigationEndpoint.watchEndpoint?.videoId != null) {
            val first_thumbnail = thumbnailRenderer.musicThumbnailRenderer.thumbnail.thumbnails.first()
            return Song.fromId(navigationEndpoint.watchEndpoint.videoId).editSongData {
                // Is this the best way of checking?
                supplySongType(if (first_thumbnail.height == first_thumbnail.width) SongType.SONG else SongType.VIDEO)
                supplyTitle(title.first_text)
                supplyArtist(getArtist(data_item))
                supplyThumbnailProvider(thumbnailRenderer.toThumbnailProvider())
            }
        }

        val item: MediaItem

        if (navigationEndpoint.watchPlaylistEndpoint != null) {
            if (!Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                return null
            }

            item = AccountPlaylist.fromId(navigationEndpoint.watchPlaylistEndpoint.playlistId)
                .editPlaylistData {
                    supplyPlaylistType(PlaylistType.RADIO, true)
                    supplyTitle(title.first_text)
                    supplyThumbnailProvider(thumbnailRenderer.toThumbnailProvider())
                }
        }
        else {
            // Playlist or artist
            val browse_id = navigationEndpoint.browseEndpoint!!.browseId
            val page_type = navigationEndpoint.browseEndpoint.getPageType()!!

            item = when (page_type) {
                "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_AUDIOBOOK", "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE" -> {
                    if (AccountPlaylist.formatId(browse_id).startsWith("RDAT") && !Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                        return null
                    }

                    AccountPlaylist.fromId(browse_id)
                        .editPlaylistData {
                            supplyPlaylistType(
                                PlaylistType.fromTypeString(page_type),
                                true
                            )
                            supplyArtist(getArtist(data_item))
                        }
                        .apply {
                            is_editable = menu?.menuRenderer?.items
                                ?.any { it.menuNavigationItemRenderer?.icon?.iconType == "DELETE" } == true
                        }
                }
                "MUSIC_PAGE_TYPE_ARTIST" -> Artist.fromId(browse_id)
                else -> throw NotImplementedError("$page_type ($browse_id)")
            }

            item.editData {
                supplyTitle(title.first_text)
                supplyThumbnailProvider(thumbnailRenderer.toThumbnailProvider())
            }
        }

        return item
    }
}
