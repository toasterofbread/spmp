package com.toasterofbread.spmp.ui.theme

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.model.AccentColourSource
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.utils.compare
import com.toasterofbread.utils.contrastAgainst
import com.toasterofbread.utils.getContrasted
import com.catppuccin.Palette as Catppuccin

const val VIBRANT_ACCENT_CONTRAST: Float = 0.2f

object Theme: ThemeData {
    val preview_active: Boolean get() = preview_theme_data != null
    override val name: String get() = getCurrentTheme().name

    override val background: Color get() = background_state.value
    override val on_background: Color get() = on_background_state.value
    override val accent: Color get() = accent_state.value
    val on_accent: Color get() = accent.getContrasted()
    val vibrant_accent: Color get() = makeVibrant(accent)

    val background_provider: () -> Color = { background_state.value }
    val on_background_provider: () -> Color = { on_background_state.value }
    val accent_provider: () -> Color = { accent_state.value }

    fun makeVibrant(colour: Color, against: Color = background): Color {
        if (colour.compare(background) > 0.8f) {
            return colour.contrastAgainst(against, VIBRANT_ACCENT_CONTRAST)
        }
        return colour
    }

    fun getCurrentTheme(): ThemeData {
        preview_theme_data?.also {
            return it
        }

        if (current_theme_idx == 0) {
            return system_theme
        }
        else {
            return loaded_themes[current_theme_idx - 1]
        }
    }

    private val default_themes = getDefaultThemes()
    private var loaded_themes: List<ThemeData> by mutableStateOf(loadThemes())

    private var current_theme_idx: Int by mutableStateOf(0)
    private var preview_theme_data: ThemeData? by mutableStateOf(null)
    private val system_theme = ColourSchemeThemeData(getString("theme_title_system"))

    private var thumbnail_colour: Color? by mutableStateOf(null)
    private var accent_colour_source: AccentColourSource? by mutableStateOf(null)

    private val background_state: Animatable<Color, AnimationVector4D> = Animatable(loaded_themes.first().background)
    private val on_background_state: Animatable<Color, AnimationVector4D> = Animatable(loaded_themes.first().on_background)
    private val accent_state: Animatable<Color, AnimationVector4D> = Animatable(loaded_themes.first().accent)

    override fun toStaticThemeData(name: String): StaticThemeData =
        StaticThemeData(
            name,
            background_state.targetValue,
            on_background_state.targetValue,
            accent_state.targetValue
        )

    fun getThemes(): List<ThemeData> {
        return listOf(system_theme) + loaded_themes
    }
    fun getThemeCount(): Int = loaded_themes.size

    @Composable
    fun Update(context: PlatformContext) {
        DisposableEffect(Unit) {
            val prefs = context.getPrefs()
            prefs.addListener(prefs_listener)

            accent_colour_source = Settings.getEnum<AccentColourSource>(Settings.KEY_ACCENT_COLOUR_SOURCE, prefs)
            current_theme_idx = Settings.get(Settings.KEY_CURRENT_THEME, prefs)

            onDispose {
                context.getPrefs().removeListener(prefs_listener)
            }
        }

        val dark_theme = isSystemInDarkTheme()
        system_theme.colour_scheme = remember(dark_theme) {
            if (dark_theme) context.getDarkColorScheme() else context.getLightColorScheme()
        }

        val data = getCurrentTheme()
        val background_colour = data.background
        val on_background_colour = data.on_background
        val accent_colour = when(accent_colour_source ?: AccentColourSource.THEME) {
            AccentColourSource.THEME -> data.accent
            AccentColourSource.THUMBNAIL -> thumbnail_colour ?: data.accent
        }

        LaunchedEffect(background_colour) {
            background_state.animateTo(background_colour)
        }
        LaunchedEffect(on_background_colour) {
            on_background_state.animateTo(on_background_colour)
        }
        LaunchedEffect(accent_colour) {
            accent_state.animateTo(accent_colour)
        }
    }

    fun setPreviewThemeData(preview_data: ThemeData?) {
        preview_theme_data = preview_data
    }

    fun currentThumbnnailColourChanged(new_colour: Color?) {
        thumbnail_colour = new_colour
    }

    fun updateTheme(index: Int, theme: ThemeData) {
        loaded_themes = loaded_themes.toMutableList().also { it[index - 1] = theme }
        saveThemes()
    }

    fun addTheme(theme: ThemeData, index: Int = loaded_themes.size + 1) {
        loaded_themes = loaded_themes.toMutableList().also { it.add(index - 1, theme) }
        saveThemes()
    }

