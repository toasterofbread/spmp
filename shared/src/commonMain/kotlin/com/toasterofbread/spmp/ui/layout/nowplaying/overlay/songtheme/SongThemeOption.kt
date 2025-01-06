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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.song_theme_menu_background_image_opacity
import spmp.shared.generated.resources.song_theme_menu_corner_radius
import spmp.shared.generated.resources.song_theme_menu_gradient_depth
import spmp.shared.generated.resources.song_theme_menu_image_shadow_radius
import spmp.shared.generated.resources.song_theme_menu_queue_opacity
import spmp.shared.generated.resources.song_theme_menu_section_background
import spmp.shared.generated.resources.song_theme_menu_section_foreground
import spmp.shared.generated.resources.song_theme_menu_section_video
import spmp.shared.generated.resources.song_theme_menu_video_position
import spmp.shared.generated.resources.song_theme_menu_wave_opacity
import spmp.shared.generated.resources.song_theme_menu_wave_speed

internal abstract class SongThemeOption {
    abstract val titleResource: StringResource
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
            val title: String = stringResource(titleResource)
            Icon(icon, title, Modifier.size(20.dp))
            Text(title, fontSize = 15.sp, lineHeight = 15.sp)
        }
    }

    companion object {
        fun getSections(): Map<StringResource, List<SongThemeOption>> =
            buildMap {
                put(
                    Res.string.song_theme_menu_section_foreground,
                    listOf(
                        CornerRadius,
                        LandscapeQueueOpacity,
                        ShadowRadius,
                        PlayerGradientDepth
                    )
                )
                put(
                    Res.string.song_theme_menu_section_background,
                    listOf(
                        BackgroundWaveSpeed,
                        BackgroundWaveOpacity,
                        BackgroundImageOpacity
                    )
                )

                if (doesPlatformSupportVideoPlayback()) {
                    put(
                        Res.string.song_theme_menu_section_video,
                        listOf(
                            VideoPosition
                        )
                    )
                }
            }
    }

    private object CornerRadius: SliderOption(
        Res.string.song_theme_menu_corner_radius,
        Icons.Default.RoundedCorner,
        { Theme.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING },
        { ThumbnailRounding }
    )

    private object PlayerGradientDepth: SliderOption(
        Res.string.song_theme_menu_gradient_depth,
        Icons.Default.Gradient,
        { Theme.NOWPLAYING_DEFAULT_GRADIENT_DEPTH },
        { PlayerGradientDepth }
    )

    private object BackgroundWaveSpeed: SliderOption(
        Res.string.song_theme_menu_wave_speed,
        Icons.Default.Speed,
        { Theme.NOWPLAYING_DEFAULT_WAVE_SPEED },
        { BackgroundWaveSpeed }
    )

    private object BackgroundWaveOpacity: SliderOption(
        Res.string.song_theme_menu_wave_opacity,
        Icons.Default.Opacity,
        { Theme.NOWPLAYING_DEFAULT_WAVE_OPACITY },
        { BackgroundWaveOpacity }
    )

    private object BackgroundImageOpacity: SliderOption(
        Res.string.song_theme_menu_background_image_opacity,
        Icons.Default.Opacity,
        { Theme.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY },
        { BackgroundImageOpacity }
    )

    private object LandscapeQueueOpacity: SliderOption(
        Res.string.song_theme_menu_queue_opacity,
        Icons.Default.Opacity,
        { Theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS },
        { LandscapeQueueOpacity }
    )

    private object ShadowRadius: SliderOption(
        Res.string.song_theme_menu_image_shadow_radius,
        Icons.Default.Scale,
        { Theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS },
        { ShadowRadius }
    )

    private object VideoPosition: DropdownOption<ThemeSettings.VideoPosition>(
        ThemeSettings.VideoPosition.entries,
        { it.getReadable() },
        Res.string.song_theme_menu_video_position,
        Icons.Default.FitScreen,
        { Theme.NOWPLAYING_DEFAULT_VIDEO_POSITION },
        { VideoPosition }
    )
}
