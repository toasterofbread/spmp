package com.toasterofbread.spmp.ui.layout

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.api.*
import com.toasterofbread.spmp.platform.WebViewLogin
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.composable.rememberImagePainter
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.composable.LinkifyText
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private const val DISCORD_LOGIN_URL = "https://discord.com/login"
private const val DISCORD_API_URL = "https://discord.com/api/"
private const val DISCORD_DEFAULT_AVATAR = "https://discord.com/assets/1f0bfc0865d324c2587920a7d80c609b.png"

@Composable
fun DiscordLoginConfirmation(info_only: Boolean = false, onFinished: (manual: Boolean?) -> Unit) {
    PlatformAlertDialog(
        { onFinished(false) },
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
                LinkifyText(getString(if (info_only) "info_discord_login" else "warning_discord_login"))
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
fun DiscordLogin(modifier: Modifier = Modifier, manual: Boolean = false, onFinished: (Result<String?>?) -> Unit) {
    if (manual) {
        DiscordManualLogin(modifier, onFinished)
    }
    else if (isWebViewLoginSupported()) {
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
        LaunchedEffect(Unit) {
            SpMp.context.openUrl(DISCORD_LOGIN_URL)
        }
        DiscordManualLogin(modifier, onFinished)
    }
}

data class DiscordMeResponse(
    val id: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val discriminator: String? = null,
    val banner_color: String? = null,
    val bio: String? = null
) {
    var token: String? = null

    fun isEmpty(): Boolean = this == EMPTY
    fun getAvatarUrl(): String {
        check(!isEmpty())
        check(id != null)

        return if (avatar != null) "https://cdn.discordapp.com/avatars/$id/$avatar.webp"
                else DISCORD_DEFAULT_AVATAR
    }

    companion object {
        val EMPTY = DiscordMeResponse()
    }
}

suspend fun getDiscordAccountInfo(account_token: String): Result<DiscordMeResponse> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("https://discord.com/api/v9/users/@me")
        .addHeader("authorization", account_token)
        .build()

    val result = Api.request(request, is_gzip = false)
    val response = result.getOrNull() ?: return@withContext result.cast()

    val stream = response.body!!.charStream()
    val me: DiscordMeResponse = try {
        Klaxon().parse(stream)!!
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream.close()
    }
    me.token = account_token

    return@withContext Result.success(me)
}

private val DiscordMeResponseSaver = run {
    mapSaver(
        save = {
            if (it.isEmpty()) emptyMap()
            else with (it) { mapOf(
                "id" to id,
                "username" to username,
                "avatar" to avatar,
                "discriminator" to discriminator,
                "banner_color" to banner_color,
                "bio" to bio,
                "token" to token
            )}
        },
        restore = { Klaxon().parseFromJsonObject<DiscordMeResponse>(JsonObject(it))?.apply { token = it["token"] as String? } }
    )
}

@Composable
fun DiscordAccountPreview(account_token: String, modifier: Modifier = Modifier) {
    var account_info by rememberSaveable(stateSaver = DiscordMeResponseSaver) { mutableStateOf(DiscordMeResponse.EMPTY) }
    var started by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(account_token) {
        if (account_info.token != account_token) {
            account_info = DiscordMeResponse.EMPTY
            loading = true
            started = true
            account_info = getDiscordAccountInfo(account_token).getOrReport("DiscordAccountPreview") ?: DiscordMeResponse.EMPTY
        }
        loading = false
    }

    Crossfade(if (!account_info.isEmpty()) account_info else if (started) loading else null, modifier.fillMaxHeight()) { state ->
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
                    Image(rememberImagePainter(state.getAvatarUrl()), null, Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape))

                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                        Text(state.username ?: "?", overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }
            }
        }
    }
}
