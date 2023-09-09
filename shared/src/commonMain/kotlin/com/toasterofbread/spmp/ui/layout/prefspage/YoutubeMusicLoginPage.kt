package com.toasterofbread.spmp.ui.layout.prefspage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.youtubeapi.composable.LoginPage

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
            var login_error: Throwable? by remember { mutableStateOf(null) }
            
            Crossfade(login_error) { error ->
                if (error == null) {
                    login_page.LoginPage(Modifier.fillMaxSize(), confirm_param, content_padding) { result ->
                        result?.fold(
                            { auth_info ->
                                ytm_auth.set(auth_info.getSetData())
                                goBack()
                            },
                            { error ->
                                login_error = error
                            }
                        )
                    }
                }
                else {
                    Box(Modifier.fillMaxSize().padding(content_padding), contentAlignment = Alignment.Center) {
                        ErrorInfoDisplay(
                            error,
                            Modifier.fillMaxWidth(),
                            expanded_content_modifier = Modifier.fillMaxHeight(),
                            start_expanded = true,
                            onDismiss = goBack
                        )
                    }
                }
            }
        }

        override suspend fun resetKeys() {
            ytm_auth.reset()
        }
    }
}
