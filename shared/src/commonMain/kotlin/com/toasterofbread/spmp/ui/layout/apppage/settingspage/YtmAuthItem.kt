package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import dev.toastbits.ytmkt.model.ApiAuthenticationState
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
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.settings.ui.item.BasicSettingsValueState
import dev.toastbits.composekit.settings.ui.item.ComposableSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.LargeToggleSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsValueState
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.YoutubeAuthSettings
import com.toasterofbread.spmp.model.settings.unpackSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getYoutubeAccountCategory
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.ui.component.NotImplementedMessage
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.LoginPage
import io.ktor.http.Headers

fun getYtmAuthItem(context: AppContext, ytm_auth: SettingsValueState<Set<String>>, initialise: Boolean = false): SettingsItem {
    var own_channel: Artist? by mutableStateOf(null)
    val login_page: LoginPage = context.ytapi.LoginPage

    if (!login_page.isImplemented()) {
        return ComposableSettingsItem {
            login_page.NotImplementedMessage(Modifier.fillMaxSize())
        }
    }

    return LargeToggleSettingsItem(
        object : BasicSettingsValueState<Boolean> {
            override fun getKeys(): List<String> = ytm_auth.getKeys()
            override fun get(): Boolean = ytm_auth.get().isNotEmpty()
            override fun set(value: Boolean) {
                if (!value) {
                    ytm_auth.set(emptySet())
                }
            }

            override fun init(prefs: PlatformPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
            override fun release(prefs: PlatformPreferences) {}
            override fun setEnableAutosave(value: Boolean) {}
            override fun reset() = ytm_auth.reset()
            override fun PlatformPreferences.Editor.save() = with (ytm_auth) { save() }
            override fun getDefault(defaultProvider: (String) -> Any): Boolean =
                defaultProvider(YoutubeAuthSettings.Key.YTM_AUTH.getName()) is YoutubeiAuthenticationState

            @Composable
            override fun onChanged(key: Any?, action: (Boolean) -> Unit) {}
        },
        enabledContent = { modifier ->
//            val auth_value: Set<String> = ytm_auth.get()

//            val data: Pair<Artist?, Headers> = remember(auth_value) {
//                YoutubeApi.UserAuthState.unpackSetData(auth_value, context)
//            }
//            if (data.first != null) {
//                own_channel = data.first
//            }

            val data: Pair<String?, Headers> = ApiAuthenticationState.unpackSetData(ytm_auth.get(), context)
            if (data.first != null) {
                own_channel = ArtistRef(data.first!!)
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
            login_page.LoginConfirmationDialog(
                info_only = false,
                manual_only = !isWebViewLoginSupported()
            ) { param ->
                dismiss()
                if (param != null) {
                    openPage(PrefsPageScreen.YOUTUBE_MUSIC_LOGIN.ordinal, param)
                }
            }
        },
        infoButton = { enabled, show_extra_state ->
            var show_extra: Boolean by show_extra_state
            var show_info_dialog: Boolean by remember(enabled) { mutableStateOf(false) }

            if (show_info_dialog) {
                login_page.LoginConfirmationDialog(info_only = true, manual_only = false) {
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
                    containerColor = if (enabled) context.theme.background else context.theme.vibrant_accent,
                    contentColor = if (enabled) context.theme.on_background else context.theme.on_accent
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
            initialise(Settings.prefs, Settings::provideDefault)
        }
    }
}
