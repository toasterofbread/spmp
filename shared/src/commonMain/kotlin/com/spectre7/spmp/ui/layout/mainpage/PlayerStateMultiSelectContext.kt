package com.spectre7.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.mediaitem.MediaItemPreviewParams
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext

fun getPlayerStateMultiSelectContext(): MediaItemMultiSelectContext =
    MediaItemMultiSelectContext(
        selectedItemActions = { multiselect ->
            val player = LocalPlayerState.current

            // Play after button
            Row(
                Modifier.clickable {
                    player.withPlayer {
                        addMultipleToQueue(
                            multiselect.getUniqueSelectedItems().filterIsInstance<Song>(),
                            (active_queue_index + 1).coerceAtMost(player.status.m_song_count),
                            is_active_queue = true
                        )
                    }
                    multiselect.onActionPerformed()
                },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SubdirectoryArrowRight, null)
                player.player?.apply {
                    val distance = active_queue_index - player.status.m_index + 1
                    Text(
                        getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace(
                            "\$x",
                            distance.toString()
                        ), fontSize = 15.sp
                    )
                }
            }
        },
        nextRowSelectedItemActions = { multiselect ->
            // Play after controls and song indicator
            AnimatedVisibility(LocalPlayerState.current.status.m_song_count > 0) {
                MultiSelectNextRowActions()
            }
        }
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiSelectNextRowActions() {
    val player = LocalPlayerState.current

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val active_queue_item =
            player.player?.run {
                if (active_queue_index < song_count)
                    getSong(active_queue_index)
                else null
            }

        CompositionLocalProvider(LocalPlayerState provides remember {
            player.copy(onClickedOverride = { item, _ -> player.openMediaItem(item) })
        }) {
            Crossfade(active_queue_item, animationSpec = tween(100), modifier = Modifier.weight(1f)) {
                it?.PreviewLong(MediaItemPreviewParams())
            }
        }

        val button_modifier = Modifier
            .size(30.dp)
            .fillMaxHeight()
            .aspectRatio(1f)
            .align(Alignment.CenterVertically)

        Surface(
            button_modifier.combinedClickable(
                remember { MutableInteractionSource() },
                rememberRipple(),
                onClick = {
                    player.player?.updateActiveQueueIndex(-1)
                },
                onLongClick = {
                    SpMp.context.vibrateShort()
                    player.withPlayer {
                        active_queue_index = current_song_index
                    }
                }
            ),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Remove, null)
        }

        Surface(
            button_modifier.combinedClickable(
                remember { MutableInteractionSource() },
                rememberRipple(),
                onClick = {
                    player.player?.updateActiveQueueIndex(1)
                },
                onLongClick = {
                    SpMp.context.vibrateShort()
                    player.withPlayer {
                        active_queue_index = song_count - 1
                    }
                }
            ),
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, null)
        }
    }
}
