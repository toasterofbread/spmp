package com.toasterofbread.spmp.model.settings.category

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.SettingsInterface
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.packSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getYtmAuthItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.PrefsPageScreen
import dev.toastbits.composekit.platform.PlatformPreferences
import io.ktor.http.Headers
import kotlinx.serialization.json.Json

class YoutubeAuthSettings(val context: AppContext): SettingsGroup("YTAUTH", context.getPrefs()) {
    override fun getUnregisteredProperties(): List<PreferencesProperty<*>> =
        listOf(
            context.settings.system.ADD_SONGS_TO_HISTORY
        )

    val YTM_AUTH: PreferencesProperty<Set<String>> by property(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = {
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
    )

    override val page: CategoryPage? =
        object : CategoryPage(
            this,
            { getString("s_cat_youtube_auth") }
        ) {
            override fun openPageOnInterface(context: AppContext, settings_interface: SettingsInterface) {
                val manual: Boolean = false
                settings_interface.openPageById(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal, manual)
            }

            override fun getTitleItem(context: AppContext): SettingsItem? =
                getYtmAuthItem(
                    context,
                    YTM_AUTH
                )
        }
}
