@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.utils.modifier.background

@Composable
fun StopAfterSongButton(getBackgroundColour: () -> Color, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val rotation by animateFloatAsState(
        if (player.controller?.service_player?.stop_after_current_song == true) 180f else 0f
    )

    Crossfade(player.controller?.service_player?.stop_after_current_song == true) { stopping ->
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .background(CircleShape, getBackgroundColour)
                .rotate(rotation)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        player.controller?.service_player?.stop_after_current_song = !stopping
                    },
                    onLongClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (stopping) Icons.Filled.HourglassBottom else Icons.Filled.HourglassEmpty,
                null,
                tint = getBackgroundColour().getContrasted()
            )
        }
    }
}
