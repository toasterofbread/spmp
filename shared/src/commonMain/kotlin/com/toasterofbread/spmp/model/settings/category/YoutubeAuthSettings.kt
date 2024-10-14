package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.packSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.PrefsPageScreen
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getYtmAuthItem
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.SettingsInterface
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import io.ktor.http.Headers
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_youtube_auth

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
                val headers: String? = YTM_HEADERS
                if (YTM_CHANNEL_ID != null && headers != null)
                    ApiAuthenticationState.packSetData(
                        YTM_CHANNEL_ID,
                        Headers.build {
                            val headers: Map<String, String> = Json.decodeFromString(headers)
                            for ((key, value) in headers) {
                                append(key, value)
                            }
                        }
                    )
                else emptySet()
            }
        }
    )

    override fun getPage(): CategoryPage? =
        object : CategoryPage(
            this,
            { stringResource(Res.string.s_cat_youtube_auth) }
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

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_youtube_auth)

    @Composable
    override fun getDescription(): String = ""

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.PlayCircle

    override fun getConfigurationItems(): List<SettingsItem> = emptyList()
}
