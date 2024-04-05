package com.toasterofbread.spmp.ui.layout.nowplaying.container

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NOW_PLAYING_LARGE_BOTTOM_BAR_HEIGHT
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.ThemeSettings

private const val MAX_WAVE_SPEED_PORTRAIT: Float = 0.3f
private const val MAX_WAVE_SPEED_LANDSCAPE: Float = 1f

@Composable
internal fun WaveBackground(page_height: Dp, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current

    val form_factor: FormFactor = NowPlayingPage.getFormFactor(player)
    val current_song: Song? by player.status.song_state

    val wave_layers: List<WaveLayer> = remember {
        getDefaultOverlappingWavesLayers(7, 0.35f)
    }

    val default_wave_speed: Float by ThemeSettings.Key.NOWPLAYING_DEFAULT_WAVE_SPEED.rememberMutableState()
    val song_wave_speed: Float? by current_song?.BackgroundWaveSpeed?.observe(player.database)
    val background_wave_speed: Float = song_wave_speed ?: default_wave_speed

    val default_wave_opacity: Float by ThemeSettings.Key.NOWPLAYING_DEFAULT_WAVE_OPACITY.rememberMutableState()
    val song_wave_opacity: Float? by current_song?.BackgroundWaveOpacity?.observe(player.database)
    val background_wave_opacity: Float = song_wave_opacity ?: default_wave_opacity

    val wave_height: Dp
    val wave_alpha: Float
    val speed: Float
    val bottom_spacing: Dp

    when (form_factor) {
        FormFactor.PORTRAIT -> {
            wave_height = page_height * 0.5f
            wave_alpha = 0.5f * background_wave_opacity
            speed = MAX_WAVE_SPEED_PORTRAIT * background_wave_speed
            bottom_spacing = 0.dp
        }
        FormFactor.LANDSCAPE -> {
            wave_height = page_height * 0.5f
            wave_alpha = 1f * background_wave_opacity
            speed = MAX_WAVE_SPEED_LANDSCAPE * background_wave_speed
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
                IntOffset(0, ((queue_expansion * page_height) - bottom_spacing - wave_height).roundToPx())
            },
        layers = wave_layers,
        speed = speed
    )
}
