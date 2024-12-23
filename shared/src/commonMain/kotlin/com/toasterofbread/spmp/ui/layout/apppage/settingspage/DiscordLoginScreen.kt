package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.DiscordLogin
import dev.toastbits.composekit.navigation.compositionlocal.LocalNavigator
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.navigation.screen.Screen
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.discord_manual_login_title

class DiscordLoginScreen(
    private val authState: PlatformSettingsProperty<String>,
    private val manual: Boolean
): Screen {
    override val title: String?
        @Composable
        get() = if (manual) stringResource(Res.string.discord_manual_login_title) else null


//    override val icon: ImageVector?
//        @Composable
//        get() = if (manual) DiscordSettings.getDiscordIcon() else null

    @Composable
    override fun Content(modifier: Modifier, contentPadding: PaddingValues) {
        val player: PlayerState = LocalPlayerState.current
        val navigator: Navigator = LocalNavigator.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()
        var exited: Boolean by remember { mutableStateOf(false) }

        DiscordLogin(contentPadding, Modifier.fillMaxSize(), manual = manual) { auth_info ->
            if (exited) {
                return@DiscordLogin
            }

            if (auth_info == null) {
                navigator.navigateBackward(1)
                exited = true
                return@DiscordLogin
            }

            auth_info.fold(
                {
                    coroutine_scope.launch {
                        if (it != null) {
                            authState.set(it)
                        }
                        navigator.navigateBackward(1)
                        exited = true
                    }
                },
                { error ->
                    error.message?.also {
                        player.context.sendToast(it)
                    }
                }
            )
        }
    }
}
