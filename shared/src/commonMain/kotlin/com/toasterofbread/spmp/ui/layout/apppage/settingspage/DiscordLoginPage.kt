package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.toastbits.composekit.settings.ui.SettingsPage
import dev.toastbits.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.DiscordSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.DiscordLogin
import com.toasterofbread.spmp.service.playercontroller.PlayerState

internal fun getDiscordLoginPage(discord_auth: SettingsValueState<String>, manual: Boolean = false): SettingsPage {
    return object : SettingsPage() {
        override val scrolling: Boolean
            @Composable
            get() = false

        override val title: String?
            @Composable
            get() = if (manual) getString("discord_manual_login_title") else null
        override val icon: ImageVector?
            @Composable
            get() = if (manual) DiscordSettings.getIcon() else null

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int, Any?) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            val player: PlayerState = LocalPlayerState.current

            DiscordLogin(content_padding, Modifier.fillMaxSize(), manual = manual) { auth_info ->
                if (auth_info == null) {
                    goBack()
                    return@DiscordLogin
                }

                auth_info.fold(
                    {
                        if (it != null) {
                            discord_auth.set(it)
                        }
                        goBack()
                    },
                    { error ->
                        error.message?.also {
                            player.context.sendToast(it)
                        }
                    }
                )
            }
        }

        override suspend fun resetKeys() {
            discord_auth.reset()
        }
    }
}
