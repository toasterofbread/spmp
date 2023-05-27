package com.spectre7.spmp.ui.layout.prefspage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.settings.model.SettingsValueState
import com.spectre7.spmp.model.YoutubeMusicAuthInfo
import com.spectre7.spmp.ui.layout.YoutubeMusicLogin

internal fun getYoutubeMusicLoginPage(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, manual: Boolean = false): SettingsPage {
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
            YoutubeMusicLogin(Modifier.fillMaxSize(), manual = manual) { auth_info ->
                auth_info?.fold({
                    ytm_auth.value = it
                }, {
                    throw RuntimeException(it)
                })
                goBack()
            }
        }

        override suspend fun resetKeys() {
            ytm_auth.reset()
        }
    }
}
