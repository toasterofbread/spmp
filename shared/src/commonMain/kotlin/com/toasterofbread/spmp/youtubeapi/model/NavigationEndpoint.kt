package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint? = null,
    val browseEndpoint: BrowseEndpoint? = null,
    val searchEndpoint: SearchEndpoint? = null,
    val watchPlaylistEndpoint: WatchPlaylistEndpoint? = null,
    val channelCreationFormEndpoint: ChannelCreationFormEndpoint? = null,
    val urlEndpoint: String? = null
) {
    fun getMediaItem(): MediaItemData? {
        if (watchEndpoint != null) {
            if (watchEndpoint.videoId != null) {
                return SongData(watchEndpoint.videoId)
            }
            else if (watchEndpoint.playlistId != null) {
                return RemotePlaylistData(watchEndpoint.playlistId)
            }
        }
        if (browseEndpoint != null) {
            browseEndpoint.getMediaItem()?.also { return it }
        }
        if (watchPlaylistEndpoint != null) {
            return RemotePlaylistData(watchPlaylistEndpoint.playlistId)
        }
        return null
    }

    fun getViewMore(item: MediaItem): MediaItemLayout.ViewMore? {
        if (browseEndpoint != null) {
            browseEndpoint.getViewMore(item).also { return it }
        }
        return getMediaItem()?.let { MediaItemLayout.MediaItemViewMore(it, null) }
    }
}

data class ChannelCreationFormEndpoint(val channelCreationToken: String)
