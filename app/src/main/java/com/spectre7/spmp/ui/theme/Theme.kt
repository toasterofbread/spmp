package com.spectre7.spmp.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.ViewCompat
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.model.AccentColourSource
import com.spectre7.spmp.model.Settings
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.contrastAgainst
import com.spectre7.utils.getContrasted

const val VIBRANT_ACCENT_CONTRAST: Float = 0.2f

@Composable
fun ApplicationTheme(
    font_family: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val colour_scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    val dark_theme = isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colour_scheme.background.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !dark_theme
        }
    }

    val default = Typography()

    MaterialTheme(
        colorScheme = colour_scheme,
        typography = Typography(
            displayLarge = default.displayLarge.copy(fontFamily = font_family),
            displayMedium = default.displayMedium.copy(fontFamily = font_family),
            displaySmall = default.displaySmall.copy(fontFamily = font_family),
            headlineLarge = default.headlineLarge.copy(fontFamily = font_family),
            headlineMedium = default.headlineMedium.copy(fontFamily = font_family),
            headlineSmall = default.headlineSmall.copy(fontFamily = font_family),
            titleLarge = default.titleLarge.copy(fontFamily = font_family),
            titleMedium = default.titleMedium.copy(fontFamily = font_family),
            titleSmall = default.titleSmall.copy(fontFamily = font_family),
            bodyLarge = default.bodyLarge.copy(fontFamily = font_family),
            bodyMedium = default.bodyMedium.copy(fontFamily = font_family),
            bodySmall = default.bodySmall.copy(fontFamily = font_family),
            labelLarge = default.labelLarge.copy(fontFamily = font_family),
            labelMedium = default.labelMedium.copy(fontFamily = font_family),
            labelSmall = default.labelSmall.copy(fontFamily = font_family)
        ),
        content = content
    )
}

class Theme(data: ThemeData) {
    var theme_data: ThemeData by mutableStateOf(data)
        private set

    private val background_state: Animatable<Color, AnimationVector4D> = Animatable(data.background)
    private val on_background_state: Animatable<Color, AnimationVector4D> = Animatable(data.on_background)
    private val accent_state: Animatable<Color, AnimationVector4D> = Animatable(data.accent)

    private var background_is_default = true
    private var on_background_is_default = true
    private var accent_is_default = true

    val background: Color get() = background_state.value
    val on_background: Color get() = on_background_state.value
    val accent: Color get() = accent_state.value

    val on_accent: Color get() = accent.getContrasted()
    val vibrant_accent: Color get() = accent.contrastAgainst(background, VIBRANT_ACCENT_CONTRAST)

    val background_provider: () -> Color = { background_state.value }
    val on_background_provider: () -> Color = { on_background_state.value }
    val accent_provider: () -> Color = { accent_state.value }

    suspend fun setBackground(value: Color?, snap: Boolean = false) {
        background_is_default = value == null

        val colour = value ?: theme_data.background
        if (snap)
            background_state.snapTo(colour)
        else
            background_state.animateTo(colour)
    }
    suspend fun setOnBackground(value: Color?, snap: Boolean = false) {
        on_background_is_default = value == null

        val colour = value ?: theme_data.on_background
        if (snap)
            on_background_state.snapTo(colour)
        else
            on_background_state.animateTo(colour)
    }
    suspend fun setAccent(value: Color?, snap: Boolean = false) {
        accent_is_default = value == null

        val colour = value ?: theme_data.accent
        if (snap)
            accent_state.snapTo(colour)
        else
            accent_state.animateTo(colour)
    }

    suspend fun setThemeData(theme: ThemeData, snap: Boolean = false) {
        theme_data = theme

        if (background_is_default) {
            setBackground(null, snap)
        }
        if (on_background_is_default) {
            setOnBackground(null, snap)
        }
        if (accent_is_default) {
            setAccent(null, snap)
        }
    }

    private suspend fun applyThemeData(theme: ThemeData, snap: Boolean = false) {
        setBackground(theme.background, snap)
        setOnBackground(theme.on_background, snap)
        setAccent(theme.accent, snap)
    }

    fun getDataFromCurrent(): ThemeData {
        return ThemeData(
            "If you're reading this, a bug has occurred",
            background_state.targetValue,
            on_background_state.targetValue,
            accent_state.targetValue
        )
    }

