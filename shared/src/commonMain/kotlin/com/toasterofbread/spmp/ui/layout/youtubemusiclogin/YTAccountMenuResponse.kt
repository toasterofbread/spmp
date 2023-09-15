package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.youtubeapi.model.BrowseEndpoint
import com.toasterofbread.spmp.youtubeapi.model.MusicThumbnailRenderer
import com.toasterofbread.spmp.youtubeapi.model.NavigationEndpoint
import com.toasterofbread.spmp.youtubeapi.model.TextRuns

data class CreateChannelResponse(val navigationEndpoint: ChannelNavigationEndpoint) {
    data class ChannelNavigationEndpoint(val browseEndpoint: BrowseEndpoint)
}

data class YTAccountMenuResponse(val actions: List<Action>) {
    data class Action(val openPopupAction: OpenPopupAction)
    data class OpenPopupAction(val popup: Popup)
    data class Popup(val multiPageMenuRenderer: MultiPageMenuRenderer)
    data class MultiPageMenuRenderer(val sections: List<Section>, val header: Header? = null)

    data class Section(val multiPageMenuSectionRenderer: MultiPageMenuSectionRenderer)
    data class MultiPageMenuSectionRenderer(val items: List<Item>)
    data class Item(val compactLinkRenderer: CompactLinkRenderer)
    data class CompactLinkRenderer(val navigationEndpoint: NavigationEndpoint? = null)

    data class Header(val activeAccountHeaderRenderer: ActiveAccountHeaderRenderer)
    data class ActiveAccountHeaderRenderer(val accountName: TextRuns, val accountPhoto: MusicThumbnailRenderer.Thumbnail)

    fun getAritst(): Artist? {
        val account = actions.first().openPopupAction.popup.multiPageMenuRenderer.header!!.activeAccountHeaderRenderer
        return ArtistData(getChannelId() ?: return null).apply {
            title = account.accountName.first_text
            thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(account.accountPhoto.thumbnails)
        }
    }

    private fun getSections() = actions.first().openPopupAction.popup.multiPageMenuRenderer.sections

    private fun getChannelId(): String? {
        for (section in getSections()) {
            for (item in section.multiPageMenuSectionRenderer.items) {
                val browse_endpoint = item.compactLinkRenderer.navigationEndpoint?.browseEndpoint
                if (browse_endpoint?.getMediaItemType() == MediaItemType.ARTIST) {
                    return browse_endpoint.browseId
                }
            }
        }
        return null
    }

    fun getChannelCreationToken(): String? {
        for (section in getSections()) {
            for (item in section.multiPageMenuSectionRenderer.items) {
                val endpoint = item.compactLinkRenderer.navigationEndpoint?.channelCreationFormEndpoint
                if (endpoint != null) {
                    return endpoint.channelCreationToken
                }
            }
        }
        return null
    }
}