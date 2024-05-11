package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.utils.composable.LinkifyText
import dev.toastbits.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.platform.DiscordMeResponse
import com.toasterofbread.spmp.platform.WebViewLogin
import com.toasterofbread.spmp.platform.getDiscordAccountInfo
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.foundation.layout.PaddingValues
import com.toasterofbread.spmp.platform.getOrNotify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.encodeToString
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

private const val DISCORD_LOGIN_URL: String = "https://discord.com/login"
private const val DISCORD_API_URL: String = "https://discord.com/api/"

@Composable
fun DiscordLoginConfirmation(info_only: Boolean = false, onFinished: (manual: Boolean?) -> Unit) {
    AlertDialog(
        { onFinished(null) },
        confirmButton = {
            FilledTonalButton({
                onFinished(if (info_only) null else false)
            }) {
                Text(getString("action_confirm_action"))
            }
        },
        dismissButton = if (info_only) null else ({
            TextButton({ onFinished(null) }) { Text(getString("action_deny_action")) }
        }),
        title = if (info_only) null else ({ Text(getString("prompt_confirm_action")) }),
        text = {
            Column {
                LinkifyText(getString(if (info_only) "info_discord_login" else "warning_discord_login"), LocalPlayerState.current.theme.accent)
                if (!info_only) {
                    FilledTonalButton({ onFinished(true) }, Modifier.fillMaxWidth().padding(top = 5.dp).offset(y = 20.dp)) {
                        Text(getString("action_login_manually"))
                    }
                }
            }
        }
    )
}

@Composable
fun DiscordLogin(content_padding: PaddingValues, modifier: Modifier = Modifier, manual: Boolean = false, onFinished: (Result<String?>?) -> Unit) {
    val player: PlayerState = LocalPlayerState.current

    if (!manual && isWebViewLoginSupported()) {
        WebViewLogin(
            initial_url = DISCORD_LOGIN_URL,
            modifier = modifier
                .padding(
                    top = content_padding.calculateTopPadding() - 20.dp,
                    bottom = content_padding.calculateBottomPadding() - 20.dp
                ),
            onClosed = { onFinished(null) },
            shouldShowPage = { it.startsWith(DISCORD_LOGIN_URL) },
            user_agent = "Mozilla/5.0 (X11; Linux x86_64; rv:126.0) Gecko/20100101 Firefox/126.0",
        ) { request, openUrl, getCookies ->
            if (request.url.startsWith(DISCORD_API_URL)) {
                val auth = request.headers["Authorization"]
                if (auth != null) {
                    onFinished(Result.success(auth))
                }
            }
        }
    }
    else {
        // TODO
        LaunchedEffect(Unit) {
            player.context.openUrl(DISCORD_LOGIN_URL)
        }
        DiscordManualLogin(content_padding, modifier, onFinished)
    }
}

private val DiscordMeResponseSaver: Saver<DiscordMeResponse?, String> =
    Saver<DiscordMeResponse?, String>(
        save = { it: DiscordMeResponse? ->
            it?.let { Json.encodeToString(it) }
        },
        restore = { data: String ->
            Json.decodeFromString(data)
        }
    )

@Composable
fun DiscordAccountPreview(account_token: String, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    var account_info: DiscordMeResponse? by rememberSaveable(stateSaver = DiscordMeResponseSaver) { mutableStateOf(DiscordMeResponse.EMPTY) }
    var started: Boolean by remember { mutableStateOf(false) }
    var loading: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(account_token) {
        if (account_info?.token != account_token) {
            account_info = DiscordMeResponse.EMPTY
            loading = true
            started = true
            account_info = getDiscordAccountInfo(account_token).getOrNotify(player.context, "DiscordAccountPreview") ?: DiscordMeResponse.EMPTY
        }
        loading = false
    }

    Crossfade(if (account_info?.isEmpty() == false) account_info else if (started) loading else null, modifier.fillMaxHeight()) { state ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            when (state) {
                true -> {
                    SubtleLoadingIndicator(Modifier.fillMaxSize())
                }
                false -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Error, null)
                    }
                }
                is DiscordMeResponse -> {
                    KamelImage(asyncPainterResource(state.getAvatarUrl()), null, Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape))

                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                        Text(state.username ?: "?", overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }
            }
        }
    }
}
