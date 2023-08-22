package com.toasterofbread.spmp.ui.layout.prefspage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.youtubeapi.composable.LoginPage
import java.net.SocketException

internal fun getYoutubeMusicLoginPage(
    ytm_auth: SettingsValueState<Set<String>>,
    confirm_param: Any?
): SettingsPage {
    return object : SettingsPage() {
        val login_page: LoginPage
            @Composable
            get() {
                return LocalPlayerState.current.context.ytapi.LoginPage
            }

        override val disable_padding: Boolean
            @Composable
            get() = login_page.targetsDisabledPadding(confirm_param)
        override val scrolling: Boolean
            @Composable
            get() = false

        override val title: String?
            @Composable
            get() = login_page.getTitle(confirm_param)
        override val icon: ImageVector?
            @Composable
            get() = login_page.getIcon(confirm_param)

        @Composable
        override fun TitleBar(is_root: Boolean, modifier: Modifier, goBack: () -> Unit) {}

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int, Any?) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            login_page.LoginPage(Modifier.fillMaxSize(), confirm_param) { result ->
                result?.fold(
                    { auth_info ->
                        ytm_auth.set(auth_info.getSetData())
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
