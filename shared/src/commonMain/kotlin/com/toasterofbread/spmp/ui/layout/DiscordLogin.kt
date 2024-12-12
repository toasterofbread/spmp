package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.toasterofbread.spmp.platform.DiscordMeResponse
import com.toasterofbread.spmp.platform.WebViewLogin
import com.toasterofbread.spmp.platform.getDiscordAccountInfo
import com.toasterofbread.spmp.platform.getOrNotify
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.components.utils.composable.LinkifyText
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_confirm_action
import spmp.shared.generated.resources.action_deny_action
import spmp.shared.generated.resources.action_login_manually
import spmp.shared.generated.resources.info_discord_login
import spmp.shared.generated.resources.prompt_confirm_action
import spmp.shared.generated.resources.warning_discord_login

private const val DISCORD_LOGIN_URL: String = "https://discord.com/login"
private const val DISCORD_API_URL: String = "https://discord.com/api/"

@Composable
fun DiscordLoginConfirmation(info_only: Boolean = false, onFinished: (manual: Boolean?) -> Unit) {
    val player: PlayerState = LocalPlayerState.current

    AlertDialog(
        { onFinished(null) },
        confirmButton = {
            FilledTonalButton({
                onFinished(if (info_only) null else false)
            }) {
                Text(stringResource(Res.string.action_confirm_action))
            }
        },
        dismissButton = if (info_only) null else ({
            TextButton({ onFinished(null) }) { Text(stringResource(Res.string.action_deny_action)) }
        }),
        title = if (info_only) null else ({ Text(stringResource(Res.string.prompt_confirm_action)) }),
        text = {
            Column {
                LinkifyText(
                    stringResource(if (info_only) Res.string.info_discord_login else Res.string.warning_discord_login),
                    player.theme.accent
                )
                if (!info_only) {
                    FilledTonalButton({ onFinished(true) }, Modifier.fillMaxWidth().padding(top = 5.dp).offset(y = 20.dp)) {
                        Text(stringResource(Res.string.action_login_manually))
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
                .thenIf(Platform.ANDROID.isCurrent()) {
                    padding(
                        top = content_padding.calculateTopPadding() - 20.dp,
                        bottom = content_padding.calculateBottomPadding() - 20.dp
                    )
                },
            onClosed = { onFinished(null) },
            shouldShowPage = { it.startsWith(DISCORD_LOGIN_URL) },
            user_agent = "Mozilla/5.0 (X11; Linux x86_64; rv:126.0) Gecko/20100101 Firefox/126.0",
            viewport_width = "1280px",
            viewport_height = "720px"
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
    Saver(
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
                    AsyncImage(
                        state.getAvatarUrl(),
                        null,
                        Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape),
                        onState = {
                            if (it is AsyncImagePainter.State.Error) {
                                RuntimeException(it.result.throwable).printStackTrace()
                            }
                        }
                    )

                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                        Text(state.username ?: "?", overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }
            }
        }
    }
}
