package com.spectre7.spmp.ui.layout

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.platform.PlatformAlertDialog
import com.spectre7.spmp.platform.WebViewLogin
import com.spectre7.spmp.platform.isWebViewLoginSupported
import com.spectre7.utils.LinkifyText
import com.spectre7.utils.getString

private const val DISCORD_LOGIN_URL = "https://discord.com/login"
private const val DISCORD_API_URL = "https://discord.com/api/"

@Composable
fun DiscordLoginWarningPopup(info: Boolean = false, onFinished: (accepted: Boolean) -> Unit) {
    PlatformAlertDialog(
        { onFinished(false) },
        confirmButton = {
            FilledTonalButton({
                onFinished(!info)
            }) {
                Text(getString("action_confirm_action"))
            }
        },
        dismissButton = if (!info) ({ TextButton({ onFinished(false) }) { Text(getString("action_deny_action")) } }) else null,
        title = { Text(getString("prompt_confirm_action")) },
        text = {
            LinkifyText(getString(if (info) "info_discord_login" else "warning_discord_login"))
        }
    )
}

@Composable
fun DiscordLogin(modifier: Modifier = Modifier, onFinished: (Result<String?>?) -> Unit) {
    if (isWebViewLoginSupported()) {
        WebViewLogin(DISCORD_LOGIN_URL, modifier, { it.startsWith(DISCORD_LOGIN_URL) }) { request, openUrl, getCookie ->
            if (request.url.startsWith(DISCORD_API_URL)) {
                val auth = request.requestHeaders["Authorization"]
                if (auth != null) {
                    onFinished(Result.success(auth))
                }
            }
        }
    }
    else {
        // TODO
        SpMp.context.openUrl(DISCORD_LOGIN_URL)
    }
}
