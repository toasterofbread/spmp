package com.toasterofbread.spmp.ui.component.mediaitempreview

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.api.getOrReport
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.rememberEditorOrNull
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuActionProvider
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.composable.WidthShrinkText
import kotlinx.coroutines.launch

fun getPlaylistLongPressMenuData(
    playlist: Playlist,
    thumb_shape: Shape? = RoundedCornerShape(10.dp),
    multiselect_context: MediaItemMultiSelectContext? = null
): LongPressMenuData {
    return LongPressMenuData(
        playlist,
        thumb_shape,
        { PlaylistLongPressMenuInfo(playlist, it) },
        getString("lpm_long_press_actions"),
        multiselect_context = multiselect_context
    )
}

@Composable
fun LongPressMenuActionProvider.PlaylistLongPressMenuActions(playlist: MediaItem) {
    require(playlist is Playlist)

    val player = LocalPlayerState.current
    val coroutine_context = rememberCoroutineScope()

    ActionButton(
        Icons.Default.PlayArrow, getString("lpm_action_play"),
        onClick = {
            player.playMediaItem(playlist)
        }
    )

    ActionButton(
        Icons.Default.Shuffle, getString("lpm_action_shuffle_playlist"),
        onClick = {
            TODO() // Shuffle
        }
    )

    ActiveQueueIndexAction(
        { distance ->
            getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString()) 
        },
        onClick = { active_queue_index ->
            TODO() // Insert at position
        },
        onLongClick = { active_queue_index ->
            TODO() // Insert shuffled at position
        }
    )

    val playlist_editor = playlist.rememberEditorOrNull(SpMp.context.database)
    if (playlist_editor != null) {
        ActionButton(
            Icons.Default.Delete,
            getString("playlist_delete"),
            onClick = {
                coroutine_context.launch {
                    playlist_editor.deletePlaylist().getOrReport("deletePlaylist")
                }
            }
        )
    }
}

@Composable
private fun ColumnScope.PlaylistLongPressMenuInfo(playlist: Playlist, getAccentColour: () -> Color) {
    @Composable
    fun Item(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = getAccentColour())
            WidthShrinkText(text, fontSize = 15.sp)
        }
    }
    @Composable
    fun Item() {
        Spacer(Modifier.height(25.dp))
    }

    Item() // Play
    Item() // Shuffle

    Item(Icons.Default.SubdirectoryArrowRight, getString("lpm_action_play_shuffled_after_x_songs"))

    Spacer(
        Modifier
            .fillMaxHeight()
            .weight(1f)
    )
}
