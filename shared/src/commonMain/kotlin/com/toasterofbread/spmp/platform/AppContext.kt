package com.toasterofbread.spmp.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.settings.ui.ThemeManager
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.settings.ui.rememberSystemTheme
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.theme_title_system

expect class AppContext: PlatformContext {
    val database: Database
    val download_manager: PlayerDownloadManager
    val ytapi: YtmApi
    val theme: AppThemeManager
    val settings: Settings

    fun getPrefs(): PlatformPreferences
}

class AppThemeManager(
    private val context: AppContext
): ThemeValues {
    override val accent: Color
        get() = manager.accent
    override val background: Color
        get() = manager.background
    override val card: Color
        get() = manager.card
    override val on_background: Color
        get() = manager.on_background

    private var accent_colour_source: AccentColourSource? by mutableStateOf(null)

    private var _manager: ThemeManager? by mutableStateOf(null)
    val manager: ThemeManager get() = _manager!!

    @Composable
    fun Update(): Boolean {
        val current_theme: Int by context.settings.theme.CURRENT_THEME.observe()
        val themes: List<NamedTheme> by context.settings.theme.THEMES.observe()
        val system_theme: NamedTheme = rememberSystemTheme(stringResource(Res.string.theme_title_system), context)
        val composable_coroutine_scope: CoroutineScope = rememberCoroutineScope()
        var initialised: Boolean by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            _manager = object : ThemeManager(
                ThemeValuesData.fromColourScheme(context.getDarkColorScheme()),
                composable_coroutine_scope
            ) {
                override fun selectAccentColour(values: ThemeValues, thumbnail_colour: Color?): Color =
                    when(accent_colour_source ?: AccentColourSource.THEME) {
                        AccentColourSource.THEME -> values.accent
                        AccentColourSource.THUMBNAIL -> thumbnail_colour ?: values.accent
                    }
            }

            initialised = true
        }

        val theme: NamedTheme =
            themes.getOrNull(current_theme - 1)
            ?: system_theme

        LaunchedEffect(theme, initialised) {
            if (!initialised) {
                return@LaunchedEffect
            }

            manager.setTheme(theme.theme)
        }

        return initialised
    }

    fun onCurrentThumbnnailColourChanged(thumbnail_colour: Color?) {
        manager.onThumbnailColourChanged(thumbnail_colour)
    }

    private val prefs_listener: PlatformPreferencesListener =
        PlatformPreferencesListener { _, key ->
            when (key) {
                context.settings.theme.ACCENT_COLOUR_SOURCE.key -> {
                    context.coroutine_scope.launch {
                        accent_colour_source = context.settings.theme.ACCENT_COLOUR_SOURCE.get()
                    }
                }
            }
        }

    init {
        val prefs: PlatformPreferences = context.getPrefs()
        prefs.addListener(prefs_listener)

        context.coroutine_scope.launch {
            accent_colour_source = context.settings.theme.ACCENT_COLOUR_SOURCE.get()
        }
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

suspend fun AppContext.getUiLanguage(): String =
    settings.system.LANG_UI.get().ifEmpty { getDefaultLanguage() }

@Composable
fun AppContext.observeUiLanguage(): State<String> {
    val lang_ui: String by settings.system.LANG_UI.observe()
    return remember { derivedStateOf {
        lang_ui.ifEmpty { getDefaultLanguage() }
    } }
}

suspend fun AppContext.getDataLanguage(): String =
    settings.system.LANG_DATA.get().ifEmpty { getDefaultLanguage() }

@Composable
fun AppContext.observeDataLanguage(): State<String> {
    val lang_data: String by settings.system.LANG_DATA.observe()
    return remember { derivedStateOf {
        lang_data.ifEmpty { getDefaultLanguage() }
    } }
}

fun AppContext.getDefaultLanguage(): String =
    Locale.current.toLanguageTag()

fun <T> Result<T>.getOrNotify(context: AppContext, error_key: String): T? =
    fold(
        { return@fold it },
        {
            context.sendNotification(it)
            return@fold null
        }
    )
