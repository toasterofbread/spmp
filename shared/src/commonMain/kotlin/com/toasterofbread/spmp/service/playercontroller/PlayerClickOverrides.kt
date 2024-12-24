package com.toasterofbread.spmp.service.playercontroller

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import kotlinx.coroutines.launch

typealias PlayerOnClickedAction = (item: MediaItem, multiselect_key: Int?) -> Unit
typealias PlayerOnLongClickedAction = (item: MediaItem, long_press_data: LongPressMenuData?) -> Unit

val LocalPlayerClickOverrides: ProvidableCompositionLocal<PlayerClickOverrides> = staticCompositionLocalOf{ PlayerClickOverrides() }

data class PlayerClickOverrides(
    val onClickOverride: PlayerOnClickedAction? = null,
    val onAltClickOverride: PlayerOnLongClickedAction? = null
) {
    fun onMediaItemClicked(item: MediaItem, player: PlayerState, multiselect_key: Int? = null) {
        player.coroutine_scope.launch {
            if (onClickOverride != null) {
                onClickOverride.invoke(item, multiselect_key)
                return@launch
            }

            if (item is Song) {
                player.playMediaItem(item)
                player.onPlayActionOccurred()
            } else if (
                item is Playlist
                && player.settings.Behaviour.TREAT_SINGLES_AS_SONG.get()
                && player.settings.Behaviour.TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE.get()
            ) {
                player.coroutine_scope.launch {
                    item.loadData(player.context).onSuccess { data ->
                        val single = data.items?.singleOrNull()
                        if (single != null) {
                            onMediaItemClicked(single, player)
                        } else {
                            player.openMediaItem(item)
                        }
                    }
                }
            } else {
                player.openMediaItem(item)
            }
        }
    }

    fun onMediaItemAltClicked(item: MediaItem, queue_index: Int, player: PlayerState) {
        onMediaItemAltClicked(item, player, LongPressMenuData(item, multiselect_key = queue_index, queue_index = queue_index))
    }
    fun onMediaItemAltClicked(item: MediaItem, player: PlayerState, long_press_data: LongPressMenuData? = null) {
        if (onAltClickOverride != null) {
            onAltClickOverride.invoke(item, long_press_data)
            return
        }

        player.showLongPressMenu(long_press_data ?: item.getLongPressMenuData())
    }
}