    fun removeTheme(index: Int) {
        if (loaded_themes.size == 1) {
            loaded_themes = default_themes
        }
        else {
            loaded_themes = loaded_themes.toMutableList().also { it.removeAt(index) }
        }
        saveThemes()
    }

    private val prefs_listener: ProjectPreferences.Listener =
        object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
                when (key) {
                    Settings.KEY_ACCENT_COLOUR_SOURCE.name -> {
                        accent_colour_source = Settings.getEnum<AccentColourSource>(Settings.KEY_ACCENT_COLOUR_SOURCE, prefs)
                    }
                    Settings.KEY_CURRENT_THEME.name -> {
                        current_theme_idx = Settings.get(Settings.KEY_CURRENT_THEME, prefs)
                    }
                    Settings.KEY_THEMES.name -> {
                        loaded_themes = loadThemes()
                    }
                }
            }
        }

    private fun getDefaultThemes(): List<ThemeData> {
        val palette = Catppuccin.MOCHA

        return listOf(
            Pair(Color(palette.mauve.rgb), "mauve"),
            Pair(Color(palette.lavender.rgb), "lavender"),
            Pair(Color(palette.red.rgb), "red"),
            Pair(Color(palette.yellow.rgb), "yellow"),
            Pair(Color(palette.green.rgb), "green"),
            Pair(Color(palette.teal.rgb), "teal"),
            Pair(Color(palette.pink.rgb), "pink"),
            Pair(Color(palette.sapphire.rgb), "sapphire"),
            Pair(Color(palette.rosewater.rgb), "rosewater"),
            Pair(Color(palette.peach.rgb), "peach"),
            Pair(Color(palette.sky.rgb), "sky"),
            Pair(Color(palette.maroon.rgb), "maroon"),
            Pair(Color(palette.blue.rgb), "blue"),
            Pair(Color(palette.flamingo.rgb), "flamingo")
        ).map { accent ->
            StaticThemeData(
                "Catppuccin ${palette.name.capitalize(Locale.current)} (${accent.second})",
                Color(palette.crust.rgb),
                Color(palette.text.rgb),
                accent.first
            )
        }
    }

    private val klaxon: Klaxon get() = Klaxon().converter(colour_converter)

    private fun saveThemes() {
        Settings.set(Settings.KEY_THEMES, klaxon.toJsonString(loaded_themes))
    }

    private fun loadThemes(): List<ThemeData> {
        val themes = Settings.getJsonArray<StaticThemeData>(Settings.KEY_THEMES, klaxon)
        if (themes.isEmpty()) {
            return default_themes
        }
        return themes
    }

    private val colour_converter: Converter get() = object : Converter {
        override fun canConvert(cls: Class<*>): Boolean {
            return cls == StaticThemeData::class.java
        }

        override fun fromJson(jv: JsonValue): Any {
            checkNotNull(jv.string) { jv }
            return StaticThemeData.deserialise(jv.string!!)
        }

        override fun toJson(value: Any): String {
            require(value is StaticThemeData)
            return "\"${value.serialise()}\""
        }
    }
}

interface ThemeData {
    val name: String
    val background: Color
    val on_background: Color
    val accent: Color

    fun isEditable(): Boolean = false
    fun toStaticThemeData(name: String): StaticThemeData
}

data class StaticThemeData(
    override val name: String,
    override val background: Color,
    override val on_background: Color,
    override val accent: Color
): ThemeData {
    fun serialise(): String {
        return "${background.toArgb()},${on_background.toArgb()},${accent.toArgb()},$name"
    }

    override fun isEditable(): Boolean = true
    override fun toStaticThemeData(name: String): StaticThemeData = copy(name = name)

    companion object {
        fun deserialise(data: String): ThemeData {
            val split = data.split(',', limit = 4)
            return StaticThemeData(
                split[3],
                Color(split[0].toInt()),
                Color(split[1].toInt()),
                Color(split[2].toInt())
            )
        }
    }
}

class ColourSchemeThemeData(
    override val name: String
): ThemeData {
    var colour_scheme: ColorScheme? by mutableStateOf(null)

    override val background: Color
        get() = colour_scheme!!.background
    override val on_background: Color
        get() = colour_scheme!!.onBackground
    override val accent: Color
        get() = colour_scheme!!.primary

    override fun toStaticThemeData(name: String): StaticThemeData =
        StaticThemeData(
            name,
            background,
            on_background,
            accent
        )
}
