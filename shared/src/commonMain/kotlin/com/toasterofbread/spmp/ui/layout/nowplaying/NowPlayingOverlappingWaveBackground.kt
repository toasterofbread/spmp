package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.composekit.utils.composable.wave.OverlappingWaves
import com.toasterofbread.composekit.utils.composable.wave.WaveLayer
import com.toasterofbread.composekit.utils.composable.wave.getDefaultOverlappingWavesLayers
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NOW_PLAYING_LARGE_BOTTOM_BAR_HEIGHT

@Composable
fun NowPlayingOverlappingWaveBackground(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    
    val form_factor: FormFactor = NowPlayingPage.getFormFactor(player)
    
    val wave_layers: List<WaveLayer> = remember {
        getDefaultOverlappingWavesLayers(7, 0.35f)
    }

    val wave_height: Dp
    val wave_alpha: Float
    val speed: Float
    val bottom_spacing: Dp

    when (form_factor) {
        FormFactor.PORTRAIT -> {
            wave_height = player.screen_size.height * 0.5f
            wave_alpha = 0.5f
            speed = 0.15f
            bottom_spacing = 0.dp
        }
        FormFactor.LANDSCAPE -> {
            wave_height = player.screen_size.height * 0.5f
            wave_alpha = 1f
            speed = 0.5f
            bottom_spacing = NOW_PLAYING_LARGE_BOTTOM_BAR_HEIGHT
        }
    }

    OverlappingWaves(
        { player.theme.accent.copy(alpha = wave_alpha) },
        BlendMode.Screen,
        modifier
            .fillMaxWidth(1f)
            .requiredHeight(wave_height)
            .offset {
                val queue_expansion: Float = expansion.get().coerceAtLeast(1f)
                IntOffset(0, ((queue_expansion * player.screen_size.height) - bottom_spacing - wave_height).roundToPx())
            },
        layers = wave_layers,
        speed = speed
    )
}
