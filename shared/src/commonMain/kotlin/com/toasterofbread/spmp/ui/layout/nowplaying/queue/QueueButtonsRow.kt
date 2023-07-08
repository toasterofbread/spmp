@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import SpMp
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.utils.getContrasted
import com.toasterofbread.utils.setAlpha

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueButtonsRow(
    backgroundColourProvider: () -> Color,
    multiselect_context: MediaItemMultiSelectContext
) {
    val padding = 10.dp
    val player = LocalPlayerState.current
    val background_colour = getNPBackground()

    Row(
        Modifier
            .padding(top = padding, start = padding, end = padding, bottom = 10.dp)
            .height(40.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RepeatButton(backgroundColourProvider, Modifier.fillMaxHeight())
        StopAfterSongButton(backgroundColourProvider, Modifier.fillMaxHeight())

        Button(
            onClick = {
                player.player?.undoableAction {
                    if (multiselect_context.is_active) {
                        for (item in multiselect_context.getSelectedItems().sortedByDescending { it.second!! }) {
                            player.player?.removeFromQueue(item.second!!)
                        }
                        multiselect_context.onActionPerformed()
                    } else {
                        player.player?.clearQueue(keep_current = player.status.m_song_count > 1)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = background_colour,
                contentColor = background_colour.getContrasted()
            ),
            border = multiselect_context.getActiveHintBorder()
        ) {
            Text(getString("queue_clear"))
        }

        Surface(
            Modifier.combinedClickable(
                onClick = {
                    if (multiselect_context.is_active) {
                        player.player?.undoableAction {
                            player.player?.shuffleQueueIndices(multiselect_context.getSelectedItems().map { it.second!! })
                        }
                        multiselect_context.onActionPerformed()
                    } else {
                        player.player?.undoableAction {
                            player.player?.shuffleQueue()
                        }
                    }
                },
                onLongClick = if (multiselect_context.is_active) null else ({
                    player.player?.undoableAction {
                        if (!multiselect_context.is_active) {
                            SpMp.context.vibrateShort()
                            player.player?.shuffleQueue(start = 0)
                        }
                    }
                })
            ),
            color = background_colour,
            shape = FilledButtonTokens.ContainerShape.toShape(),
            border = multiselect_context.getActiveHintBorder()
        ) {
            Box(
                Modifier
                    .defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight
                    )
                    .padding(ButtonDefaults.ContentPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString("queue_shuffle"),
//                                color = background_colour.getContrasted(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        val undo_background = animateColorAsState(
            if (player.status.m_undo_count != 0) LocalContentColor.current
            else LocalContentColor.current.setAlpha(0.3f)
        ).value

        Box(
            modifier = Modifier
                .minimumTouchTargetSize()
                .background(
                    undo_background,
                    CircleShape
                )
                .combinedClickable(
                    enabled = player.status.m_undo_count != 0 || player.status.m_redo_count != 0,
                    onClick = {
                        player.player?.undo()
                        SpMp.context.vibrateShort()
                    },
                    onLongClick = {
                        player.player?.redo()
                        SpMp.context.vibrateShort()
                    }
                )
                .size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Undo, null, tint = undo_background.getContrasted(true))
        }
    }
}
