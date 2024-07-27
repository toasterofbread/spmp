package com.toasterofbread.spmp.model.settings.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.SettingsInterface
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getDiscordAuthItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.PrefsPageScreen
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_discord_auth

class DiscordAuthSettings(val context: AppContext): SettingsGroup("DISCORDAUTH", context.getPrefs()) {
    val DISCORD_ACCOUNT_TOKEN: PreferencesProperty<String> by property(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.DISCORD_ACCOUNT_TOKEN ?: "" }
    )
    val DISCORD_WARNING_ACCEPTED: PreferencesProperty<Boolean> by property(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { false }
    )

    override val page: CategoryPage? =
        object : CategoryPage(
            this,
            { stringResource(Res.string.s_cat_discord_auth) }
        ) {
            override fun openPageOnInterface(context: AppContext, settings_interface: SettingsInterface) {
                val manual: Boolean = false
                settings_interface.openPageById(PrefsPageScreen.DISCORD_LOGIN.ordinal, manual)
            }

            override fun getTitleItem(context: AppContext): SettingsItem? =
                getDiscordAuthItem(
                    context,
                    info_only = true,
                    ignore_prerequisite = true,
                    StartIcon = {
                        Box(Modifier.height(40.dp).padding(end = 20.dp), contentAlignment = Alignment.Center) {
                            Icon(DiscordSettings.getIcon(), null)
                        }
                    }
                )
        }

    override fun showPage(exporting: Boolean): Boolean = exporting
}
