package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.item.LargeToggleSettingsItem
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.DiscordAccountPreview
import com.toasterofbread.spmp.ui.layout.DiscordLoginConfirmation
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.auth_not_signed_in
import spmp.shared.generated.resources.s_discord_status_disabled
import spmp.shared.generated.resources.auth_sign_in
import spmp.shared.generated.resources.s_discord_status_enable
import spmp.shared.generated.resources.auth_sign_out
import spmp.shared.generated.resources.s_discord_status_disable

fun getDiscordAuthItem(
    context: AppContext,
    info_only: Boolean = false,
    ignore_prerequisite: Boolean = false,
    StartIcon: (@Composable () -> Unit)? = null
): LargeToggleSettingsItem {
    val discord_auth: PreferencesProperty<String> = context.settings.discord_auth.DISCORD_ACCOUNT_TOKEN

    val login_required: Boolean = DiscordStatus.isAccountTokenRequired()
    val prerequisite: PreferencesProperty<Boolean>? =
        if (login_required)
            context.settings.discord_auth.DISCORD_WARNING_ACCEPTED
        else null

    return LargeToggleSettingsItem(
        object : PreferencesProperty<Boolean> {
            override val key: String get() = throw IllegalStateException()
            @Composable
            override fun getName(): String = ""
            @Composable
            override fun getDescription(): String = ""

            override suspend fun get(): Boolean = discord_auth.get().isNotEmpty()

            override fun set(value: Boolean, editor: PlatformPreferences.Editor?) {
                if (!value) {
                    discord_auth.set("", editor)
                }
            }

            override fun set(data: JsonElement, editor: PlatformPreferences.Editor?) {
                set(data.jsonPrimitive.boolean, editor)
            }

            override fun serialise(value: Any?): JsonElement =
                JsonPrimitive(value as Boolean?)

            override fun reset() = discord_auth.reset()

            override suspend fun getDefaultValue(): Boolean =
                discord_auth.getDefaultValue().isNotEmpty()

            @Composable
            override fun getDefaultValueComposable(): Boolean =
                discord_auth.getDefaultValueComposable().isNotEmpty()

            @Composable
            override fun observe(): MutableState<Boolean> {
                val auth: String by discord_auth.observe()

                val state: MutableState<Boolean> = remember { mutableStateOf(auth.isNotEmpty()) }
                LaunchedEffect(auth.isNotEmpty()) {
                    state.value = auth.isNotEmpty()
                }

                return state
            }
        },
        show_button = !info_only,
        enabledContent = { modifier ->
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StartIcon?.invoke()

                val auth: String by discord_auth.observe()
                var current_account_token: String by remember { mutableStateOf(auth) }

                LaunchedEffect(auth) {
                    if (auth.isNotEmpty()) {
                        current_account_token = auth
                    }
                }

                if (current_account_token.isNotEmpty()) {
                    DiscordAccountPreview(current_account_token)
                }
            }
        },
        disabledContent = {
            StartIcon?.invoke()
        },
        disabled_text = if (login_required) Res.string.auth_not_signed_in else Res.string.s_discord_status_disabled,
        enable_button = if (login_required) Res.string.auth_sign_in else Res.string.s_discord_status_enable,
        disable_button = if (login_required) Res.string.auth_sign_out else Res.string.s_discord_status_disable,
        warningDialog =
            if (login_required) {{ dismiss, openPage ->
                DiscordLoginConfirmation { manual ->
                    dismiss()
                    if (manual != null) {
                        openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal, manual)
                    }
                }
            }}
            else null,
        infoButton = { enabled, _ ->
            if (info_only || !login_required) {
                return@LargeToggleSettingsItem
            }

            val player: PlayerState = LocalPlayerState.current
            var show_info_dialog: Boolean by remember { mutableStateOf(false) }

            if (show_info_dialog) {
                DiscordLoginConfirmation(true) {
                    show_info_dialog = false
                }
            }

            ShapedIconButton(
                { show_info_dialog = !show_info_dialog },
                shape = CircleShape,
                colours = IconButtonDefaults.iconButtonColors(
                    containerColor = if (enabled) player.theme.background else player.theme.vibrant_accent,
                    contentColor = if (enabled) player.theme.on_background else player.theme.on_accent
                )
            ) {
                Icon(
                    if (enabled) Icons.Default.Settings
                    else Icons.Default.Info,
                    null
                )
            }
        },
        prerequisite_value = if (ignore_prerequisite) null else prerequisite
    ) { target, setEnabled, _, openPage ->
        if (target) {
            if (login_required) {
                openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal, null)
            }
            else {
                discord_auth.set("0")
            }
        }
        else {
            setEnabled(false)
        }
    }
}
