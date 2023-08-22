package com.toasterofbread.spmp.ui.layout.prefspage

import SpMp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.DiscordLogin

internal fun getDiscordLoginPage(discord_auth: SettingsValueState<String>, manual: Boolean = false): SettingsPage {
    return object : SettingsPage() {
        override val disable_padding: Boolean
            @Composable
            get() = !manual
        override val scrolling: Boolean
            @Composable
            get() = false

        override val title: String?
            @Composable
            get() = if (manual) getString("discord_manual_login_title") else null
        override val icon: ImageVector?
            @Composable
            get() = if (manual) PrefsPageCategory.DISCORD_STATUS.getIcon() else null

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int, Any?) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            DiscordLogin(Modifier.fillMaxSize(), manual = manual) { auth_info ->
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
                            SpMp.context.sendToast(it)
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
