package com.toasterofbread.spmp.model.settings.category

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsValueState
import dev.toastbits.composekit.settings.ui.SettingsInterface
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.packSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getYtmAuthItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.PrefsPageScreen
import io.ktor.http.Headers
import kotlinx.serialization.json.Json

data object YoutubeAuthSettings: SettingsCategory("ytauth") {
    override val keys: List<SettingsKey> = Key.entries.toList() + listOf(SystemSettings.Key.ADD_SONGS_TO_HISTORY)

    override fun getPage(): CategoryPage? =
        object : CategoryPage(
            this,
            getString("s_cat_youtube_auth")
        ) {
            override fun openPageOnInterface(context: AppContext, settings_interface: SettingsInterface) {
                val manual: Boolean = false
                settings_interface.openPageById(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal, manual)
            }

            override fun getTitleItem(context: AppContext): SettingsItem? =
                getYtmAuthItem(
                    context,
                    SettingsValueState<Set<String>>(Key.YTM_AUTH.getName()).init(context.getPrefs(), Settings::provideDefault),
                    true
                )
        }

    enum class Key: SettingsKey {
        YTM_AUTH;

        override val category: SettingsCategory get() = YoutubeAuthSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                YTM_AUTH -> {
                    with(ProjectBuildConfig) {
                        if (YTM_CHANNEL_ID != null && YTM_HEADERS != null)
                            ApiAuthenticationState.packSetData(
                                YTM_CHANNEL_ID,
                                Headers.build {
                                    val headers: Map<String, String> = Json.decodeFromString(YTM_HEADERS)
                                    for ((key, value) in headers) {
                                        append(key, value)
                                    }
                                }
                            )
                        else emptySet()
                    }
                }
            } as T
    }
}
