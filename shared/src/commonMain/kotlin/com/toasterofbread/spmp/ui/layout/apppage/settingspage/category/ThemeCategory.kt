package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.doesPlatformSupportVideoPlayback
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.settings.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ThemeSelectorSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ThemeSelectorThemeProvider
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.settings.ui.rememberSystemTheme
import isWindowTransparencySupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_group_theming_desktop
import spmp.shared.generated.resources.s_option_accent_theme
import spmp.shared.generated.resources.s_option_accent_thumbnail
import spmp.shared.generated.resources.s_option_np_accent_background
import spmp.shared.generated.resources.s_option_np_accent_elements
import spmp.shared.generated.resources.s_option_np_accent_none
import spmp.shared.generated.resources.s_theme_editor_button_preview
import spmp.shared.generated.resources.s_theme_editor_field_accent
import spmp.shared.generated.resources.s_theme_editor_field_background
import spmp.shared.generated.resources.s_theme_editor_field_card
import spmp.shared.generated.resources.s_theme_editor_field_name
import spmp.shared.generated.resources.s_theme_editor_field_on_background
import spmp.shared.generated.resources.s_theme_editor_title
import spmp.shared.generated.resources.theme_title_new
import spmp.shared.generated.resources.theme_title_system

internal fun getThemeCategoryItems(context: AppContext): List<SettingsItem> =
    listOfNotNull(
        createThemeSelectorSettingsItem(
            context,
            context.settings.theme.CURRENT_THEME,
            getFooterModifier = {
                LocalPlayerState.current.nowPlayingTopOffset(Modifier, NowPlayingTopOffsetSection.PAGE_BAR)
            }
        ),

        MultipleChoiceSettingsItem(
            context.settings.theme.ACCENT_COLOUR_SOURCE
        ) { source ->
            stringResource(source.getNameResource())
        },

        MultipleChoiceSettingsItem(
            context.settings.theme.NOWPLAYING_THEME_MODE
        ) { mode ->
            when (mode) {
                ThemeMode.BACKGROUND -> stringResource(Res.string.s_option_np_accent_background)
                ThemeMode.ELEMENTS -> stringResource(Res.string.s_option_np_accent_elements)
                ThemeMode.NONE -> stringResource(Res.string.s_option_np_accent_none)
            }
        },

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_GRADIENT_DEPTH
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY
        ),

        if (doesPlatformSupportVideoPlayback())
            MultipleChoiceSettingsItem(
                context.settings.theme.NOWPLAYING_DEFAULT_VIDEO_POSITION
            ) { position ->
                position.getReadable()
            }
        else null,

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING
        ),

        ToggleSettingsItem(
            context.settings.theme.SHOW_EXPANDED_PLAYER_WAVE
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_WAVE_SPEED
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_WAVE_OPACITY
        )
    ) + when (Platform.current) {
        Platform.DESKTOP -> getDesktopGroupItems(context)
        else -> emptyList()
    }

private fun getDesktopGroupItems(context: AppContext): List<SettingsItem> =
    listOf(
        GroupSettingsItem(Res.string.s_group_theming_desktop)
    ) + (
        if (isWindowTransparencySupported()) getWindowTransparencyItems(context)
        else emptyList()
    )

private fun getWindowTransparencyItems(context: AppContext): List<SettingsItem> = listOf(
    ToggleSettingsItem(
        context.settings.theme.ENABLE_WINDOW_TRANSPARENCY
    ),

    AppSliderItem(
        context.settings.theme.WINDOW_BACKGROUND_OPACITY,
        range = 0f..1f
    )
)

fun createThemeSelectorSettingsItem(
    context: AppContext,
    state: PreferencesProperty<Int>,
    getExtraStartThemes: @Composable () -> List<NamedTheme> = { emptyList() },
    getFooterModifier: @Composable () -> Modifier = { Modifier }
) =
    ThemeSelectorSettingsItem(
        state,
        context.theme.manager,
        str_editor_title = Res.string.s_theme_editor_title,
        str_field_name = Res.string.s_theme_editor_field_name,
        str_field_background = Res.string.s_theme_editor_field_background,
        str_field_on_background = Res.string.s_theme_editor_field_on_background,
        str_field_card = Res.string.s_theme_editor_field_card,
        str_field_accent = Res.string.s_theme_editor_field_accent,
        str_button_preview = Res.string.s_theme_editor_button_preview,
        getFooterModifier = getFooterModifier,
        getThemeProvider = {
            val extra_start_themes: List<NamedTheme> = getExtraStartThemes()
            val start_theme_count: Int = 1 + extra_start_themes.size
            val system_theme: NamedTheme = rememberSystemTheme(stringResource(Res.string.theme_title_system), context)

            val initial_themes: List<NamedTheme> by context.settings.theme.THEMES.observe()

            return@ThemeSelectorSettingsItem object : ThemeSelectorThemeProvider {
                private var themes: List<NamedTheme> = initial_themes

                private fun setThemes(new_themes: List<NamedTheme>) {
                    themes = new_themes
                    context.settings.theme.THEMES.set(new_themes)
                }

                private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
                private val prefs_listener: PlatformPreferencesListener = PlatformPreferencesListener { key ->
                    if (key == context.settings.theme.THEMES.key) {
                        coroutine_scope.launch {
                            themes = context.settings.theme.THEMES.get()
                        }
                    }
                }

                init {
                    context.getPrefs().addListener(prefs_listener)
                }

                override fun getTheme(index: Int): NamedTheme? =
                    if (index >= 0 && index < extra_start_themes.size) extra_start_themes[index]
                    else if (index <= extra_start_themes.size) system_theme
                    else themes.getOrNull(index - start_theme_count)

                override fun getThemeCount(): Int =
                    themes.size + start_theme_count

                override fun isThemeEditable(index: Int): Boolean =
                    themes.indices.contains(index - start_theme_count)

                override suspend fun createTheme(index: Int): Int {
                    val new_theme_index: Int = (index - start_theme_count).coerceAtLeast(0)
                    val new_themes: List<NamedTheme> = themes.toMutableList().apply {
                        add(
                            new_theme_index,
                            NamedTheme(
                                getString(Res.string.theme_title_new),
                                getTheme(index - 1)?.theme ?: ThemeValuesData.of(context.theme.manager.current_theme)
                            )
                        )
                    }
                    setThemes(new_themes)
                    return new_theme_index + start_theme_count
                }

                override suspend fun removeTheme(index: Int) {
                    val new_themes: List<NamedTheme> = themes.toMutableList().apply {
                        removeAt(index - start_theme_count)
                    }
                    setThemes(new_themes)
                }

                override fun onThemeEdited(index: Int, theme: ThemeValues, theme_name: String) {
                    val new_themes: List<NamedTheme> = themes.toMutableList().apply {
                        set(index - start_theme_count, NamedTheme(theme_name, ThemeValuesData.of(theme)))
                    }
                    setThemes(new_themes)
                }
            }
        },
        getFieldModifier = { Modifier.appTextField() },
        resetThemes = {
            context.settings.theme.THEMES.reset()
        }
    )
