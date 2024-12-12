package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.getInnerSquareSizeOfCircle
import dev.toastbits.composekit.components.utils.composable.crossOut
import dev.toastbits.composekit.components.utils.modifier.background
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import com.toasterofbread.spmp.service.playercontroller.PlayerState
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
                    player.controller?.setRepeatMode(
                        when (player.controller?.repeat_mode) {
                            SpMsPlayerRepeatMode.ALL -> SpMsPlayerRepeatMode.ONE
                            SpMsPlayerRepeatMode.ONE -> SpMsPlayerRepeatMode.NONE
                            else -> SpMsPlayerRepeatMode.ALL
                        }
                    )
                }
            )
            .crossOut(
                crossed_out = player.status.m_repeat_mode == SpMsPlayerRepeatMode.NONE,
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
                SpMsPlayerRepeatMode.ONE -> Icons.Filled.RepeatOne
                else -> Icons.Filled.Repeat
            },
            null,
            Modifier.size(20.dp),
            tint = getBackgroundColour().getContrasted()
        )
    }
}
