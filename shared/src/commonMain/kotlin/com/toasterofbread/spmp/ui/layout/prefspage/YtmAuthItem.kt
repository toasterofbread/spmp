package com.toasterofbread.spmp.ui.layout.prefspage

import LocalPlayerState
import SpMp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.composesettings.ui.item.BasicSettingsValueState
import com.toasterofbread.composesettings.ui.item.SettingsComposableItem
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsLargeToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo

@Composable
fun rememberYtmAuthItem(ytm_auth: SettingsValueState<Set<String>>, initialise: Boolean = false): SettingsItem {
    val player = LocalPlayerState.current
    var own_channel: Artist? by remember { mutableStateOf(null) }
    val login_page = player.context.ytapi.LoginPage

    return remember(login_page) {
        if (!login_page.isImplemented()) {
            return@remember SettingsComposableItem {
                login_page.NotImplementedMessage(Modifier.fillMaxSize())
            }
        }

        return@remember SettingsLargeToggleItem(
            object : BasicSettingsValueState<Boolean> {
                override fun get(): Boolean = ytm_auth.get().isNotEmpty()
                override fun set(value: Boolean) {
                    if (!value) {
                        ytm_auth.set(emptySet())
                    }
                }

                override fun init(prefs: PlatformPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
                override fun release(prefs: PlatformPreferences) {}
                override fun reset() = ytm_auth.reset()
                override fun save() = ytm_auth.save()
                override fun getDefault(defaultProvider: (String) -> Any): Boolean =
                    defaultProvider(Settings.KEY_YTM_AUTH.name) is YoutubeMusicAuthInfo
            },
            enabled_content = { modifier ->
                val data = YoutubeApi.UserAuthState.unpackSetData(ytm_auth.get(), player.context)
                if (data.first != null) {
                    own_channel = data.first
                }

                own_channel?.also { channel ->
                    MediaItemPreviewLong(channel, modifier, show_type = false)
                }
            },
            disabled_text = getString("auth_not_signed_in"),
            enable_button = getString("auth_sign_in"),
            disable_button = getString("auth_sign_out"),
            warningDialog = { dismiss, openPage ->
                login_page.LoginConfirmationDialog(false) { param ->
                    dismiss()
                    openPage(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal, param)
                }
            },
            infoDialog = { dismiss, _ ->
                login_page.LoginConfirmationDialog(true) {
                    dismiss()
                }
            }
        ) { target, setEnabled, _, openPage ->
            if (target) {
                openPage(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal, null)
            }
            else {
                setEnabled(false)
            }
        }.apply {
            if (initialise) {
                initialise(player.context, Settings.prefs, Settings.Companion::provideDefault)
            }
        }
    }
}
