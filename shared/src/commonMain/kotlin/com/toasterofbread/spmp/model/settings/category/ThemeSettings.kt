package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getThemeCategoryItems
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_current_theme
import spmp.shared.generated.resources.s_theme_editor_title
import spmp.shared.generated.resources.s_key_accent_source
import spmp.shared.generated.resources.s_key_np_theme_mode
import spmp.shared.generated.resources.s_key_np_default_gradient_depth
import spmp.shared.generated.resources.s_key_np_default_background_image_video_opacity
import spmp.shared.generated.resources.s_key_np_default_video_position
import spmp.shared.generated.resources.s_key_np_default_landscape_queue_opacity
import spmp.shared.generated.resources.s_key_np_default_shadow_radius
import spmp.shared.generated.resources.s_key_np_default_image_corner_rounding
import spmp.shared.generated.resources.s_key_np_default_wave_speed
import spmp.shared.generated.resources.s_key_np_default_wave_opacity
import spmp.shared.generated.resources.s_key_show_expanded_player_wave
import spmp.shared.generated.resources.s_key_enable_window_transparency
import spmp.shared.generated.resources.s_sub_enable_window_transparency
import spmp.shared.generated.resources.s_key_window_background_opacity
import spmp.shared.generated.resources.s_sub_window_background_opacity
import spmp.shared.generated.resources.s_cat_theme
import spmp.shared.generated.resources.s_cat_desc_theme
import spmp.shared.generated.resources.s_key_np_default_video_position_none
import spmp.shared.generated.resources.s_key_np_default_video_position_background
import spmp.shared.generated.resources.s_key_np_default_video_position_thumbnail

class ThemeSettings(val context: AppContext): SettingsGroup("THEME", context.getPrefs()) {
    val CURRENT_THEME: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_current_theme) },
        getDescription = { null },
        getDefaultValue = { 0 }
    )
    val THEMES: PreferencesProperty<List<String>> by serialisableProperty(
        getName = { stringResource(Res.string.s_theme_editor_title) },
        getDescription = { null },
        getDefaultValue = { emptyList() }
    )
    val ACCENT_COLOUR_SOURCE: PreferencesProperty<AccentColourSource> by enumProperty(
        getName = { stringResource(Res.string.s_key_accent_source) },
        getDescription = { null },
        getDefaultValue = { AccentColourSource.THUMBNAIL }
    )
    val NOWPLAYING_THEME_MODE: PreferencesProperty<ThemeMode> by enumProperty(
        getName = { stringResource(Res.string.s_key_np_theme_mode) },
        getDescription = { null },
        getDefaultValue = { ThemeMode.DEFAULT }
    )
    val NOWPLAYING_DEFAULT_GRADIENT_DEPTH: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_gradient_depth) },
        getDescription = { null },
        getDefaultValue = { 1f }
    )
    val NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_background_image_video_opacity) },
        getDescription = { null },
        getDefaultValue = { if (Platform.DESKTOP.isCurrent()) 0.0f else 0.5f }
    )
    val NOWPLAYING_DEFAULT_VIDEO_POSITION: PreferencesProperty<VideoPosition> by enumProperty(
        getName = { stringResource(Res.string.s_key_np_default_video_position) },
        getDescription = { null },
        getDefaultValue = { VideoPosition.NONE }
    )
    val NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_landscape_queue_opacity) },
        getDescription = { null },
        getDefaultValue = { 1f }
    )
    val NOWPLAYING_DEFAULT_SHADOW_RADIUS: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_shadow_radius) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_image_corner_rounding) },
        getDescription = { null },
        getDefaultValue = {
            when (Platform.current) {
                Platform.ANDROID -> 0.05f
                Platform.DESKTOP -> 0f
                Platform.WEB -> 0.05f
            }
        }
    )
    val NOWPLAYING_DEFAULT_WAVE_SPEED: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_wave_speed) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val NOWPLAYING_DEFAULT_WAVE_OPACITY: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_wave_opacity) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val SHOW_EXPANDED_PLAYER_WAVE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_show_expanded_player_wave) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val ENABLE_WINDOW_TRANSPARENCY: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_window_transparency) },
        getDescription = { stringResource(Res.string.s_sub_enable_window_transparency) },
        getDefaultValue = { false }
    )
    val WINDOW_BACKGROUND_OPACITY: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_window_background_opacity) },
        getDescription = { stringResource(Res.string.s_sub_window_background_opacity) },
        getDefaultValue = { 1f }
    )

    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_theme) },
            { stringResource(Res.string.s_cat_desc_theme) },
            { getThemeCategoryItems(context) },
            { Icons.Outlined.Palette }
        )

    enum class VideoPosition {
        NONE, BACKGROUND, THUMBNAIL;

        @Composable
        fun getReadable(): String =
            when (this) {
                VideoPosition.NONE -> stringResource(Res.string.s_key_np_default_video_position_none)
                VideoPosition.BACKGROUND -> stringResource(Res.string.s_key_np_default_video_position_background)
                VideoPosition.THUMBNAIL -> stringResource(Res.string.s_key_np_default_video_position_thumbnail)
            }
    }
}

enum class AccentColourSource {
    THEME, THUMBNAIL
}

