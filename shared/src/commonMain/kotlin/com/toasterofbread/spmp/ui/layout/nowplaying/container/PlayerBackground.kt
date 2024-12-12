package com.toasterofbread.spmp.ui.layout.nowplaying.container

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NOW_PLAYING_LARGE_BOTTOM_BAR_HEIGHT
import dev.toastbits.composekit.components.utils.composable.wave.OverlappingWaves
import dev.toastbits.composekit.components.utils.composable.wave.WaveLayer
import dev.toastbits.composekit.components.utils.composable.wave.getDefaultOverlappingWavesLayers
import dev.toastbits.composekit.components.utils.modifier.brushBackground
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.util.thenIf
import kotlin.math.absoluteValue

private const val GRADIENT_BOTTOM_PADDING_DP: Float = 100f
private const val GRADIENT_TOP_START_RATIO: Float = 0.7f
private const val MAX_WAVE_SPEED_PORTRAIT: Float = 0.3f
private const val MAX_WAVE_SPEED_LANDSCAPE: Float = 1f

@Composable
internal fun PlayerBackground(
    page_height: Dp,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current

    val form_factor: FormFactor by NowPlayingPage.observeFormFactor()
    val current_song: Song? by player.status.song_state

    val wave_layers: List<WaveLayer> = remember {
        getDefaultOverlappingWavesLayers(7, 0.35f)
    }

    val default_wave_speed: Float by player.settings.Theme.NOWPLAYING_DEFAULT_WAVE_SPEED.observe()
    val song_wave_speed: Float? by current_song?.BackgroundWaveSpeed?.observe(player.database)
    val background_wave_speed: Float = song_wave_speed ?: default_wave_speed

    val default_wave_opacity: Float by player.settings.Theme.NOWPLAYING_DEFAULT_WAVE_OPACITY.observe()
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

    Box(
        modifier
            .offset {
                val queue_expansion: Float = expansion.get().coerceAtLeast(1f) - 1f
                IntOffset(0, (queue_expansion * page_height).roundToPx())
            }
    ) {
        ImageBackground(
            form_factor == FormFactor.LANDSCAPE,
            Modifier.requiredSize(player.screen_size.width, page_height - bottom_spacing)
        )

        val show_waves: Boolean by player.settings.Theme.SHOW_EXPANDED_PLAYER_WAVE.observe()
        if (show_waves) {
            OverlappingWaves(
                { player.theme.accent.copy(alpha = wave_alpha * expansion.getAbsolute()) },
                BlendMode.Screen,
                modifier
                    .fillMaxWidth(1f)
                    .requiredHeight(wave_height)
                    .offset {
                        IntOffset(
                            0,
                            (wave_height - bottom_spacing).roundToPx()
                        )
                    },
                layers = wave_layers,
                speed = speed
            )
        }
    }
}

@Composable
private fun ImageBackground(
    landscape: Boolean,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current

    val default_background_opacity: Float by player.settings.Theme.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY.observe()
    val song_background_opacity: Float? by player.status.m_song?.BackgroundImageOpacity?.observe(player.database)

    val background_content_opacity: Float by remember { derivedStateOf { song_background_opacity ?: default_background_opacity } }
    val show_background_content: Boolean by remember { derivedStateOf { background_content_opacity > 0f } }

    val default_video_position: ThemeSettings.VideoPosition by player.settings.Theme.NOWPLAYING_DEFAULT_VIDEO_POSITION.observe()
    val song_video_position: ThemeSettings.VideoPosition? by player.status.m_song?.VideoPosition?.observe(player.database)

    BoxWithConstraints(modifier) {
        if (show_background_content) {
            var video_showing: Boolean = false

            Box(
                Modifier
                    .fillMaxSize()
                    .playerBackground { maxHeight }
            )

            if ((song_video_position ?: default_video_position) == ThemeSettings.VideoPosition.BACKGROUND) {
                video_showing = VideoBackground(Modifier.fillMaxSize())
            }

            if (landscape && !video_showing) {
                ThumbnailBackground(Modifier.fillMaxSize())
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .thenIf(show_background_content) {
                    graphicsLayer { alpha = 1f - (background_content_opacity * (1f - (expansion.get() - 1f).absoluteValue)) }
                }
                .playerBackground { maxHeight }
        )
    }
}

private fun Modifier.playerBackground(getPageHeight: () -> Dp): Modifier = composed {
    val player: PlayerState = LocalPlayerState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current
    val density: Density = LocalDensity.current

    val default_gradient_depth: Float by player.settings.Theme.NOWPLAYING_DEFAULT_GRADIENT_DEPTH.observe()
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
