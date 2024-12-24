package com.toasterofbread.spmp.model.mediaitem

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.db.rememberHiddenItems
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist

interface MediaItemHolder {
    // If item is null, consider it deleted
    val item: MediaItem?
}

private suspend fun List<MediaItemHolder>.filterItems(
    context: AppContext,
    apply_filter: Boolean,
    hidden_items: List<MediaItem>,
    is_song_feed: Boolean = false
): List<MediaItem> {
    val hide_radios: Boolean = is_song_feed && !context.settings.Feed.SHOW_RADIOS.get()

    return mapNotNull {
        val item: MediaItem? = it.item
        if (item == null || (apply_filter && isMediaItemHidden(item, context, hidden_items))) {
            return@mapNotNull null
        }

        if (hide_radios && item is RemotePlaylist && item.TypeOfPlaylist.get(context.database) == PlaylistType.RADIO) {
            return@mapNotNull null
        }

        return@mapNotNull item
    }
}

@Composable
fun List<MediaItemHolder>.rememberFilteredItems(
    apply_filter: Boolean = true,
    is_song_feed: Boolean = false
): State<List<MediaItem>> {
    val player: PlayerState = LocalPlayerState.current
    val hidden_items: List<MediaItem> = rememberHiddenItems()
    val items_state: MutableState<List<MediaItem>> = remember { mutableStateOf(emptyList()) }

    LaunchedEffect(this, apply_filter, hidden_items) {
        items_state.value = filterItems(player.context, apply_filter, hidden_items, is_song_feed)
    }

    return items_state
}

@Composable
fun List<YtmMediaItem>.rememberFilteredYtmItems(apply_filter: Boolean = true) =
    remember(this) { map { it.toMediaItemRef() } }.rememberFilteredItems(apply_filter)