package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroup
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getThemeCategoryItems
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import dev.toastbits.composekit.commonsettings.impl.group.impl.ComposeKitSettingsGroupThemeImpl
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.util.platform.Platform
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_theme
import spmp.shared.generated.resources.s_cat_theme
import spmp.shared.generated.resources.s_key_accent_source
import spmp.shared.generated.resources.s_key_enable_window_transparency
import spmp.shared.generated.resources.s_key_np_default_background_image_video_opacity
import spmp.shared.generated.resources.s_key_np_default_gradient_depth
import spmp.shared.generated.resources.s_key_np_default_image_corner_rounding
import spmp.shared.generated.resources.s_key_np_default_landscape_queue_opacity
import spmp.shared.generated.resources.s_key_np_default_shadow_radius
import spmp.shared.generated.resources.s_key_np_default_video_position
import spmp.shared.generated.resources.s_key_np_default_video_position_background
import spmp.shared.generated.resources.s_key_np_default_video_position_none
import spmp.shared.generated.resources.s_key_np_default_video_position_thumbnail
import spmp.shared.generated.resources.s_key_np_default_wave_opacity
import spmp.shared.generated.resources.s_key_np_default_wave_speed
import spmp.shared.generated.resources.s_key_np_theme_mode
import spmp.shared.generated.resources.s_key_show_expanded_player_wave
import spmp.shared.generated.resources.s_key_window_background_opacity
import spmp.shared.generated.resources.s_optionAccent_theme
import spmp.shared.generated.resources.s_optionAccent_thumbnail
import spmp.shared.generated.resources.s_sub_enable_window_transparency
import spmp.shared.generated.resources.s_sub_window_background_opacity

class ThemeSettings(val context: AppContext): ComposeKitSettingsGroupThemeImpl("THEME", context.getPrefs()), SettingsGroup {
    val ACCENT_COLOUR_SOURCE: PlatformSettingsProperty<AccentColourSource> by enumProperty(
        getName = { stringResource(Res.string.s_key_accent_source) },
        getDescription = { null },
        getDefaultValue = { AccentColourSource.DEFAULT }
    )
    val NOWPLAYING_THEME_MODE: PlatformSettingsProperty<ThemeMode> by enumProperty(
        getName = { stringResource(Res.string.s_key_np_theme_mode) },
        getDescription = { null },
        getDefaultValue = { ThemeMode.DEFAULT }
    )
    val NOWPLAYING_DEFAULT_GRADIENT_DEPTH: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_gradient_depth) },
        getDescription = { null },
        getDefaultValue = { 1f }
    )
    val NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_background_image_video_opacity) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val NOWPLAYING_DEFAULT_VIDEO_POSITION: PlatformSettingsProperty<VideoPosition> by enumProperty(
        getName = { stringResource(Res.string.s_key_np_default_video_position) },
        getDescription = { null },
        getDefaultValue = { VideoPosition.NONE }
    )
    val NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_landscape_queue_opacity) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val NOWPLAYING_DEFAULT_SHADOW_RADIUS: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_shadow_radius) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING: PlatformSettingsProperty<Float> by property(
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
    val NOWPLAYING_DEFAULT_WAVE_SPEED: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_wave_speed) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val NOWPLAYING_DEFAULT_WAVE_OPACITY: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_default_wave_opacity) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val SHOW_EXPANDED_PLAYER_WAVE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_show_expanded_player_wave) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val ENABLE_WINDOW_TRANSPARENCY: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_window_transparency) },
        getDescription = { stringResource(Res.string.s_sub_enable_window_transparency) },
        getDefaultValue = { false }
    )
    val WINDOW_BACKGROUND_OPACITY: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_window_background_opacity) },
        getDescription = { stringResource(Res.string.s_sub_window_background_opacity) },
        getDefaultValue = { 1f }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_theme)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_theme)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Palette

    override fun getConfigurationItems(): List<SettingsItem> = super.getConfigurationItems() + getThemeCategoryItems(context)

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
    THEME, THUMBNAIL;

    fun getNameResource(): StringResource =
        when (this) {
            THEME -> Res.string.s_optionAccent_theme
            THUMBNAIL -> Res.string.s_optionAccent_thumbnail
        }

    companion object {
        val DEFAULT: AccentColourSource = THUMBNAIL
    }
}
