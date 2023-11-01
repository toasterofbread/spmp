package com.toasterofbread.spmp.platform

import android.app.Activity
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.toasterofbread.db.Database
import com.toasterofbread.toastercomposetools.platform.ApplicationContext
import com.toasterofbread.toastercomposetools.platform.PlatformContext
import com.toasterofbread.toastercomposetools.platform.PlatformPreferences
import com.toasterofbread.toastercomposetools.settings.ui.StaticThemeData
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.settings.ui.ThemeData
import com.toasterofbread.spmp.model.AccentColourSource
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual class AppContext(
    context: Context,
    val coroutine_scope: CoroutineScope,
    application_context: ApplicationContext? = null
): PlatformContext(context, coroutine_scope, application_context) {
    companion object {
        lateinit var main_activity: Class<out Activity>
    }

    actual val database: Database = createDatabase()
    actual val download_manager = PlayerDownloadManager(this)
    actual val ytapi: YoutubeApi
    actual val theme: Theme by lazy {
        object : Theme(getString("theme_title_system")) {
            private val gson: Gson get() = GsonBuilder().let { builder ->
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
                val prefs = getPrefs()
                prefs.addListener(prefs_listener)
                accent_colour_source = Settings.getEnum<AccentColourSource>(Settings.KEY_ACCENT_COLOUR_SOURCE, prefs)
                setCurrentThemeIdx(Settings.get(Settings.KEY_CURRENT_THEME, prefs), false)
            }

            override fun getDarkColorScheme(): ColorScheme =
                this@AppContext.getDarkColorScheme()

            override fun getLightColorScheme(): ColorScheme =
                this@AppContext.getLightColorScheme()

            override fun loadThemes(): List<ThemeData> {
                val themes = Settings.getJsonArray<String>(Settings.KEY_THEMES, gson, getPrefs())
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
    }

    init {
        val prefs = getPrefs()
        val youtubeapi_type: YoutubeApi.Type = Settings.KEY_YOUTUBEAPI_TYPE.getEnum(prefs)
        ytapi = youtubeapi_type.instantiate(this, Settings.KEY_YOUTUBEAPI_URL.get(prefs))
    }

    fun init(): AppContext {
        coroutine_scope.launch {
            ytapi.init()
        }
        return this
    }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferences.getInstance(ctx)
}
