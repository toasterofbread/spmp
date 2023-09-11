package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMore
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData

data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint?,
    val browseEndpoint: BrowseEndpoint?,
    val searchEndpoint: SearchEndpoint?,
    val watchPlaylistEndpoint: WatchPlaylistEndpoint?,
    val channelCreationFormEndpoint: ChannelCreationFormEndpoint?
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

    fun getViewMore(item: MediaItem): ViewMore? {
        if (browseEndpoint != null) {
            browseEndpoint.getViewMore(item).also { return it }
        }
        return getMediaItem()?.let { MediaItemViewMore(it, null) }
    }
}

data class ChannelCreationFormEndpoint(val channelCreationToken: String)
