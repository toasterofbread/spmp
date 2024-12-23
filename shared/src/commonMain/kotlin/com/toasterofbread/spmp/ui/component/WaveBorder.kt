package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.thenIf
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.util.WaveShape
import dev.toastbits.composekit.theme.core.ThemeValues

const val WAVE_BORDER_HEIGHT_DP: Float = 20f

@Composable
fun WaveBorder(
    modifier: Modifier = Modifier,
    getColour: ThemeValues.() -> Color = { background },
    height: Dp = WAVE_BORDER_HEIGHT_DP.dp,
    getOffset: ((height: Float) -> Float)? = null,
    waves: Int = 3,
    invert: Boolean = false,
    width_multiplier: Float = 1f,
    clip_content: Boolean = false,
    getWaveOffset: (Density.() -> Float)? = null,
    border_thickness: Dp = 0.dp,
    border_colour: Color = LocalContentColor.current,
    getAlpha: () -> Float = { 1f }
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current

    val colour: Color = getColour(player.theme)
    val offset: Float? = getWaveOffset?.invoke(density)
    val shape: WaveShape = remember(waves, offset, width_multiplier) {
        WaveShape(waves, offset ?: 0f, invert = invert, width_multiplier = width_multiplier)
    }

    Box(modifier.requiredHeight(0.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .requiredHeight(height)
                .offset(
                    0.dp,
                    with(density) {
                        val user_offset: Dp = getOffset?.invoke(height.toPx())?.toDp()
                            ?: (
                                if (invert) (-height / 2) + 0.2.dp
                                else (height / 2) - 0.2.dp
                            )
                        user_offset + border_thickness
                    }
                )
                .thenIf(clip_content) {
                    clipToBounds()
                }
                .graphicsLayer { alpha = getAlpha() }
                .background(border_colour, shape)
                .offset(0.dp, -border_thickness)
                .background(colour, shape)
        )
    }
}
