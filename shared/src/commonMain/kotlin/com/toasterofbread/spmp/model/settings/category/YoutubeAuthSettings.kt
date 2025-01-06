package com.toasterofbread.spmp.model.settings.category

import LocalPlayerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.model.settings.packSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getYtmAuthItem
import dev.toastbits.composekit.settings.ComposeKitSettingsGroupWithCustomPreview
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import io.ktor.http.Headers
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_youtube_auth

class YoutubeAuthSettings(val context: AppContext): SettingsGroupImpl("YTAUTH", context.getPrefs()), ComposeKitSettingsGroupWithCustomPreview {
    override fun getUnregisteredProperties(): List<PlatformSettingsProperty<*>> =
        listOf(
            context.settings.Misc.ADD_SONGS_TO_HISTORY
        )

    val YTM_AUTH: PlatformSettingsProperty<Set<String>> by property(
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

    @Composable
    override fun PreviewContent(modifier: Modifier, onSelected: () -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        val item: SettingsItem = remember { getYtmAuthItem(player.context, YTM_AUTH) }
        item.Item(modifier)
    }

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_youtube_auth)

    @Composable
    override fun getDescription(): String = ""

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.PlayCircle

    override fun getConfigurationItems(): List<SettingsItem> = emptyList()
}
