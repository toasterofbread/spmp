package com.toasterofbread.spmp.ui.layout.prefspage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.settings.model.SettingsValueState
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.ui.layout.YoutubeMusicLogin

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
