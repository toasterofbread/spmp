package com.toasterofbread.composesettings.ui.item

import androidx.compose.runtime.Composable
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.ui.theme.Theme

class SettingsComposableItem(val composable: @Composable Theme.() -> Unit): SettingsItem() {
    override fun initialiseValueStates(prefs: PlatformPreferences, default_provider: (String) -> Any) {}
    override fun releaseValueStates(prefs: PlatformPreferences) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(theme: Theme, openPage: (Int, Any?) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        composable(theme)
    }
}
