package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.utils.composable.ShapedIconButton

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
            enabledContent = { modifier ->
                val data = YoutubeApi.UserAuthState.unpackSetData(ytm_auth.get(), player.context)
                if (data.first != null) {
                    own_channel = data.first
                }

                own_channel?.also { channel ->
                    MediaItemPreviewLong(channel, modifier, show_type = false)
                }
            },
            extra_items = getYoutubeAccountCategory(),
            disabled_text = getString("auth_not_signed_in"),
            enable_button = getString("auth_sign_in"),
            disable_button = getString("auth_sign_out"),
            warningDialog = { dismiss, openPage ->
                login_page.LoginConfirmationDialog(false) { param ->
                    dismiss()
                    openPage(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal, param)
                }
            },
            infoButton = { enabled, show_extra_state ->
                var show_extra: Boolean by show_extra_state
                var show_info_dialog: Boolean by remember(enabled) { mutableStateOf(false) }

                if (show_info_dialog) {
                    login_page.LoginConfirmationDialog(true) {
                        show_info_dialog = false
                    }
                }

                ShapedIconButton(
                    {
                        if (enabled) {
                            show_extra = !show_extra
                        }
                        else {
                            show_info_dialog = !show_info_dialog
                        }
                    },
                    shape = CircleShape,
                    colours = IconButtonDefaults.iconButtonColors(
                        containerColor = if (enabled) player.theme.background else player.theme.vibrant_accent,
                        contentColor = if (enabled) player.theme.on_background else player.theme.on_accent
                    ),
                    indication = null
                ) {
                    Icon(
                        if (!enabled) Icons.Default.Info
                        else if (show_extra) Icons.Default.Close
                        else Icons.Default.Settings,
                        null
                    )
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
