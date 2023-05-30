package com.spectre7.spmp.ui.layout.prefspage

import androidx.compose.runtime.MutableState
import com.spectre7.settings.model.BasicSettingsValueState
import com.spectre7.settings.model.SettingsItem
import com.spectre7.settings.model.SettingsItemLargeToggle
import com.spectre7.settings.model.SettingsValueState
import com.spectre7.spmp.model.mediaitem.Artist
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.YoutubeMusicAuthInfo
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.layout.YoutubeMusicLoginConfirmation

internal fun getYtmAuthItem(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, own_channel: MutableState<Artist?>): SettingsItem =
    SettingsItemLargeToggle(
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
                own_channel.value = it
            }

            own_channel.value?.PreviewLong(
                MediaItem.PreviewParams(
                    modifier
                )
            )
        },
        disabled_text = getStringTODO("Not signed in"),
        enable_button = getStringTODO("Sign in"),
        disable_button = getStringTODO("Sign out"),
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
    }
