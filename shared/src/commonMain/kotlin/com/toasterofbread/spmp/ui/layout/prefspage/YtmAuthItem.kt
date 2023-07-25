package com.toasterofbread.spmp.ui.layout.prefspage

import androidx.compose.runtime.MutableState
import com.toasterofbread.composesettings.ui.item.BasicSettingsValueState
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsLargeToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.YoutubeMusicLoginConfirmation
import androidx.compose.runtime.*

@Composable
fun rememberYtmAuthItem(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, initialise: Boolean = false): SettingsItem {
    var own_channel: Artist? by remember { mutableStateOf(null) }
    return remember { 
        SettingsLargeToggleItem(
            object : BasicSettingsValueState<Boolean> {
                override var value: Boolean
                    get() = ytm_auth.value.initialised
                    set(value) {
                        if (!value) {
                            ytm_auth.value = YoutubeMusicAuthInfo()
                        }
                    }

                override fun init(prefs: ProjectPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
                override fun reset() = ytm_auth.reset()
                override fun save() = ytm_auth.save()
                override fun getDefault(defaultProvider: (String) -> Any): Boolean =
                    defaultProvider(Settings.KEY_YTM_AUTH.name) is YoutubeMusicAuthInfo
            },
            enabled_content = { modifier ->
                ytm_auth.value.getOwnChannelOrNull()?.also {
                    own_channel = it
                }

                own_channel?.PreviewLong(
                    MediaItemPreviewParams(
                        modifier,
                        show_type = false
                    )
                )
            },
            disabled_text = getString("auth_not_signed_in"),
            enable_button = getString("auth_sign_in"),
            disable_button = getString("auth_sign_out"),
            warningDialog = { dismiss, openPage ->
                YoutubeMusicLoginConfirmation { manual ->
                    dismiss()
                    if (manual == true) {
                        openPage(PrefsPageScreen.YOUTUBE_MUSIC_MANUAL_LOGIN.ordinal)
                    } else if (manual == false) {
                        openPage(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal)
                    }
                }
            },
            infoDialog = { dismiss, _ ->
                YoutubeMusicLoginConfirmation(true) {
                    dismiss()
                }
            }
        ) { target, setEnabled, _, openPage ->
            if (target) {
                openPage(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal)
            } else {
                setEnabled(false)
            }
        }.apply {
            if (initialise) {
                initialise(SpMp.context, Settings.prefs, Settings.Companion::provideDefault) 
            }
        }
    }
}
