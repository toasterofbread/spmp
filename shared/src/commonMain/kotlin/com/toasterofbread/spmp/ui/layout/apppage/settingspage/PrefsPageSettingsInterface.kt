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
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.settings.ui.SettingsInterface
import com.toasterofbread.composekit.settings.ui.SettingsPageWithItems
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.Languages
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState

fun PrefsPageCategory.getCategory(context: AppContext, discord_auth_state: SettingsValueState<String>? = null): List<SettingsItem> =
    when (this) {
        PrefsPageCategory.GENERAL -> getGeneralCategory(context.getUiLanguage(), Languages.loadAvailableLanugages(context))
        PrefsPageCategory.FILTER -> getFilterCategory()
        PrefsPageCategory.FEED -> getFeedCategory()
        PrefsPageCategory.PLAYER -> getPlayerCategory()
        PrefsPageCategory.LIBRARY -> getLibraryCategory(context)
        PrefsPageCategory.THEME -> getThemeCategory(context.theme)
        PrefsPageCategory.LYRICS -> getLyricsCategory()
        PrefsPageCategory.DOWNLOAD -> getDownloadCategory()
        PrefsPageCategory.DISCORD_STATUS -> getDiscordStatusGroup(discord_auth_state!!)
        PrefsPageCategory.SERVER -> getServerCategory()
        PrefsPageCategory.OTHER -> getOtherCategory()
        PrefsPageCategory.DEVELOPMENT -> getDevelopmentCategory()
    }

internal fun getPrefsPageSettingsInterface(
    page_state: AppPageState,
    pill_menu: PillMenu,
    ytm_auth: SettingsValueState<Set<String>>,
    footer_modifier: Modifier,
    getCategory: () -> PrefsPageCategory?,
    close: () -> Unit
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

    val discord_auth: SettingsValueState<String> =
        SettingsValueState<String>(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name).init(Settings.prefs, Settings.Companion::provideDefault)

    val categories: MutableMap<PrefsPageCategory, List<SettingsItem>> = mutableMapOf()

    settings_interface = SettingsInterface(
        { context.theme },
        PrefsPageScreen.ROOT.ordinal,
        Settings.prefs,
        Settings.Companion::provideDefault,
        { context.vibrateShort() },
        { index, param ->
            when (PrefsPageScreen.values()[index]) {
                PrefsPageScreen.ROOT -> SettingsPageWithItems(
                    { getCategory()?.getTitle() },
                    {
                        val category: PrefsPageCategory = getCategory() ?: return@SettingsPageWithItems emptyList()
                        categories.getOrPut(category) {
                            category.getCategory(context, discord_auth)
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
                PrefsPageScreen.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth, param)
                PrefsPageScreen.DISCORD_LOGIN -> getDiscordLoginPage(discord_auth, manual = param == true)
                PrefsPageScreen.UI_DEBUG_INFO -> getUiDebugInfoPage()
            }
        },
        { page: Int? ->
            if (page == PrefsPageScreen.ROOT.ordinal) {
                pill_menu.removeActionOverrider(pill_menu_action_overrider)
            }
        else {
                pill_menu.addActionOverrider(pill_menu_action_overrider)
            }
        },
        close,
        footer_modifier
    )

    return settings_interface
}
