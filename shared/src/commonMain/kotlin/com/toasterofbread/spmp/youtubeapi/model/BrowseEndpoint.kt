package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

data class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
data class BrowseEndpointContextMusicConfig(val pageType: String)
data class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
data class BrowseEndpoint(
    val browseId: String,
    val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null,
    val params: String? = null
) {
    fun getPageType(): String? =
        browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
    fun getMediaItemType(): MediaItemType? =
        getPageType()?.let { MediaItemType.fromBrowseEndpointType(it) }

    fun getMediaItem(): MediaItemData? =
        getPageType()?.let { page_type ->
            MediaItemData.fromBrowseEndpointType(page_type, browseId)
        }

    fun getViewMore(base_item: MediaItem): MediaItemLayout.ViewMore {
        val item = getMediaItem()
        if (item != null) {
            return MediaItemLayout.MediaItemViewMore(item, params)
        }
        else if (params != null) {
            return MediaItemLayout.ListPageBrowseIdViewMore(
                base_item.id,
                list_page_browse_id = browseId,
                browse_params = params
            )
        }
        else {
            return MediaItemLayout.PlainViewMore(browseId)
        }
    }
}
data class SearchEndpoint(val query: String, val params: String? = null)
data class WatchPlaylistEndpoint(val playlistId: String, val params: String)
