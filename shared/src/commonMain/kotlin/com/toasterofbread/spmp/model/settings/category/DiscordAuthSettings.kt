package com.toasterofbread.spmp.model.settings.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.settings.PlatformSettingsProperty
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_discord_auth

class DiscordAuthSettings(val context: AppContext): SettingsGroupImpl("DISCORDAUTH", context.getPrefs()) {
    val DISCORD_ACCOUNT_TOKEN: PlatformSettingsProperty<String> by property(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.DISCORD_ACCOUNT_TOKEN ?: "" }
    )
    val DISCORD_WARNING_ACCEPTED: PlatformSettingsProperty<Boolean> by property(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { false }
    )

    // TODO REFACTOR
//    override fun getPage(): CategoryPage =
//        object : CategoryPage(
//            this,
//            { stringResource(Res.string.s_cat_discord_auth) }
//        ) {
//            override fun openPage(context: AppContext) {
//                val manual: Boolean = false
//                SpMp.player_state.app_page_state.Settings.settings_interface.openPageById(PrefsPageScreen.DISCORD_LOGIN.ordinal, manual)
//            }
//
//            override fun getTitleItem(context: AppContext): SettingsItem =
//                getDiscordAuthItem(
//                    context,
//                    info_only = true,
//                    ignore_prerequisite = true,
//                    StartIcon = {
//                        Box(Modifier.height(40.dp).padding(end = 20.dp), contentAlignment = Alignment.Center) {
//                            Icon(DiscordSettings.getDiscordIcon(), null)
//                        }
//                    }
//                )
//        }

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_discord_auth)

    @Composable
    override fun getDescription(): String = ""

    @Composable
    override fun getIcon(): ImageVector = DiscordSettings.getDiscordIcon()

    override fun getConfigurationItems(): List<SettingsItem> = emptyList()

//    override fun showPage(exporting: Boolean): Boolean = exporting
}
