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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.settings.ui.item.ComposableSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.LargeToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.unpackSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getYoutubeAccountCategory
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.ui.component.NotImplementedMessage
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.LoginPage
import io.ktor.http.Headers
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.auth_not_signed_in
import spmp.shared.generated.resources.auth_sign_in
import spmp.shared.generated.resources.auth_sign_out

fun getYtmAuthItem(context: AppContext, ytm_auth: PreferencesProperty<Set<String>>): SettingsItem {
    var own_channel: Artist? by mutableStateOf(null)
    val login_page: LoginPage = context.ytapi.LoginPage

    if (!login_page.isImplemented()) {
        return ComposableSettingsItem {
            login_page.NotImplementedMessage(Modifier.fillMaxSize())
        }
    }

    return LargeToggleSettingsItem(
        object : PreferencesProperty<Boolean> {
            override val key: String = ytm_auth.key
            @Composable
            override fun getName(): String = ytm_auth.getName()
            @Composable
            override fun getDescription(): String? = ytm_auth.getDescription()

            override suspend fun get(): Boolean =
                ytm_auth.get().isNotEmpty()

            override fun set(value: Boolean, editor: PlatformPreferences.Editor?) {
                if (!value) {
                    ytm_auth.set(emptySet(), editor)
                }
            }

            override fun set(data: JsonElement, editor: PlatformPreferences.Editor?) {
                set(data.jsonPrimitive.boolean, editor)
            }

            override fun serialise(value: Any?): JsonElement =
                JsonPrimitive(value as Boolean?)

            override suspend fun getDefaultValue(): Boolean =
                ytm_auth.getDefaultValue().isNotEmpty()

            @Composable
            override fun getDefaultValueComposable(): Boolean =
                ytm_auth.getDefaultValueComposable().isNotEmpty()

            @Composable
            override fun observe(): MutableState<Boolean> {
                val auth: Set<String> by ytm_auth.observe()

                val state: MutableState<Boolean> = remember { mutableStateOf(auth.isNotEmpty()) }
                LaunchedEffect(auth.isNotEmpty()) {
                    state.value = auth.isNotEmpty()
                }

                return state
            }

            override fun reset() =
                ytm_auth.reset()
        },
        enabledContent = { modifier ->
//            val auth_value: Set<String> = ytm_auth.get()

//            val data: Pair<Artist?, Headers> = remember(auth_value) {
//                YoutubeApi.UserAuthState.unpackSetData(auth_value, context)
//            }
//            if (data.first != null) {
//                own_channel = data.first
//            }

            val auth: Set<String> by ytm_auth.observe()
            val data: Pair<String?, Headers> = ApiAuthenticationState.unpackSetData(auth, context)
            if (data.first != null) {
                own_channel = ArtistRef(data.first!!)
            }

            own_channel?.also { channel ->
                MediaItemPreviewLong(channel, modifier, show_type = false)
            }
        },
        extra_items = getYoutubeAccountCategory(context),
        disabled_text = Res.string.auth_not_signed_in,
        enable_button = Res.string.auth_sign_in,
        disable_button = Res.string.auth_sign_out,
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
    }
}
