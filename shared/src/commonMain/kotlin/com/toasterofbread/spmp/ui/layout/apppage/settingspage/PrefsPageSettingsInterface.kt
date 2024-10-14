package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.platform.vibrateShort
import dev.toastbits.composekit.settings.ui.SettingsInterface
import dev.toastbits.composekit.settings.ui.SettingsPageWithItems
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState

internal fun getPrefsPageSettingsInterface(
    page_state: AppPageState,
    pill_menu: PillMenu,
    ytm_auth: PreferencesProperty<Set<String>>
): SettingsInterface {
    lateinit var settings_interface: SettingsInterface
    val context: AppContext = page_state.context

    val pill_menu_action_overrider: @Composable PillMenu.Action.(i: Int) -> Boolean = { i ->
        if (i == 0) {
            var go_back by remember { mutableStateOf(false) }
            LaunchedEffect(go_back) {
                if (go_back) {
                    settings_interface.goBack()
                }
            }

            ActionButton(
                Icons.Filled.ArrowBack
            ) {
                go_back = true
            }
            true
        }
        else {
            false
        }
    }

    val discord_auth: PreferencesProperty<String> = context.settings.discord_auth.DISCORD_ACCOUNT_TOKEN

    settings_interface = SettingsInterface(
        context,
        context.getPrefs(),
        { context.theme },
        PrefsPageScreen.ROOT.ordinal,
        { index, param ->
            when (PrefsPageScreen.entries[index]) {
                PrefsPageScreen.ROOT -> SettingsPageWithItems(
                    { null },
                    { emptyList() }
                )
                PrefsPageScreen.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth, param)
                PrefsPageScreen.DISCORD_LOGIN -> getDiscordLoginPage(discord_auth, manual = param == true)
                PrefsPageScreen.UI_DEBUG_INFO -> getUiDebugInfoPage()
            }
        },
        { context.vibrateShort() },
        { page: Int? ->
            if (page == PrefsPageScreen.ROOT.ordinal) {
                pill_menu.removeActionOverrider(pill_menu_action_overrider)
            }
            else {
                pill_menu.addActionOverrider(pill_menu_action_overrider)
            }
        }
    )

    return settings_interface
}
