package com.toasterofbread.spmp.service.playercontroller

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import LocalAppState
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
    fun onMediaItemClicked(item: MediaItem, state: SpMp.State, multiselect_key: Int? = null) {
        state.context.coroutine_scope.launch {
            if (onClickOverride != null) {
                onClickOverride.invoke(item, multiselect_key)
                return@launch
            }

            if (item is Song) {
                state.session.playMediaItem(item)
                state.ui.onPlayActionOccurred()
            } else if (
                item is Playlist
                && state.settings.behaviour.TREAT_SINGLES_AS_SONG.get()
                && state.settings.behaviour.TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE.get()
            ) {
                state.context.coroutine_scope.launch {
                    item.loadData(state.context).onSuccess { data ->
                        val single = data.items?.singleOrNull()
                        if (single != null) {
                            onMediaItemClicked(single, state)
                        } else {
                            state.ui.openMediaItem(item)
                        }
                    }
                }
            } else {
                state.ui.openMediaItem(item)
            }
        }
    }

    fun onMediaItemAltClicked(item: MediaItem, queue_index: Int, state: SpMp.State) {
        onMediaItemAltClicked(item, state, LongPressMenuData(item, multiselect_key = queue_index))
    }
    fun onMediaItemAltClicked(item: MediaItem, state: SpMp.State, long_press_data: LongPressMenuData? = null) {
        if (onAltClickOverride != null) {
            onAltClickOverride.invoke(item, long_press_data)
            return
        }

        state.ui.showLongPressMenu(long_press_data ?: item.getLongPressMenuData())
    }
}
