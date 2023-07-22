package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.BrowseEndpoint
import com.toasterofbread.spmp.api.SearchEndpoint
import com.toasterofbread.spmp.api.WatchEndpoint
import com.toasterofbread.spmp.api.WatchPlaylistEndpoint
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint? = null,
    val browseEndpoint: BrowseEndpoint? = null,
    val searchEndpoint: SearchEndpoint? = null,
    val watchPlaylistEndpoint: WatchPlaylistEndpoint? = null
) {
    fun getMediaItem(): MediaItemData? {
        if (watchEndpoint != null) {
            if (watchEndpoint.videoId != null) {
                return SongData(watchEndpoint.videoId)
            }
            else if (watchEndpoint.playlistId != null) {
                return PlaylistData(watchEndpoint.playlistId)
            }
        }
        if (browseEndpoint != null) {
            browseEndpoint.getMediaItem()?.also { return it }
        }
        if (watchPlaylistEndpoint != null) {
            return PlaylistData(watchPlaylistEndpoint.playlistId)
        }
        return null
    }

    fun getViewMore(): MediaItemLayout.ViewMore? {
        if (browseEndpoint != null) {
            browseEndpoint.getViewMore().also { return it }
        }
        return getMediaItem()?.let { MediaItemLayout.MediaItemViewMore(it) }
    }
}
