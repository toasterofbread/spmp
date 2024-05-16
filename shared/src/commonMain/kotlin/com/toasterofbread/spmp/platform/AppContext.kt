package com.toasterofbread.spmp.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.composekit.settings.ui.StaticThemeData
import dev.toastbits.composekit.settings.ui.Theme
import dev.toastbits.composekit.settings.ui.ThemeData
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.YtmApi
import java.util.*

expect class AppContext: PlatformContext {
    val database: Database
    val download_manager: PlayerDownloadManager
    val ytapi: YtmApi
    val theme: Theme
    val settings: Settings

    fun getPrefs(): PlatformPreferences
}

internal class ThemeImpl(private val context: AppContext): Theme(getString("theme_title_system")) {
    private var accent_colour_source: AccentColourSource? by mutableStateOf(null)

    private val prefs_listener: PlatformPreferencesListener =
        PlatformPreferencesListener { _, key ->
            when (key) {
                context.settings.theme.ACCENT_COLOUR_SOURCE.key -> {
                    accent_colour_source = context.settings.theme.ACCENT_COLOUR_SOURCE.get()
                }
                context.settings.theme.CURRENT_THEME.key -> {
                    setCurrentThemeIdx(context.settings.theme.CURRENT_THEME.get())
                }
                context.settings.theme.THEMES.key -> {
                    reloadThemes()
                }
            }
        }

    init {
        val prefs = context.getPrefs()
        prefs.addListener(prefs_listener)
        accent_colour_source = context.settings.theme.ACCENT_COLOUR_SOURCE.get()
        setCurrentThemeIdx(context.settings.theme.CURRENT_THEME.get(), false)
    }

    override fun getDarkColorScheme(): ColorScheme =
        context.getDarkColorScheme()

    override fun getLightColorScheme(): ColorScheme =
        context.getLightColorScheme()

    override fun loadThemes(): List<StaticThemeData> {
        val themes: List<String> = context.settings.theme.THEMES.get()
        return themes.map { serialised ->
            StaticThemeData.deserialise(serialised)
        }
    }

    override fun saveThemes(themes: List<ThemeData>) {
        context.settings.theme.THEMES.set(themes.map { it.serialise() })
    }

    override fun selectAccentColour(theme_data: ThemeData, thumbnail_colour: Color?): Color =
        when(accent_colour_source ?: AccentColourSource.THEME) {
            AccentColourSource.THEME -> theme_data.accent
            AccentColourSource.THUMBNAIL -> thumbnail_colour ?: theme_data.accent
        }
}

@Composable
fun PlayerState.getDefaultHorizontalPadding(): Dp =
    if (Platform.DESKTOP.isCurrent() && FormFactor.observe().value == FormFactor.LANDSCAPE) 30.dp else 10.dp
@Composable
fun PlayerState.getDefaultVerticalPadding(): Dp =
    if (Platform.DESKTOP.isCurrent() && FormFactor.observe().value == FormFactor.LANDSCAPE) 30.dp else 10.dp

@Composable
fun PlayerState.getDefaultPaddingValues(): PaddingValues = PaddingValues(horizontal = getDefaultHorizontalPadding(), vertical = getDefaultVerticalPadding())

fun AppContext.getUiLanguage(): String =
    settings.system.LANG_UI.get().ifEmpty { getDefaultLanguage() }

fun AppContext.getDataLanguage(): String =
    settings.system.LANG_DATA.get().ifEmpty { getDefaultLanguage() }

fun AppContext.getDefaultLanguage(): String =
    Locale.getDefault().toLanguageTag()

fun <T> Result<T>.getOrNotify(context: AppContext, error_key: String): T? =
    fold(
        { return@fold it },
        {
            context.sendNotification(it)
            return@fold null
        }
    )
