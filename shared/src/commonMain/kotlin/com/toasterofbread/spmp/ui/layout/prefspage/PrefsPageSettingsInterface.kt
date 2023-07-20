package com.toasterofbread.spmp.ui.layout.prefspage

import SpMp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.composesettings.ui.SettingsInterface
import com.toasterofbread.composesettings.ui.SettingsPageWithItems
import com.toasterofbread.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.theme.Theme

@Composable
internal fun rememberPrefsPageSettingsInterfade(pill_menu: PillMenu, ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, getCategory: () -> PrefsPageCategory?, close: () -> Unit): SettingsInterface {
    return remember {
        lateinit var settings_interface: SettingsInterface
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
            } else {
                false
            }
        }

        val discord_auth =
            SettingsValueState<String>(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name).init(Settings.prefs, Settings.Companion::provideDefault)

        settings_interface = SettingsInterface(
            { Theme.current },
            PrefsPageScreen.ROOT.ordinal,
            SpMp.context,
            Settings.prefs,
            Settings.Companion::provideDefault,
            pill_menu,
            {
                when (PrefsPageScreen.values()[it]) {
                    PrefsPageScreen.ROOT -> SettingsPageWithItems(
                        { getCategory()?.getTitle() },
                        {
                            when (getCategory()) {
                                PrefsPageCategory.GENERAL -> getGeneralCategory()
                                PrefsPageCategory.FILTER -> getFilterCategory()
                                PrefsPageCategory.FEED -> getFeedCategory()
                                PrefsPageCategory.PLAYER -> getPlayerCategory()
                                PrefsPageCategory.LIBRARY -> getLibraryCategory()
                                PrefsPageCategory.THEME -> getThemeCategory(Theme.manager)
                                PrefsPageCategory.LYRICS -> getLyricsCategory()
                                PrefsPageCategory.DOWNLOAD -> getDownloadCategory()
                                PrefsPageCategory.DISCORD_STATUS -> getDiscordStatusGroup(discord_auth)
                                PrefsPageCategory.OTHER -> getOtherCategory()
                                null -> emptyList()
                            }
                        },
                        getIcon = {
                            val icon = getCategory()?.getIcon()
                            var current_icon by remember { mutableStateOf(icon) }

                            LaunchedEffect(icon) {
                                if (icon != null) {
                                    current_icon = icon
                                }
                            }

                            return@SettingsPageWithItems current_icon
                        }
                    )
                    PrefsPageScreen.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth)
                    PrefsPageScreen.YOUTUBE_MUSIC_MANUAL_LOGIN -> getYoutubeMusicLoginPage(ytm_auth, manual = true)
                    PrefsPageScreen.DISCORD_LOGIN -> getDiscordLoginPage(discord_auth)
                    PrefsPageScreen.DISCORD_MANUAL_LOGIN -> getDiscordLoginPage(discord_auth, manual = true)
                }
            },
            { page: Int? ->
                if (page == PrefsPageScreen.ROOT.ordinal) {
                    pill_menu.removeActionOverrider(pill_menu_action_overrider)
                } else {
                    pill_menu.addActionOverrider(pill_menu_action_overrider)
                }
            },
            close
        )

        return@remember settings_interface
    }
}
