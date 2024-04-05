package com.toasterofbread.spmp.ui.layout.nowplaying.container

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.runtime.getValue
import com.toasterofbread.composekit.utils.modifier.brushBackground
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.*

private const val GRADIENT_BOTTOM_PADDING_DP: Float = 100f
private const val GRADIENT_TOP_START_RATIO: Float = 0.7f

internal fun Modifier.playerBackground(getPageHeight: () -> Dp): Modifier = composed {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val density: Density = LocalDensity.current

    val default_gradient_depth: Float by ThemeSettings.Key.NOWPLAYING_DEFAULT_GRADIENT_DEPTH.rememberMutableState()
    val song_gradient_depth: Float? by player.status.m_song?.PlayerGradientDepth?.observe(player.database)

    brushBackground { with (density) {
        val page_height_px: Float = getPageHeight().toPx()
        val v_offset: Float = (expansion.get() - 1f).coerceAtLeast(0f) * page_height_px

        val gradient_depth: Float = 1f - (song_gradient_depth ?: default_gradient_depth)
        check(gradient_depth in 0f .. 1f)

        return@brushBackground Brush.verticalGradient(
            listOf(player.getNPBackground(), player.getNPAltBackground()),
            startY = v_offset + (page_height_px * GRADIENT_TOP_START_RATIO),
            endY = v_offset - GRADIENT_BOTTOM_PADDING_DP.dp.toPx() + (
                page_height_px * (1.2f + (gradient_depth * 2f))
            )
        )
    } }
}