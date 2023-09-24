package com.toasterofbread.spmp.model.mediaitem.layout

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException

sealed interface ViewMore {
    fun execute(player: PlayerState, title: LocalisedString?)
}

enum class ViewMoreType {
    MediaItem, ListPage, Plain;

    fun getViewMore(data: String): ViewMore {
        try {
            when (this) {
                MediaItem -> {
                    val split = data.split(VIEW_MORE_SPLIT_CHAR, limit = 3).map { it.ifBlank { null } }
                    return MediaItemViewMore(
                        browse_media_item = getMediaItemFromUid(split[0]!!),
                        browse_params = split.getOrNull(1),
                        media_item = split.getOrNull(2)?.let { getMediaItemFromUid(it) }
                    )
                }
                ListPage -> {
                    val split = data.split(VIEW_MORE_SPLIT_CHAR, limit = 3)
                    return ListPageBrowseIdViewMore(
                        item_id = split[0],
                        list_page_browse_id = split[1],
                        browse_params = split[2]
                    )
                }
                Plain -> {
                    return PlainViewMore(browse_id = data)
                }
            }
        }
        catch (e: Throwable) {
            throw RuntimeException("Parsing ViewMore($this) data failed '$data'", e)
        }
    }

    companion object {
        private const val VIEW_MORE_SPLIT_CHAR = '|'
        fun fromViewMore(view_more: ViewMore): Pair<Long, String>? =
            when (view_more) {
                is MediaItemViewMore ->
                    Pair(
                        MediaItem.ordinal.toLong(),
                        view_more.browse_media_item.getUid() + VIEW_MORE_SPLIT_CHAR + (view_more.browse_params ?: "") + VIEW_MORE_SPLIT_CHAR + (view_more.media_item?.getUid() ?: "")
                    )
                is ListPageBrowseIdViewMore ->
                    Pair(
                        ListPage.ordinal.toLong(),
                        view_more.item_id + VIEW_MORE_SPLIT_CHAR + view_more.list_page_browse_id + VIEW_MORE_SPLIT_CHAR + view_more.browse_params
                    )
                is PlainViewMore ->
                    Pair(
                        Plain.ordinal.toLong(),
                        view_more.browse_id
                    )
                is LambdaViewMore -> null
            }
    }
}

data class LambdaViewMore(
    val action: (player: PlayerState, title: LocalisedString?) -> Unit
): ViewMore {
    override fun execute(player: PlayerState, title: LocalisedString?) = action(player, title)
}

data class BrowseParamsData(
    val browse_id: String,
    val browse_params: String,
    val title: String
)

data class MediaItemViewMore(
    val browse_media_item: MediaItem,
    val browse_params: String?,
    val media_item: MediaItem? = null
): ViewMore {
    override fun execute(player: PlayerState, title: LocalisedString?) {
        player.openMediaItem(
            media_item ?: browse_media_item,
            true,
            browse_params = browse_params?.let { params ->
                BrowseParamsData(browse_media_item.id, params, title?.getString() ?: "")
            }
        )
    }
}

data class ListPageBrowseIdViewMore(
    val item_id: String,
    val list_page_browse_id: String,
    val browse_params: String
): ViewMore {
    override fun execute(player: PlayerState, title: LocalisedString?) {
        player.openMediaItem(
            ArtistRef(item_id),
            browse_params = BrowseParamsData(list_page_browse_id, browse_params, title?.getString() ?: "")
        )
    }
}

data class PlainViewMore(
    val browse_id: String
): ViewMore {
    override fun execute(player: PlayerState, title: LocalisedString?) {
        player.openViewMorePage(browse_id, title?.getString())
    }
}
