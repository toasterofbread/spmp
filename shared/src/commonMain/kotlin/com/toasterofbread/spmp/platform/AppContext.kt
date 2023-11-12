package com.toasterofbread.spmp.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import java.util.Locale

private const val MIN_PORTRAIT_RATIO: Float = 1f / 1.2f

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
