package com.toasterofbread.spmp.ui.layout.prefspage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.YoutubeMusicLogin
import java.net.SocketException

internal fun getYoutubeMusicLoginPage(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, manual: Boolean = false): SettingsPage {
    return object : SettingsPage() {
        override val disable_padding: Boolean = !manual
        override val scrolling: Boolean = false

        override val title: String? = if (manual) getString("youtube_manual_login_title") else null
        override val icon: ImageVector?
            @Composable
            get() = if (manual) Icons.Default.PlayCircle else null

        @Composable
        override fun TitleBar(is_root: Boolean, modifier: Modifier, goBack: () -> Unit) {}

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            YoutubeMusicLogin(Modifier.fillMaxSize(), manual = manual) { result ->
                result?.fold(
                    { auth_info ->
                        ytm_auth.set(auth_info)
                    },
                    { error ->
                        if (error !is SocketException) {
                            throw RuntimeException(error)
                        }
                    }
                )
                goBack()
            }
        }

        override suspend fun resetKeys() {
            ytm_auth.reset()
        }
    }
}
