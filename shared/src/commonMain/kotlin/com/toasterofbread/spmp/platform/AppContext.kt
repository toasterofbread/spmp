package com.toasterofbread.spmp.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.toastbits.composekit.commonsettings.impl.group.theme.SettingsThemeManager
import dev.toastbits.composekit.context.PlatformContext
import dev.toastbits.composekit.settings.PlatformSettings
import dev.toastbits.composekit.settings.PlatformSettingsListener
import dev.toastbits.composekit.theme.core.ThemeManager
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.ytmkt.model.YtmApi
import kotlinx.coroutines.launch
import dev.toastbits.composekit.util.model.Locale as ComposeKitLocale

expect class AppContext: PlatformContext {
    val database: Database
    val download_manager: PlayerDownloadManager
    val ytapi: YtmApi
    val theme: ThemeManager
    val settings: Settings

    fun getPrefs(): PlatformSettings
}

class AppThemeManager(
    private val context: AppContext
): SettingsThemeManager(context.settings) {
    private var accent_colour_source: AccentColourSource? by
        mutableStateOf(context.settings.Theme.ACCENT_COLOUR_SOURCE.get())
    private var background_opacity: Float by
        mutableStateOf(context.settings.Theme.WINDOW_BACKGROUND_OPACITY.get())

    override fun selectAccentColour(values: ThemeValues, contextualColour: Color?): Color =
        when(accent_colour_source ?: AccentColourSource.THEME) {
            AccentColourSource.THEME -> values.accent
            AccentColourSource.THUMBNAIL -> contextualColour ?: values.accent
        }

    private val prefs_listener: PlatformSettingsListener =
        PlatformSettingsListener { key ->
            when (key) {
                context.settings.Theme.ACCENT_COLOUR_SOURCE.key -> {
                    accent_colour_source = context.settings.Theme.ACCENT_COLOUR_SOURCE.get()
                }
                context.settings.Theme.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY.key -> {
                    background_opacity = context.settings.Theme.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY.get()
                }
            }
        }

    override val background: Color
        get() = super.background.copy(alpha = background_opacity)

    override val card: Color
        get() = super.card.copy(alpha = background_opacity)

    init {
        val prefs: PlatformSettings = context.getPrefs()
        prefs.addListener(prefs_listener)

        accent_colour_source = context.settings.Theme.ACCENT_COLOUR_SOURCE.get()
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

suspend fun AppContext.getUiLanguage() =
    settings.Interface.UI_LOCALE.get() ?: getDefaultLanguage()

@Composable
fun AppContext.observeUiLanguage(): State<ComposeKitLocale> {
    val lang_ui: ComposeKitLocale? by settings.Interface.UI_LOCALE.observe()
    return remember { derivedStateOf {
        lang_ui ?: getDefaultLanguage()
    } }
}

suspend fun AppContext.getDataLanguage(): ComposeKitLocale =
    settings.Interface.DATA_LOCALE.get() ?: getDefaultLanguage()
        .let {
            if (it == ComposeKitLocale("en", "GB")) ComposeKitLocale("en", "US")
            else it
        }

@Composable
fun AppContext.observeDataLanguage(): State<ComposeKitLocale> {
    val lang_data: ComposeKitLocale? by settings.Interface.DATA_LOCALE.observe()
    return remember { derivedStateOf {
        lang_data ?: getDefaultLanguage()
    } }
}

fun AppContext.getDefaultLanguage(): ComposeKitLocale =
    ComposeKitLocale(Locale.current.language, Locale.current.region)

fun <T> Result<T>.getOrNotify(context: AppContext, error_key: String): T? =
    fold(
        { return@fold it },
        {
            context.sendNotification(it)
            return@fold null
        }
    )
