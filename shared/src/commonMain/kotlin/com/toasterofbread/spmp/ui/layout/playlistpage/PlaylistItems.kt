package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import LocalAppState
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import dev.toastbits.composekit.utils.common.getValue
import dev.toastbits.ytmkt.uistrings.durationToString
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import LocalPlayerState
import com.toasterofbread.spmp.platform.observeUiLanguage

internal fun PlaylistAppPage.PlaylistItems(
    playlist: Playlist,
    list_scope: LazyListScope,
    list_state: ReorderableLazyListState,
    sorted_items: List<MediaItem>?
) {
    list_scope.itemsIndexed(sorted_items ?: emptyList()) { index, item ->
        val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current
        val state: SpMp.State = LocalAppState.current

        val long_press_menu_data = remember(item) {
            item.getLongPressMenuData(multiselect_context)
        }

        CompositionLocalProvider(LocalPlayerClickOverrides provides click_overrides.copy(
            onClickOverride = { _, _ ->
                if (sort_type == MediaItemSortType.NATIVE && current_filter == null) {
                    state.session.playPlaylist(playlist, index)
                    state.ui.onPlayActionOccurred()
                }
                else {
                    sorted_items?.also { items ->
                        state.session.withPlayer {
                            addMultipleToQueue(items.filterIsInstance<Song>(), clear = true)
                            seekToSong(index)
                            state.ui.onPlayActionOccurred()
                        }
                    }
                }
            }
        )) {
            ReorderableItem(list_state, key = item) { dragging ->
                Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    MediaItemPreviewLong(
                        item,
                        Modifier.fillMaxWidth().weight(1f),
                        long_press_menu_data = long_press_menu_data,
                        title_lines = 2,
                        show_artist = true,
                        show_type = false,
                        getExtraInfo = {
                            val item_duration: Long? by (item as? Song)?.Duration?.observe(state.database)
                            val ui_language: String by state.context.observeUiLanguage()
                            remember(item_duration, ui_language) {
                                listOfNotNull(
                                    item_duration?.let { duration ->
                                        durationToString(duration, ui_language, true)
                                    }
                                )
                            }
                        },
                        multiselect_context = multiselect_context
                    )

                    AnimatedVisibility(reordering) {
                        Icon(Icons.Default.Reorder, null, Modifier.padding(end = 20.dp).detectReorder(list_state))
                    }
                }
            }
        }
    }
}
