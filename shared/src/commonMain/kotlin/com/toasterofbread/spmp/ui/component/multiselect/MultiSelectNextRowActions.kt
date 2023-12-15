package com.toasterofbread.spmp.ui.component.multiselect

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.MultiSelectNextRowActions(multiselect_context: MediaItemMultiSelectContext) {
    val player: PlayerState = LocalPlayerState.current

    AnimatedVisibility(player.status.m_song_count > 0) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val active_queue_item: Song? =
                player.controller?.service_player?.run {
                    if (active_queue_index < song_count)
                        getSong(active_queue_index)
                    else null
                }

            CompositionLocalProvider(LocalPlayerState provides remember {
                player.copy(onClickedOverride = { _, _ ->  })
            }) {
                Crossfade(active_queue_item, animationSpec = tween(100), modifier = Modifier.weight(1f)) { song ->
                    if (song != null) {
                        MediaItemPreviewLong(song)
                    }
                }
            }

            val button_modifier: Modifier = Modifier
                .size(30.dp)
                .fillMaxHeight()
                .aspectRatio(1f)
                .align(Alignment.CenterVertically)

            Surface(
                button_modifier.combinedClickable(
                    remember { MutableInteractionSource() },
                    rememberRipple(),
                    onClick = {
                        player.controller?.service_player?.updateActiveQueueIndex(-1)
                    },
                    onLongClick = {
                        player.context.vibrateShort()
                        player.withPlayer {
                            active_queue_index = current_song_index
                        }
                    }
                ).clip(CircleShape),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, null)
            }

            Surface(
                button_modifier.combinedClickable(
                    remember { MutableInteractionSource() },
                    rememberRipple(),
                    onClick = {
                        player.controller?.service_player?.updateActiveQueueIndex(1)
                    },
                    onLongClick = {
                        player.context.vibrateShort()
                        player.withPlayer {
                            active_queue_index = song_count - 1
                        }
                    }
                ).clip(CircleShape),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, null)
            }
        }
    }
}
