package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewInteractionPressStage
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.ui.component.mediaitempreview.ArtistLongPressMenuActions
import com.toasterofbread.spmp.ui.component.mediaitempreview.PlaylistLongPressMenuActions
import com.toasterofbread.spmp.ui.component.mediaitempreview.SongLongPressMenuActions
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistSubscribeButton
import com.toasterofbread.utils.common.getContrasted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class LongPressMenuData(
    val item: MediaItem,
    val thumb_shape: Shape? = null,
    val infoContent: (@Composable ColumnScope.(accent: () -> Color) -> Unit)? = null,
    val info_title: String? = null,
    val getInitialInfoTitle: (@Composable () -> String?)? = null,
    val multiselect_context: MediaItemMultiSelectContext? = null,
    val multiselect_key: Int? = null,
    val playlist_as_song: Boolean = false
) {
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
        val auth_state = LocalPlayerState.current.context.ytapi.user_auth_state

        if (auth_state != null) {
            when (item) {
                is Song -> LikeDislikeButton(item, auth_state, modifier) { background.getContrasted() }
                is Artist -> {
                    if (!item.isForItem()) {
                        ArtistSubscribeButton(item, auth_state, modifier)
                    }
                }
                else -> {}
            }
        }
    }
}
