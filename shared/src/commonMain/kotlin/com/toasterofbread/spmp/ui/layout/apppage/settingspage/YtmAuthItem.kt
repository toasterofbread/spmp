package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.settings.PlatformSettings
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ComposableSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.LargeToggleSettingsItem
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.settings.unpackSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getYoutubeAccountCategory
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.ui.component.NotImplementedMessage
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.LoginPage
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsEditor
import dev.toastbits.composekit.theme.core.onAccent
import dev.toastbits.composekit.theme.core.vibrantAccent
import io.ktor.http.Headers
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.JsonPrimitive
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.auth_not_signed_in
import spmp.shared.generated.resources.auth_sign_in
import spmp.shared.generated.resources.auth_sign_out

fun getYtmAuthItem(context: AppContext, ytmAuth: PlatformSettingsProperty<Set<String>>): SettingsItem {
    var own_channel: Artist? by mutableStateOf(null)
    val login_page: LoginPage = context.ytapi.LoginPage

    if (!login_page.isImplemented()) {
        return ComposableSettingsItem(resetComposeUiState = {}) {
            login_page.NotImplementedMessage(Modifier.fillMaxSize())
        }
    }

    return LargeToggleSettingsItem(
        object : PlatformSettingsProperty<Boolean> {
            override val key: String = ytmAuth.key
            @Composable
            override fun getName(): String = ytmAuth.getName()
            @Composable
            override fun getDescription(): String? = ytmAuth.getDescription()

            override fun get(): Boolean =
                ytmAuth.get().isNotEmpty()

            override suspend fun set(value: Boolean, editor: PlatformSettingsEditor?) {
                if (!value) {
                    ytmAuth.set(emptySet(), editor)
                }
            }

            override suspend fun set(data: JsonElement, editor: PlatformSettingsEditor?) {
                set(data.jsonPrimitive.boolean, editor)
            }

            override fun serialise(value: Any?): JsonElement =
                JsonPrimitive(value as Boolean?)

            override fun getDefaultValue(): Boolean =
                ytmAuth.getDefaultValue().isNotEmpty()

            @Composable
            override fun observe(): MutableState<Boolean> {
                val auth: Set<String> by ytmAuth.observe()

                val state: MutableState<Boolean> = remember { mutableStateOf(auth.isNotEmpty()) }
                LaunchedEffect(auth.isNotEmpty()) {
                    state.value = auth.isNotEmpty()
                }

                return state
            }

            override suspend fun reset(editor: PlatformSettingsEditor?) {
                ytmAuth.reset(editor)
            }
        },
        enabledContent = { modifier ->
//            val auth_value: Set<String> = ytm_auth.get()

//            val data: Pair<Artist?, Headers> = remember(auth_value) {
//                YoutubeApi.UserAuthState.unpackSetData(auth_value, context)
//            }
//            if (data.first != null) {
//                own_channel = data.first
//            }

            val auth: Set<String> by ytmAuth.observe()
            val data: Pair<String?, Headers>? = ApiAuthenticationState.unpackSetData(auth, context)
            if (data?.first != null) {
                own_channel = ArtistRef(data.first!!)
            }

            own_channel?.also { channel ->
                MediaItemPreviewLong(channel, modifier.requiredHeight(45.dp), show_type = false)
            }
        },
        extra_items = getYoutubeAccountCategory(context),
        disabled_text = Res.string.auth_not_signed_in,
        enable_button = Res.string.auth_sign_in,
        disable_button = Res.string.auth_sign_out,
        warningDialog = { dismiss ->
            login_page.LoginConfirmationDialog(
                info_only = false,
                manual_only = !isWebViewLoginSupported()
            ) { param ->
                dismiss()
                if (param != null) {
                    SpMp.player_state.app_page_state.Settings.openScreen(
                        YoutubeMusicLoginScreen(
                            context.settings.YoutubeAuth.YTM_AUTH,
                            param
                        )
                    )
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
                    containerColor = if (enabled) context.theme.background else context.theme.vibrantAccent,
                    contentColor = if (enabled) context.theme.onBackground else context.theme.onAccent
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
    ) { target, setEnabled, _ ->
        if (target) {
            SpMp.player_state.app_page_state.Settings.openScreen(
                YoutubeMusicLoginScreen(
                    context.settings.YoutubeAuth.YTM_AUTH,
                    null
                )
            )
        }
        else {
            setEnabled(false)
        }
    }
}
