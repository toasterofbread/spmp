package com.toasterofbread.spmp.ui.component.multiselect_context

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

@Composable
internal fun RowScope.MultiSelectSelectedItemActions(multiselect_context: MediaItemMultiSelectContext, additionalActions: (@Composable RowScope.(MediaItemMultiSelectContext) -> Unit)? = null) {
    val player = LocalPlayerState.current

    // Play after button
    Row(
        Modifier.clickable {
            player.withPlayer {
                addMultipleToQueue(
                    multiselect_context.getUniqueSelectedItems().filterIsInstance<Song>(),
                    (active_queue_index + 1).coerceAtMost(player.status.m_song_count),
                    is_active_queue = true
                )
            }
            multiselect_context.onActionPerformed()
        },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.SubdirectoryArrowRight, null)
        player.withPlayer {
            val distance = active_queue_index - player.status.m_index + 1
            Text(
                getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace(
                    "\$x",
                    distance.toString()
                ), fontSize = 15.sp
            )
        }
    }

    additionalActions?.invoke(this, multiselect_context)
}
