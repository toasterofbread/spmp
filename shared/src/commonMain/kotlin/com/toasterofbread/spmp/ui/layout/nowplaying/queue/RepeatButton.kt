package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.getInnerSquareSizeOfCircle
import com.toasterofbread.composekit.utils.composable.crossOut
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.spmp.platform.playerservice.MediaPlayerRepeatMode
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import kotlin.math.roundToInt

@Composable
fun RepeatButton(getBackgroundColour: () -> Color, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    Box(
        modifier = modifier
            .size(40.dp)
            .background(CircleShape, getBackgroundColour)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    player.controller?.repeat_mode =
                        when (player.controller?.repeat_mode) {
                            MediaPlayerRepeatMode.ALL -> MediaPlayerRepeatMode.ONE
                            MediaPlayerRepeatMode.ONE -> MediaPlayerRepeatMode.NONE
                            else -> MediaPlayerRepeatMode.ALL
                        }
                }
            )
            .crossOut(
                crossed_out = player.status.m_repeat_mode == MediaPlayerRepeatMode.NONE,
                getColour = { getBackgroundColour().getContrasted() },
            ) {
                return@crossOut IntSize(
                    (getInnerSquareSizeOfCircle(it.width * 0.5f, 50) * 1.25f).roundToInt(),
                    (getInnerSquareSizeOfCircle(it.height * 0.5f, 50) * 1.25f).roundToInt()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            when (player.status.m_repeat_mode) {
                MediaPlayerRepeatMode.ONE -> Icons.Filled.RepeatOne
                else -> Icons.Filled.Repeat
            },
            null,
            Modifier.size(20.dp),
            tint = getBackgroundColour().getContrasted()
        )
    }
}
