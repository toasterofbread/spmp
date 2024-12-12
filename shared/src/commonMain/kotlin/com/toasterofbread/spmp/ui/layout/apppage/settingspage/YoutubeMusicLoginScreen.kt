package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import SpMp.isDebugBuild
import dev.toastbits.ytmkt.model.ApiAuthenticationState
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
import dev.toastbits.composekit.settings.PlatformSettingsProperty
import com.toasterofbread.spmp.model.settings.packSetData
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.LoginPage
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.navigation.screen.Screen

class YoutubeMusicLoginScreen(
    private val ytmAuth: PlatformSettingsProperty<Set<String>>,
    private val confirmParam: Any?
): Screen {
    private val loginPage: LoginPage
        @Composable
        get() = LocalPlayerState.current.context.ytapi.LoginPage

    override val title: String?
        @Composable
        get() = loginPage.getTitle(confirmParam)

//    override val icon: ImageVector?
//        @Composable
//        get() = loginPage.getIcon(confirm_param)

    @Composable
    override fun Content(navigator: Navigator, modifier: Modifier, contentPadding: PaddingValues) {
        var login_error: Throwable? by remember { mutableStateOf(null) }

        Crossfade(login_error) { error ->
            if (error == null) {
                loginPage.LoginPage(Modifier.fillMaxSize(), confirmParam, contentPadding) { result ->
                    result?.fold(
                        { auth_info ->
                            ytmAuth.set(
                                ApiAuthenticationState.packSetData(auth_info.own_channel_id, auth_info.headers)
                            )
                            navigator.navigateBackward(1)
                        },
                        { error ->
                            login_error = error
                        }
                    )
                }
            }
            else {
                Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                    ErrorInfoDisplay(
                        error,
                        isDebugBuild(),
                        Modifier.fillMaxWidth(),
                        expanded_content_modifier = Modifier.fillMaxHeight(),
                        start_expanded = true,
                        onDismiss = { navigator.navigateBackward(1) }
                    )
                }
            }
        }
    }
}
