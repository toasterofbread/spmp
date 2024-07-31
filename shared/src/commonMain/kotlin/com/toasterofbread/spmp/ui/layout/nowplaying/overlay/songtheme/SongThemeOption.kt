package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.songtheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.doesPlatformSupportVideoPlayback
import com.toasterofbread.spmp.resources.getString

internal abstract class SongThemeOption {
    abstract val title: String
    abstract val icon: ImageVector

    @Composable
    abstract fun Content(song: Song, modifier: Modifier)

    @Composable
    protected fun TitleBar(modifier: Modifier = Modifier) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, title, Modifier.size(20.dp))
            Text(title, fontSize = 15.sp, lineHeight = 15.sp)
        }
    }

    companion object {
        fun getSections(): Map<String, List<SongThemeOption>> =
            buildMap {
                put(
                    getString("song_theme_menu_section_foreground"),
                    listOf(
                        CornerRadius,
                        LandscapeQueueOpacity,
                        ShadowRadius,
                        PlayerGradientDepth
                    )
                )
                put(
                    getString("song_theme_menu_section_background"),
                    listOf(
                        BackgroundWaveSpeed,
                        BackgroundWaveOpacity,
                        BackgroundImageOpacity
                    )
                )

                if (doesPlatformSupportVideoPlayback()) {
                    put(
                        getString("song_theme_menu_section_video"),
                        listOf(
                            VideoPosition
                        )
                    )
                }
            }
    }

    private object CornerRadius: SliderOption(
        getString("song_theme_menu_corner_radius"),
        Icons.Default.RoundedCorner,
        { theme.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING },
        { ThumbnailRounding }
    )

    private object PlayerGradientDepth: SliderOption(
        getString("song_theme_menu_gradient_depth"),
        Icons.Default.Gradient,
        { theme.NOWPLAYING_DEFAULT_GRADIENT_DEPTH },
        { PlayerGradientDepth }
    )

    private object BackgroundWaveSpeed: SliderOption(
        getString("song_theme_menu_wave_speed"),
        Icons.Default.Speed,
        { theme.NOWPLAYING_DEFAULT_WAVE_SPEED },
        { BackgroundWaveSpeed }
    )

    private object BackgroundWaveOpacity: SliderOption(
        getString("song_theme_menu_wave_opacity"),
        Icons.Default.Opacity,
        { theme.NOWPLAYING_DEFAULT_WAVE_OPACITY },
        { BackgroundWaveOpacity }
    )

    private object BackgroundImageOpacity: SliderOption(
        getString("song_theme_menu_background_image_opacity"),
        Icons.Default.Opacity,
        { theme.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY },
        { BackgroundImageOpacity }
    )

    private object LandscapeQueueOpacity: SliderOption(
        getString("song_theme_menu_queue_opacity"),
        Icons.Default.Opacity,
        { theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS },
        { LandscapeQueueOpacity }
    )

    private object ShadowRadius: SliderOption(
        getString("song_theme_menu_image_shadow_radius"),
        Icons.Default.Scale,
        { theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS },
        { ShadowRadius }
    )

    private object VideoPosition: DropdownOption<ThemeSettings.VideoPosition>(
        ThemeSettings.VideoPosition.entries,
        { it.getReadable() },
        getString("song_theme_menu_video_position"),
        Icons.Default.FitScreen,
        { theme.NOWPLAYING_DEFAULT_VIDEO_POSITION },
        { VideoPosition }
    )
}
