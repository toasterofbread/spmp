package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.YoutubeAccountCreationForm
import com.toasterofbread.spmp.api.getYoutubeAccountCreationForm
import com.toasterofbread.spmp.model.YoutubeChannelNotCreatedException
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.ArtistRef
import com.toasterofbread.spmp.platform.WebViewLogin
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.getDefaultPaddingValues
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.utils.composable.LinkifyText
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.Request
import java.net.URI

private const val MUSIC_URL = "https://music.youtube.com/"
private const val MUSIC_LOGIN_URL = "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"

@Composable
fun YoutubeMusicLoginConfirmation(info_only: Boolean = false, onFinished: (manual: Boolean?) -> Unit) {
    PlatformAlertDialog(
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
                LinkifyText(getString(if (info_only) "info_ytm_login" else "warning_ytm_login"))
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
fun YoutubeMusicLogin(modifier: Modifier = Modifier, manual: Boolean = false, onFinished: (Result<YoutubeMusicAuthInfo>?) -> Unit) {
    var channel_not_created_error: YoutubeChannelNotCreatedException? by remember { mutableStateOf(null) }
    var channel_creation_form: Result<YoutubeAccountCreationForm.ChannelCreationForm>? by remember { mutableStateOf(null) }
    var account_selection_data: AccountSelectionData? by remember { mutableStateOf(null) }

    LaunchedEffect(channel_not_created_error) {
        val error = channel_not_created_error ?: return@LaunchedEffect
        if (error.channel_creation_token != null) {
            channel_creation_form = null
            channel_creation_form = getYoutubeAccountCreationForm(error.cookie, error.headers, error.channel_creation_token)
        }
        else {
            channel_creation_form = Result.failure(RuntimeException(getStringTODO("No channel creation token")))
        }
    }

    channel_creation_form?.also {
        it.onSuccess { form ->
            val chanel_error = channel_not_created_error!!

            YoutubeChannelCreateDialog(
                chanel_error.cookie,
                chanel_error.headers,
                form
            ) { result ->
                if (result == null) {
                    onFinished(null)
                    return@YoutubeChannelCreateDialog
                }

                result.fold(
                    { channel ->
                        onFinished(
                            Result.success(
                                YoutubeMusicAuthInfo(
                                    channel,
                                    chanel_error.cookie,
                                    chanel_error.headers
                                )
                            )
                        )
                    },
                    { error ->
                        onFinished(Result.failure(error))
                    }
                )
            }
        }
    }

    Crossfade(account_selection_data ?: channel_creation_form ?: if (channel_not_created_error != null) null else true, modifier) { state ->
        if (state is AccountSelectionData) {
            val coroutine_scope = rememberCoroutineScope()
            AccountSelectionPage(
                state,
                Modifier.fillMaxWidth().padding(SpMp.context.getDefaultPaddingValues())
            ) { account ->
                coroutine_scope.launch(Dispatchers.IO) {
                    account_selection_data = null
                    onFinished(completeLoginWithAccount(state.headers, account))
                }
            }
        }
        else if (state is Result<*>) {
            val error = state.exceptionOrNull()
            if (error != null) {
                ErrorInfoDisplay(error, Modifier.fillMaxWidth(), expanded_modifier = Modifier.fillMaxHeight())
            }
        }
        else if (state == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SubtleLoadingIndicator(message = getString("youtube_channel_creation_load_message"))
            }
        }
        else if (manual) {
            YoutubeMusicManualLogin(Modifier.fillMaxSize(), onFinished)
        }
        else if (isWebViewLoginSupported()) {
            var finished: Boolean by remember { mutableStateOf(false) }
            val lock = remember { Object() }

            WebViewLogin(
                MUSIC_URL,
                Modifier.fillMaxSize(),
                loading_message = getString("youtube_login_load_message"),
                shouldShowPage = { !it.startsWith(MUSIC_URL) },
                onClosed = { onFinished(null) }
            ) { request, openUrl, getCookie ->
                synchronized(lock) {
                    if (finished) {
                        return@WebViewLogin
                    }

                    val url = URI(request.url)
                    if (url.host == "music.youtube.com" && url.path?.startsWith("/youtubei/v1/") == true) {
                        if (!request.requestHeaders.containsKey("Authorization")) {
                            openUrl(MUSIC_LOGIN_URL)
                            return@WebViewLogin
                        }

                        finished = true

                        val cookie = getCookie(MUSIC_URL)
                        val headers = Headers.Builder()
                            .add("cookie", cookie)
                            .apply {
                                for (header in request.requestHeaders) {
                                    add(header.key, header.value)
                                }
                            }
                            .build()

                        val account_switcher_request = Request.Builder()
                            .url("https://music.youtube.com/getAccountSwitcherEndpoint")
                            .headers(headers)
                            .get()
                            .build()

                        val switcher_result = Api.request(account_switcher_request)
                        val response = switcher_result.fold(
                            { it },
                            { error ->
                                onFinished(Result.failure(error))
                                return@synchronized
                            }
                        )

                        try {
                            val response_body = response.body!!.string()
                            val parsed: AccountSwitcherEndpoint = Api.klaxon.parse(
                                response_body.substring(
                                    response_body.indexOf('\n') + 1
                                )
                            )!!

                            val accounts = parsed.getAccounts().filter { it.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens.any { it.accountSigninToken != null } }
                            if (accounts.size > 1) {
                                account_selection_data = AccountSelectionData(accounts, headers)
                                return@synchronized
                            }
                        }
                        catch (e: Throwable) {
                            onFinished(Result.failure(e))
                            return@synchronized
                        }
                        finally {
                            response.close()
                        }

                        val auth_result = completeLogin(headers)
                        auth_result.onFailure { error ->
                            if (error is YoutubeChannelNotCreatedException) {
                                channel_not_created_error = error
                                return@synchronized
                            }
                        }

                        onFinished(auth_result)
                    }
                }
            }
        }
        else {
            // TODO
            LaunchedEffect(Unit) {
                SpMp.context.openUrl(MUSIC_LOGIN_URL)
            }
            YoutubeMusicManualLogin(Modifier.fillMaxSize(), onFinished)
        }
    }
}

private fun completeLoginWithAccount(headers: Headers, account: AccountSwitcherEndpoint.AccountItem): Result<YoutubeMusicAuthInfo> {
    val account_headers: Headers

    if (!account.isSelected) {
        val sign_in_url: String =
            account.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens.first { it.accountSigninToken != null }.accountSigninToken!!.signinUrl

        val sign_in_request = Request.Builder()
            .url("https://music.youtube.com$sign_in_url")
            .headers(headers)
            .get()
            .build()

        val result = Api.request(sign_in_request)

        val new_cookies: List<String> = result.fold(
            {
                it.use { response ->
                    response.headers.mapNotNull { header ->
                        if (header.first == "Set-Cookie") header.second
                        else null
                    }
                }
            },
            {
                return Result.failure(it)
            }
        )

        var cookie_string = headers["Cookie"]!!

        for (cookie in new_cookies) {
            val split = cookie.split('=', limit = 2)

            val name: String = split[0]
            val new_value: String = split[1].split(';', limit = 2)[0]

            val cookie_start = cookie_string.indexOf("$name=") + name.length + 1
            if (cookie_start != -1) {
                val cookie_end = cookie_string.indexOf(';', cookie_start)
                cookie_string = (
                        cookie_string.substring(0, cookie_start)
                                + new_value
                                + if (cookie_end != -1) cookie_string.substring(cookie_end, cookie_string.length) else ""
                        )
            }
            else {
                cookie_string += "; $name=$new_value"
            }
        }

        account_headers = headers
            .newBuilder()
            .set("Cookie", cookie_string)
            .build()

        val channel_id: String? =
            account.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens.firstOrNull { it.offlineCacheKeyToken != null }?.offlineCacheKeyToken?.clientCacheKey

        if (channel_id != null) {
            return Result.success(
                YoutubeMusicAuthInfo(ArtistRef("UC$channel_id"), cookie_string, account_headers.toMap())
            )
        }
    }
    else {
        account_headers = headers
    }

    return completeLogin(account_headers)
}

private fun completeLogin(headers: Headers): Result<YoutubeMusicAuthInfo> {
    val account_request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/account/account_menu")
        .headers(headers)
        .post(Api.getYoutubeiRequestBody(null))
        .build()

    val result = Api.request(account_request)
    result.fold(
        { response ->
            val parsed: YTAccountMenuResponse = Api.klaxon.parse(response.body!!.charStream())!!
            response.close()

            return YoutubeMusicAuthInfo.fromYTAccountMenuResponse(parsed, headers["Cookie"]!!, headers.toMap())
        },
        {
            return Result.failure(it)
        }
    )
}
