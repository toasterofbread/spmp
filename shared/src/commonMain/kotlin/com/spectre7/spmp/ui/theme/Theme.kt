package com.spectre7.spmp.ui.theme

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.model.AccentColourSource
import com.spectre7.spmp.model.Settings
import com.spectre7.utils.*
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.ProjectContext

const val VIBRANT_ACCENT_CONTRAST: Float = 0.2f

@Composable
fun ApplicationTheme(
    context: ProjectContext,
    font_family: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val dark_theme = isSystemInDarkTheme()
    val colour_scheme = if (dark_theme) context.getDarkColorScheme() else context.getLightColorScheme()

//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        SideEffect {
//            (view.context as Activity).window.statusBarColor = colour_scheme.background.toArgb()
//            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !dark_theme
//        }
//    }

    val typography = MaterialTheme.typography.copy(
        h1 = MaterialTheme.typography.h1.copy(fontFamily = font_family),
        h2 = MaterialTheme.typography.h2.copy(fontFamily = font_family),
        h3 = MaterialTheme.typography.h3.copy(fontFamily = font_family),
        h4 = MaterialTheme.typography.h4.copy(fontFamily = font_family),
        h5 = MaterialTheme.typography.h5.copy(fontFamily = font_family),
        h6 = MaterialTheme.typography.h6.copy(fontFamily = font_family),
        subtitle1 = MaterialTheme.typography.subtitle1.copy(fontFamily = font_family),
        subtitle2 = MaterialTheme.typography.subtitle2.copy(fontFamily = font_family),
        body1 = MaterialTheme.typography.body1.copy(fontFamily = font_family),
        body2 = MaterialTheme.typography.body2.copy(fontFamily = font_family),
        button = MaterialTheme.typography.button.copy(fontFamily = font_family),
        caption = MaterialTheme.typography.caption.copy(fontFamily = font_family),
        overline = MaterialTheme.typography.overline.copy(fontFamily = font_family)
    )
    
    MaterialTheme(
        colors = colour_scheme,
        typography = typography,
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

    val vibrant_accent: Color get() {
        if (accent.compare(background) > 0.8f) {
            return accent.contrastAgainst(background, VIBRANT_ACCENT_CONTRAST)
        }
        return accent
    }

    val background_provider: () -> Color = { background_state.value }
    val on_background_provider: () -> Color = { on_background_state.value }
    val accent_provider: () -> Color = { accent_state.value }
    val on_accent_provider: () -> Color = { accent_state.value.getContrasted() }

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
        val default = ThemeData(getStringTemp("Default theme"), Color.Black, Color.White, Color(99, 54, 143))
        private var thumbnail_colour: Color? = null
        private var accent_colour_source: AccentColourSource by mutableStateOf(Settings.getEnum(Settings.KEY_ACCENT_COLOUR_SOURCE))
        private var system_accent_colour: Color? = null

        private val prefs_listener: ProjectPreferences.Listener =
            object : ProjectPreferences.Listener {
                override fun onChanged(prefs: ProjectPreferences, key: String) {
                    when (key) {
                        Settings.KEY_ACCENT_COLOUR_SOURCE.name -> {
                            accent_colour_source = Settings.getEnum(Settings.KEY_ACCENT_COLOUR_SOURCE, prefs)
                        }
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
        fun Update(context: ProjectContext, system_accent_colour: Color) {
            DisposableEffect(Unit) {
                this@Companion.system_accent_colour = system_accent_colour
                context.getPrefs().addListener(prefs_listener)

                onDispose {
                    context.getPrefs().removeListener(prefs_listener)
                    manager.release()
                }
            }

            OnChangedEffect(accent_colour_source) {
                updateAccentColour()
            }

            LaunchedEffect(manager.current_theme, manager.themes) {
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

class ThemeManager(val prefs: ProjectPreferences) {
    var current_theme: Int by mutableStateOf(Settings.get(Settings.KEY_CURRENT_THEME, prefs))
        private set

    var themes: List<ThemeData> by mutableStateOf(emptyList())
        private set

    private val prefs_listener = object : ProjectPreferences.Listener {
        override fun onChanged(prefs: ProjectPreferences, key: String) {
            when (key) {
                Settings.KEY_CURRENT_THEME.name -> current_theme = Settings.get(Settings.KEY_CURRENT_THEME, prefs)
                Settings.KEY_THEMES.name -> loadThemes()
            }
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
        prefs.addListener(prefs_listener)
        loadThemes()
    }

    fun release() {
        prefs.removeListener(prefs_listener)
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
        Settings.set(Settings.KEY_THEMES, klaxon.toJsonString(themes), prefs)
    }

    private fun loadThemes() {
        themes = Settings.getJsonArray(Settings.KEY_THEMES, klaxon, prefs)
        if (themes.isEmpty()) {
            themes = listOf(Theme.default)
        }
    }
}
