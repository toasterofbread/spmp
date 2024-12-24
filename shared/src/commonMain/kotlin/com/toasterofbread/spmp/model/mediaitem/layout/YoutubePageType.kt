package com.toasterofbread.spmp.model.mediaitem.layout

import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.toMediaItemRef
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.ListPageBrowseIdYoutubePage
import dev.toastbits.ytmkt.model.external.MediaItemYoutubePage
import dev.toastbits.ytmkt.model.external.PlainYoutubePage
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.uistrings.UiString
import kotlinx.coroutines.runBlocking

enum class YoutubePageType {
    MediaItem, ListPage, Plain;

    fun getPage(data: String): YoutubePage {
        try {
            when (this) {
                MediaItem -> {
                    val split: List<String?> = data.split(VIEW_MORE_SPLIT_CHAR, limit = 3).map { it.ifBlank { null } }
                    return MediaItemYoutubePage(
                        browse_media_item = getMediaItemFromUid(split[0]!!),
                        browse_params = split.getOrNull(1),
                        media_item = split.getOrNull(2)?.let { getMediaItemFromUid(it) }
                    )
                }
                ListPage -> {
                    val split: List<String> = data.split(VIEW_MORE_SPLIT_CHAR, limit = 3)
                    return ListPageBrowseIdYoutubePage(
                        media_item = getMediaItemFromUid(split[0], MediaItemType.ARTIST),
                        list_page_browse_id = split[1],
                        browse_params = split[2]
                    )
                }
                Plain -> {
                    return PlainYoutubePage(browse_id = data)
                }
            }
        }
        catch (e: Throwable) {
            throw RuntimeException("Parsing ViewMore($this) data failed '$data'", e)
        }
    }

    companion object {
        private const val VIEW_MORE_SPLIT_CHAR = '|'

        fun fromPage(view_more: YoutubePage): Pair<Long, String>? =
            when (view_more) {
                is MediaItemYoutubePage ->
                    Pair(
                        MediaItem.ordinal.toLong(),
                        view_more.browse_media_item.getUid() + VIEW_MORE_SPLIT_CHAR + (view_more.browse_params ?: "") + VIEW_MORE_SPLIT_CHAR + (view_more.media_item?.getUid() ?: "")
                    )
                is ListPageBrowseIdYoutubePage ->
                    Pair(
                        ListPage.ordinal.toLong(),
                        view_more.media_item.getUid() + VIEW_MORE_SPLIT_CHAR + view_more.list_page_browse_id + VIEW_MORE_SPLIT_CHAR + view_more.browse_params
                    )
                is PlainYoutubePage ->
                    Pair(
                        Plain.ordinal.toLong(),
                        view_more.browse_id
                    )
                is LambdaYoutubePage -> null
                else -> throw NotImplementedError(view_more::class.toString())
            }
    }
}

data class LambdaYoutubePage(
    val action: (player: PlayerState, title: UiString?) -> Unit
): YoutubePage {
    override fun getBrowseParamsData(): YoutubePage.BrowseParamsData =
        throw IllegalStateException()
}

suspend fun YoutubePage.open(player: PlayerState, title: UiString?) {
    when (this) {
        is LambdaYoutubePage ->  action(player, title)
        is MediaItemYoutubePage ->
            player.openMediaItem(
                (media_item ?: browse_media_item).toMediaItemRef(),
                true,
                browse_params = browse_params?.let {
                    YoutubePage.BrowseParamsData(browse_media_item.id, it)
                }
            )
        is ListPageBrowseIdYoutubePage ->
            player.openMediaItem(
                media_item.toMediaItemRef(),
                browse_params = getBrowseParamsData()
            )
        is PlainYoutubePage ->
            player.openViewMorePage(browse_id, runBlocking { title?.getString(player.context.getUiLanguage().toTag()) })
    }
}
