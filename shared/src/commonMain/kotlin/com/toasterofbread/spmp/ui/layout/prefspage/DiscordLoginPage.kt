package com.toasterofbread.spmp.ui.layout.prefspage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.settings.model.SettingsValueState
import com.toasterofbread.spmp.api.getOrReport
import com.toasterofbread.spmp.ui.layout.DiscordLogin

internal fun getDiscordLoginPage(discord_auth: SettingsValueState<String>, manual: Boolean = false): SettingsPage {
    return object : SettingsPage() {
        override val disable_padding: Boolean = true
        override val scrolling: Boolean = false

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int) -> Unit,
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
                            discord_auth.value = it
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
