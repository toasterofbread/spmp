package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import SpMp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.DiscordAccountPreview
import com.toasterofbread.spmp.ui.layout.DiscordLoginConfirmation
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsEditor
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.LargeToggleSettingsItem
import dev.toastbits.composekit.theme.core.onAccent
import dev.toastbits.composekit.theme.core.vibrantAccent
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.auth_not_signed_in
import spmp.shared.generated.resources.auth_sign_in
import spmp.shared.generated.resources.auth_sign_out
import spmp.shared.generated.resources.s_discord_status_disable
import spmp.shared.generated.resources.s_discord_status_disabled
import spmp.shared.generated.resources.s_discord_status_enable

fun getDiscordAuthItem(
    context: AppContext,
    info_only: Boolean = false,
    ignore_prerequisite: Boolean = false,
    StartIcon: (@Composable () -> Unit)? = null
): LargeToggleSettingsItem {
    val discord_auth: PlatformSettingsProperty<String> = context.settings.DiscordAuth.DISCORD_ACCOUNT_TOKEN

    val login_required: Boolean = DiscordStatus.isAccountTokenRequired()
    val prerequisite: PlatformSettingsProperty<Boolean>? =
        if (login_required)
            context.settings.DiscordAuth.DISCORD_WARNING_ACCEPTED
        else null

    return LargeToggleSettingsItem(
        object : PlatformSettingsProperty<Boolean> {
            override val key: String get() = throw IllegalStateException()
            @Composable
            override fun getName(): String = ""
            @Composable
            override fun getDescription(): String = ""

            override fun get(): Boolean = discord_auth.get().isNotEmpty()

            override suspend fun set(value: Boolean, editor: PlatformSettingsEditor?) {
                if (!value) {
                    discord_auth.set("", editor)
                }
            }

            override suspend fun set(data: JsonElement, editor: PlatformSettingsEditor?) {
                set(data.jsonPrimitive.boolean, editor)
            }

            override fun serialise(value: Any?): JsonElement =
                JsonPrimitive(value as Boolean?)

            override suspend fun reset(editor: PlatformSettingsEditor?) {
                discord_auth.reset(editor)
            }

            override fun getDefaultValue(): Boolean =
                discord_auth.getDefaultValue().isNotEmpty()

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
            if (login_required) {{ dismiss ->
                DiscordLoginConfirmation { manual ->
                    dismiss()
                    if (manual != null) {
                        SpMp.player_state.app_page_state.Settings.openScreen(
                            DiscordLoginScreen(
                                context.settings.DiscordAuth.DISCORD_ACCOUNT_TOKEN,
                                manual
                            )
                        )
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
                    containerColor = if (enabled) player.theme.background else player.theme.vibrantAccent,
                    contentColor = if (enabled) player.theme.onBackground else player.theme.onAccent
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
    ) { target, setEnabled, _ ->
        if (target) {
            if (login_required) {
                SpMp.player_state.app_page_state.Settings.openScreen(
                    DiscordLoginScreen(
                        context.settings.DiscordAuth.DISCORD_ACCOUNT_TOKEN,
                        false
                    )
                )
            }
            else {
                context.coroutineScope.launch {
                    discord_auth.set("0")
                }
            }
        }
        else {
            setEnabled(false)
        }
    }
}
