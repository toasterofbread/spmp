package com.toasterofbread.spmp.platform

import ProgramArguments
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.settings.ui.StaticThemeData
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.settings.ui.ThemeData
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import java.util.Locale

internal class ThemeImpl(private val context: AppContext): Theme(getString("theme_title_system")) {
    private val gson: Gson
        get() = GsonBuilder().let { builder ->
        builder.registerTypeAdapter(
            StaticThemeData::class.java,
            object : TypeAdapter<StaticThemeData>() {
                override fun write(writer: JsonWriter, value: StaticThemeData?) {
                    if (value == null) {
                        writer.nullValue()
                    }
                    else {
                        writer.value(value.serialise())
                    }
                }

                override fun read(reader: JsonReader): StaticThemeData {
                    return StaticThemeData.deserialise(reader.nextString())
                }
            }
        )

        builder.create()
    }

    private val prefs_listener: PlatformPreferencesListener =
        object : PlatformPreferencesListener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
                when (key) {
                    ThemeSettings.Key.ACCENT_COLOUR_SOURCE.getName() -> {
                        accent_colour_source = Settings.getEnum<AccentColourSource>(
                            ThemeSettings.Key.ACCENT_COLOUR_SOURCE, prefs)
                    }
                    ThemeSettings.Key.CURRENT_THEME.getName() -> {
                        setCurrentThemeIdx(Settings.get(ThemeSettings.Key.CURRENT_THEME, prefs))
                    }
                    ThemeSettings.Key.THEMES.getName() -> {
                        reloadThemes()
                    }
                }
            }
        }

    private var accent_colour_source: AccentColourSource? by mutableStateOf(null)

    init {
        val prefs = context.getPrefs()
        prefs.addListener(prefs_listener)
        accent_colour_source = Settings.getEnum<AccentColourSource>(
            ThemeSettings.Key.ACCENT_COLOUR_SOURCE, prefs)
        setCurrentThemeIdx(Settings.get(ThemeSettings.Key.CURRENT_THEME, prefs), false)
    }

    override fun getDarkColorScheme(): ColorScheme =
        context.getDarkColorScheme()

    override fun getLightColorScheme(): ColorScheme =
        context.getLightColorScheme()

    override fun loadThemes(): List<ThemeData> {
        val themes: List<String> = Settings.getJsonArray(ThemeSettings.Key.THEMES, gson, context.getPrefs())
        return themes.map { serialised ->
            StaticThemeData.deserialise(serialised)
        }
    }

    override fun saveThemes(themes: List<ThemeData>) {
        Settings.set(ThemeSettings.Key.THEMES, gson.toJson(themes))
    }

    override fun selectAccentColour(theme_data: ThemeData, thumbnail_colour: Color?): Color =
        when(accent_colour_source ?: AccentColourSource.THEME) {
            AccentColourSource.THEME -> theme_data.accent
            AccentColourSource.THUMBNAIL -> thumbnail_colour ?: theme_data.accent
        }
}

expect class AppContext: PlatformContext {
    val launch_arguments: ProgramArguments
    val database: Database
    val download_manager: PlayerDownloadManager
    val ytapi: YoutubeApi
    val theme: Theme

    fun getPrefs(): PlatformPreferences
}

@Composable
fun PlayerState.getDefaultHorizontalPadding(): Dp =
    if (Platform.DESKTOP.isCurrent() && form_factor == FormFactor.LANDSCAPE) 30.dp else 10.dp
@Composable
fun PlayerState.getDefaultVerticalPadding(): Dp =
    if (Platform.DESKTOP.isCurrent() && form_factor == FormFactor.LANDSCAPE) 30.dp else 10.dp

@Composable
fun PlayerState.getDefaultPaddingValues(): PaddingValues = PaddingValues(horizontal = getDefaultHorizontalPadding(), vertical = getDefaultVerticalPadding())

fun AppContext.getUiLanguage(): String =
    SystemSettings.Key.LANG_UI.get<String>(getPrefs()).ifEmpty { Locale.getDefault().toLanguageTag() }

fun AppContext.getDataLanguage(): String =
    SystemSettings.Key.LANG_DATA.get<String>(getPrefs()).ifEmpty { Locale.getDefault().toLanguageTag() }
