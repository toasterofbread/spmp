package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse

class MusicTwoRowItemRenderer(
    val navigationEndpoint: NavigationEndpoint,
    val title: TextRuns,
    val subtitle: TextRuns? = null,
    val thumbnailRenderer: ThumbnailRenderer,
    val menu: YoutubeiNextResponse.Menu? = null
) {
    private fun getArtist(host_item: MediaItemData): Artist? {
        for (run in subtitle?.runs ?: emptyList()) {
            val browse_endpoint = run.navigationEndpoint?.browseEndpoint

            val endpoint_type = browse_endpoint?.getMediaItemType()
            if (endpoint_type == MediaItemType.ARTIST) {
                return ArtistData(browse_endpoint.browseId).apply {
                    title = run.text
                }
            }
        }

        if (host_item is SongData) {
            val index = if (host_item.song_type == SongType.VIDEO) 0 else 1
            subtitle?.runs?.getOrNull(index)?.also {
                return ArtistData.createForItem(host_item).apply {
                    title = it.text
                }
            }
        }

        return null
    }
    
    fun toMediaItem(hl: String): MediaItemData? {
        // Video
        if (navigationEndpoint.watchEndpoint?.videoId != null) {
            val first_thumbnail = thumbnailRenderer.musicThumbnailRenderer.thumbnail.thumbnails.first()
            return SongData(navigationEndpoint.watchEndpoint.videoId).also { data ->
                data.song_type = if (first_thumbnail.height == first_thumbnail.width) SongType.SONG else SongType.VIDEO
                data.title = this@MusicTwoRowItemRenderer.title.first_text
                data.thumbnail_provider = thumbnailRenderer.toThumbnailProvider()
                data.artist = getArtist(data)
            }
        }

        val item: MediaItemData

        if (navigationEndpoint.watchPlaylistEndpoint != null) {
            if (!Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                return null
            }

            item = PlaylistData(navigationEndpoint.watchPlaylistEndpoint.playlistId).also { data ->
                data.playlist_type = PlaylistType.RADIO
                data.title = title.first_text
                data.thumbnail_provider = thumbnailRenderer.toThumbnailProvider()
            }
        }
        else {
            // Playlist or artist
            val browse_id = navigationEndpoint.browseEndpoint!!.browseId
            val page_type = navigationEndpoint.browseEndpoint.getPageType()!!

            item = when (MediaItemType.fromBrowseEndpointType(page_type)) {
                MediaItemType.PLAYLIST_ACC -> {
                    if (Playlist.formatYoutubeId(browse_id).startsWith("RDAT") && !Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                        return null
                    }

                    PlaylistData(browse_id).also { data ->
                        data.playlist_type = PlaylistType.fromBrowseEndpointType(page_type)
                        data.artist = getArtist(data)
//                        is_editable = menu?.menuRenderer?.items
//                            ?.any { it.menuNavigationItemRenderer?.icon?.iconType == "DELETE" } == true
                    }
                }
                MediaItemType.ARTIST -> ArtistData(browse_id)
                else -> throw NotImplementedError("$page_type ($browse_id)")
            }

            item.title = title.first_text
            item.thumbnail_provider = thumbnailRenderer.toThumbnailProvider()
        }

        return item
    }
}