    companion object {
        val default = ThemeData("Default theme", Color.Black, Color.White, Color(99, 54, 143))
        private var thumbnail_colour: Color? = null
        private var accent_colour_source: AccentColourSource by mutableStateOf(Settings.getEnum(Settings.KEY_ACCENT_COLOUR_SOURCE))
        private var system_accent_colour: Color? = null

        private val prefs_listener: OnSharedPreferenceChangeListener =
            OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    Settings.KEY_ACCENT_COLOUR_SOURCE.name -> {
                        accent_colour_source = Settings.getEnum(Settings.KEY_ACCENT_COLOUR_SOURCE, prefs)
                    }
                }
            }

        private var _manager: ThemeManager? = null
        val manager: ThemeManager
            get() {
                if (_manager == null) {
                    _manager = ThemeManager(Settings.prefs)
                }
                return _manager!!
            }

        val theme: Theme = Theme(default)
        val preview_theme: Theme = Theme(default)

        var preview_active: Boolean by mutableStateOf(false)
            private set
        val current: Theme get() = if (preview_active) preview_theme else theme

        @Composable
        fun Update(context: Context, system_accent_colour: Color) {
            DisposableEffect(Unit) {
                this@Companion.system_accent_colour = system_accent_colour
                Settings.getPrefs(context).registerOnSharedPreferenceChangeListener(prefs_listener)

                onDispose {
                    Settings.getPrefs(context).unregisterOnSharedPreferenceChangeListener(prefs_listener)
                    manager.release()
                }
            }

            OnChangedEffect(accent_colour_source) {
                updateAccentColour()
            }

            LaunchedEffect(manager.current_theme, manager.themes) {
                println("UU ${manager.current_theme} ${manager.themes}")
                current.setThemeData(manager.themes[manager.current_theme])
            }
        }

        suspend fun startPreview(theme_data: ThemeData) {
            if (!preview_active) {
                preview_theme.applyThemeData(theme.getDataFromCurrent(), true)
                preview_active = true
            }
            preview_theme.applyThemeData(theme_data)
        }

        suspend fun stopPreview() {
            if (!preview_active) {
                return
            }
            val theme_data = theme.getDataFromCurrent()
            theme.applyThemeData(preview_theme.theme_data, true)
            theme.applyThemeData(theme_data)
            preview_active = false
        }

        suspend fun currentThumbnnailColourChanged(colour: Color?) {
            thumbnail_colour = colour
            updateAccentColour()
        }

        private suspend fun updateAccentColour() {
            theme.setAccent(when (accent_colour_source) {
                AccentColourSource.THEME -> null
                AccentColourSource.THUMBNAIL -> thumbnail_colour
                AccentColourSource.SYSTEM -> system_accent_colour!!
            })
        }
    }
}

data class ThemeData(
    val name: String, val background: Color, val on_background: Color, val accent: Color
) {
    fun serialise(): String {
        return "${background.toArgb()},${on_background.toArgb()},${accent.toArgb()},$name"
    }

    companion object {
        fun deserialise(data: String): ThemeData {
            val split = data.split(',', limit = 4)
            return ThemeData(
                split[3],
                Color(split[0].toInt()),
                Color(split[1].toInt()),
                Color(split[2].toInt())
            )
        }
    }
}

class ThemeManager(val prefs: SharedPreferences) {
    var current_theme: Int by mutableStateOf(Settings.get(Settings.KEY_CURRENT_THEME, prefs))
        private set

    var themes: List<ThemeData> by mutableStateOf(emptyList())
        private set

    private val prefs_listener = OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            Settings.KEY_CURRENT_THEME.name -> current_theme = Settings.get(Settings.KEY_CURRENT_THEME, prefs)
            Settings.KEY_THEMES.name -> loadThemes()
        }
    }

    private val colour_converter = object : Converter {
        override fun canConvert(cls: Class<*>): Boolean {
            return cls == ThemeData::class.java
        }

        override fun fromJson(jv: JsonValue): Any {
            return ThemeData.deserialise(jv.string!!)
        }

        override fun toJson(value: Any): String {
            require(value is ThemeData)
            return "\"${value.serialise()}\""
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefs_listener)
        loadThemes()
    }

    fun release() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener)
    }

    fun updateTheme(index: Int, theme: ThemeData) {
        themes = themes.toMutableList().also { it[index] = theme }
        saveThemes()
    }

    fun addTheme(theme: ThemeData) {
        themes = themes.toMutableList().also { it.add(theme) }
        saveThemes()
    }

    fun removeTheme(index: Int) {
        if (themes.size == 1) {
            themes = listOf(Theme.default)
        }
        else {
            themes = themes.toMutableList().also { it.removeAt(index) }
        }
        saveThemes()
    }

    private val klaxon: Klaxon get() = Klaxon().converter(colour_converter)

    private fun saveThemes() {
        println(klaxon.toJsonString(themes))
        Settings.set(Settings.KEY_THEMES, klaxon.toJsonString(themes), prefs)
    }

    private fun loadThemes() {
        themes = Settings.getJsonArray(Settings.KEY_THEMES, klaxon, prefs)
        if (themes.isEmpty()) {
            themes = listOf(Theme.default)
        }
    }
}
