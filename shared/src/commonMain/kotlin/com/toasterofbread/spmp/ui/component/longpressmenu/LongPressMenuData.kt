package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import dev.toastbits.composekit.util.getContrasted
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewInteractionPressStage
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.ui.component.longpressmenu.artist.ArtistLongPressMenuActions
import com.toasterofbread.spmp.ui.component.longpressmenu.playlist.PlaylistLongPressMenuActions
import com.toasterofbread.spmp.ui.component.longpressmenu.song.SongLongPressMenuActions
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistSubscribeButton
import com.toasterofbread.spmp.ui.theme.appHover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

data class LongPressMenuData(
    val item: MediaItem,
    val thumb_shape: Shape? = null,
    val getTitle: (@Composable () -> String?)? = null,
    val multiselect_context: MediaItemMultiSelectContext? = null,
    val multiselect_key: Int? = null,
    val playlist_as_song: Boolean = false,
    val queue_index: Int? = null
) {
    var layout_offset: Offset? = null
    var layout_size: IntSize by Delegates.notNull()
    var click_offset: Offset by Delegates.notNull()

    var current_interaction_stage: MediaItemPreviewInteractionPressStage? by mutableStateOf(null)
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)
    private val HINT_MIN_STAGE = MediaItemPreviewInteractionPressStage.LONG_1

    fun getInteractionHintScale(): Int {
        return current_interaction_stage?.let {
            if (it < HINT_MIN_STAGE) 0
            else it.ordinal - HINT_MIN_STAGE.ordinal + 1
        } ?: 0
    }

    @Composable
    fun Actions(provider: LongPressMenuActionProvider, spacing: Dp) {
        val player = LocalPlayerState.current

        with(provider) {
            if (item is Song || (item is Playlist && playlist_as_song)) {
                SongLongPressMenuActions(item, spacing, multiselect_key) { callback ->
                    coroutine_scope.launch {
                        if (item is Song) {
                            callback(item)
                        }
                        else if (item is Playlist) {
                            item.loadData(player.context).onSuccess {
                                item.Items.get(player.database)?.firstOrNull()?.also { item ->
                                    callback(item)
                                }
                            }
                        }
                        else {
                            throw NotImplementedError(item::class.toString())
                        }
                    }
                }
            }
            else if (item is Playlist) {
                PlaylistLongPressMenuActions(item)
            }
            else if (item is Artist) {
                ArtistLongPressMenuActions(item)
            }
            else {
                throw NotImplementedError("$item (${item::class})")
            }
        }
    }

    @Composable
    fun SideButton(modifier: Modifier, background: Color) {
        val auth_state: ApiAuthenticationState? = LocalPlayerState.current.context.ytapi.user_auth_state

        when (item) {
            is Song -> LikeDislikeButton(item, auth_state, modifier) { background.getContrasted() }
            is Artist -> {
                if (auth_state != null && !item.isForItem()) {
                    ArtistSubscribeButton(item, auth_state, modifier)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun Modifier.longPressItem(long_press_menu_data: LongPressMenuData): Modifier {
    return this
        .onGloballyPositioned {
            long_press_menu_data.layout_size = it.size
            long_press_menu_data.layout_offset = it.positionInRoot()
        }
        .appHover()
}
