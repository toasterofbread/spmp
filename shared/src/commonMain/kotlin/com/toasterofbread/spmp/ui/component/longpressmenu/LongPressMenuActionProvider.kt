package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.thenIf
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover

class LongPressMenuActionProvider(
    val getContentColour: () -> Color,
    val getAccentColour: () -> Color,
    val getBackgroundColour: () -> Color,
    val onAction: () -> Unit
) {
    @Composable
    fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, onAltClick: (() -> Unit)? = null, onAction: () -> Unit = this.onAction, fill_width: Boolean = true) =
        ActionButton(icon, label, getAccentColour, modifier = modifier, onClick = onClick, onAltClick = onAltClick, onAction = onAction, fill_width = fill_width)

    @Composable
    fun ActiveQueueIndexAction(
        getText: @Composable (distance: Int) -> String,
        onClick: (active_queue_index: Int) -> Unit,
        onLongClick: ((active_queue_index: Int) -> Unit)? = null
    ) {
        val player: PlayerState = LocalPlayerState.current
        val service: PlayerService = LocalPlayerState.current.controller ?: return

        var active_queue_item: Song? by remember { mutableStateOf(null) }
        AnimatedVisibility(service.service_player.active_queue_index < player.status.m_song_count) {
            if (service.service_player.active_queue_index < player.status.m_song_count) {
                val current_song = service.getSong(service.service_player.active_queue_index)
                if (current_song?.id != active_queue_item?.id) {
                    active_queue_item = current_song
                }
            }

            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val distance = service.service_player.active_queue_index - service.current_item_index + 1
                    ActionButton(
                        Icons.Filled.SubdirectoryArrowRight,
                        getText(distance),
                        fill_width = false,
                        onClick = { onClick(service.service_player.active_queue_index) },
                        onAltClick = onLongClick?.let { { it.invoke(service.service_player.active_queue_index) } }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val button_modifier = Modifier
                            .size(30.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .align(Alignment.CenterVertically)

                        Surface(
                            button_modifier.platformClickable(
                                onClick = {
                                    service.service_player.updateActiveQueueIndex(-1)
                                },
                                onAltClick = {
                                    player.context.vibrateShort()
                                    service.service_player.updateActiveQueueIndex(-1, to_end = true)
                                }
                            ),
                            color = getAccentColour(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Remove, null, tint = getBackgroundColour())
                        }

                        Surface(
                            button_modifier.platformClickable(
                                onClick = {
                                    service.service_player.updateActiveQueueIndex(1)
                                },
                                onAltClick = {
                                    player.context.vibrateShort()
                                    service.service_player.updateActiveQueueIndex(1, to_end = true)
                                }
                            ),
                            color = getAccentColour(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Add, null, tint = getBackgroundColour())
                        }
                    }
                }

                CompositionLocalProvider(
                    LocalPlayerClickOverrides provides LocalPlayerClickOverrides.current.copy(
                        onClickOverride = { item, _ -> player.openMediaItem(item) }
                    )
                ) {
                    Crossfade(active_queue_item, animationSpec = tween(100)) { active_item ->
                        if (active_item != null) {
                            MediaItemPreviewLong(active_item, contentColour = getContentColour)
                        }
                    }
                }
            }
        }
    }

    companion object {
        @Composable
        fun ActionButton(
            icon: ImageVector,
            label: String,
            icon_colour: () -> Color = { Color.Unspecified },
            text_colour: () -> Color = { Color.Unspecified },
            modifier: Modifier = Modifier,
            onClick: () -> Unit,
            onAltClick: (() -> Unit)? = null,
            onAction: () -> Unit,
            fill_width: Boolean = true
        ) {
            val player: PlayerState = LocalPlayerState.current

            Row(
                modifier
                    .platformClickable(
                        onClick = {
                            onClick()
                            onAction()
                        },
                        onAltClick = if (onAltClick == null) null else {
                            {
                                player.context.vibrateShort()
                                onAltClick()
                                onAction()
                            }
                        }
                    )
                    .appHover(true)
                    .thenIf(fill_width) {
                        fillMaxWidth()
                    },
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon_col = icon_colour()
                Icon(icon, null, tint = if (icon_col.isUnspecified) LocalContentColor.current else icon_col)
                Text(label, fontSize = 15.sp, color = text_colour())
            }
        }
    }
}
