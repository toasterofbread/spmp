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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.AccentColourSource
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.toastercomposetools.platform.PlatformContext
import com.toasterofbread.toastercomposetools.platform.PlatformPreferences
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.toastercomposetools.settings.ui.StaticThemeData
import com.toasterofbread.toastercomposetools.settings.ui.ThemeData
import java.util.Locale

private const val MIN_PORTRAIT_RATIO: Float = 1f / 1.2f

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

    private val prefs_listener: PlatformPreferences.Listener =
        object : PlatformPreferences.Listener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
                when (key) {
                    Settings.KEY_ACCENT_COLOUR_SOURCE.name -> {
                        accent_colour_source = Settings.getEnum<AccentColourSource>(Settings.KEY_ACCENT_COLOUR_SOURCE, prefs)
                    }
                    Settings.KEY_CURRENT_THEME.name -> {
                        setCurrentThemeIdx(Settings.get(Settings.KEY_CURRENT_THEME, prefs))
                    }
                    Settings.KEY_THEMES.name -> {
                        reloadThemes()
                    }
                }
            }
        }

    private var accent_colour_source: AccentColourSource? by mutableStateOf(null)

    init {
        val prefs = context.getPrefs()
        prefs.addListener(prefs_listener)
        accent_colour_source = Settings.getEnum<AccentColourSource>(Settings.KEY_ACCENT_COLOUR_SOURCE, prefs)
        setCurrentThemeIdx(Settings.get(Settings.KEY_CURRENT_THEME, prefs), false)
    }

    override fun getDarkColorScheme(): ColorScheme =
        context.getDarkColorScheme()

    override fun getLightColorScheme(): ColorScheme =
        context.getLightColorScheme()

    override fun loadThemes(): List<ThemeData> {
        val themes = Settings.getJsonArray<String>(Settings.KEY_THEMES, gson, context.getPrefs())
        return themes.map { serialised ->
            StaticThemeData.deserialise(serialised)
        }
    }

    override fun saveThemes(themes: List<ThemeData>) {
        Settings.set(Settings.KEY_THEMES, gson.toJson(themes))
    }

    override fun selectAccentColour(theme_data: ThemeData, thumbnail_colour: Color?): Color =
        when(accent_colour_source ?: AccentColourSource.THEME) {
            AccentColourSource.THEME -> theme_data.accent
            AccentColourSource.THUMBNAIL -> thumbnail_colour ?: theme_data.accent
        }
}

expect class AppContext: PlatformContext {
    val database: Database
    val download_manager: PlayerDownloadManager
    val ytapi: YoutubeApi
    val theme: Theme

    fun getPrefs(): PlatformPreferences
}

fun PlayerState.isPortrait(): Boolean {
    return (screen_size.width / screen_size.height) <= MIN_PORTRAIT_RATIO
}

fun PlayerState.isScreenLarge(): Boolean {
    if (screen_size.width < 900.dp) {
        return false
    }
    return screen_size.height >= 600.dp && (screen_size.width / screen_size.height) > MIN_PORTRAIT_RATIO
}

@Composable
fun PlayerState.getDefaultHorizontalPadding(): Dp = if (isScreenLarge()) 30.dp else 10.dp
@Composable
fun PlayerState.getDefaultVerticalPadding(): Dp = if (isScreenLarge()) 30.dp else 10.dp // TODO

@Composable
fun PlayerState.getDefaultPaddingValues(): PaddingValues = PaddingValues(horizontal = getDefaultHorizontalPadding(), vertical = getDefaultVerticalPadding())

fun AppContext.getUiLanguage(): String =
    Settings.KEY_LANG_UI.get<String>(getPrefs()).ifEmpty { Locale.getDefault().toLanguageTag() }

fun AppContext.getDataLanguage(): String =
    Settings.KEY_LANG_DATA.get<String>(getPrefs()).ifEmpty { Locale.getDefault().toLanguageTag() }
