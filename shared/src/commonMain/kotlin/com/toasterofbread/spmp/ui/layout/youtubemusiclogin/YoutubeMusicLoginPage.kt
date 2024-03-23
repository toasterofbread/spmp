package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import LocalPlayerState
import SpMp.isDebugBuild
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.composable.LinkifyText
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.AccountSwitcherEndpoint
import com.toasterofbread.spmp.youtubeapi.YTMLogin
import com.toasterofbread.spmp.youtubeapi.SpMpYoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeChannelNotCreatedException
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.external.YoutubeAccountCreationForm
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.util.StringValues
import io.ktor.util.filter
import io.ktor.util.flattenEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val MUSIC_LOGIN_URL: String =
    "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"

class YoutubeMusicLoginPage(val api: YoutubeiApi): LoginPage() {
    @Composable
    override fun LoginPage(
        modifier: Modifier,
        confirm_param: Any?,
        content_padding: PaddingValues,
        onFinished: (Result<ApiAuthenticationState>?) -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()
        val manual: Boolean = confirm_param == true

        var channel_not_created_error: YoutubeChannelNotCreatedException? by remember { mutableStateOf(null) }
        var channel_creation_form: Result<YoutubeAccountCreationForm.ChannelCreationForm>? by remember { mutableStateOf(null) }
        var account_selection_data: AccountSelectionData? by remember { mutableStateOf(null) }

        LaunchedEffect(channel_not_created_error) {
            val error:YoutubeChannelNotCreatedException = channel_not_created_error ?: return@LaunchedEffect
            val channel_creation_token: String? = error.channel_creation_token

            if (channel_creation_token != null) {
                channel_creation_form = null
                channel_creation_form = api.YoutubeChannelCreationForm.getForm(error.headers, channel_creation_token)
            }
            else {
                channel_creation_form = Result.failure(RuntimeException(getStringTODO("No channel creation token"), error))
            }
        }

        channel_creation_form?.also {
            it.onSuccess { form ->
                val channel_error = channel_not_created_error!!

                YoutubeChannelCreateDialog(
                    channel_error.headers,
                    form,
                    api
                ) { result ->
                    if (result == null) {
                        onFinished(null)
                        return@YoutubeChannelCreateDialog
                    }

                    result.fold(
                        { channel_id ->
                            onFinished(
                                Result.success(
                                    SpMpYoutubeiAuthenticationState(
                                        player.context.database,
                                        api,
                                        channel_id,
                                        channel_error.headers
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

        suspend fun YtmApi.onHeadersProvided(provided_headers: Headers) {
            val headers: StringValues = provided_headers.filter { key, value ->
                YoutubeiAuthenticationState.INCLUDED_HEADERS.contains(key.lowercase())
            }

            val switcher_response: HttpResponse =
                HttpClient(CIO).get("https://music.youtube.com/getAccountSwitcherEndpoint") {
                    expectSuccess = true
                    headers {
                        appendAll(headers)
                    }
                }

            val new_cookies: List<String> = switcher_response.headers.flattenEntries().mapNotNull { header ->
                if (header.first.lowercase() == "set-cookie") header.second
                else null
            }

            val new_headers: Headers = Headers.build {
                set("Cookie", YTMLogin.replaceCookiesInString(headers["Cookie"]!!, new_cookies))
            }

            val response_body: String = switcher_response.bodyAsText().let { body ->
                body.substring(
                    body.indexOf('\n') + 1
                )
            }

            val parsed: AccountSwitcherEndpoint
            try {
                parsed = Json { ignoreUnknownKeys = true }.decodeFromString(response_body)
            }
            catch (e: Throwable) {
                onFinished(Result.failure(
                    RuntimeException("Account switcher response parsing failed $response_body ", e)
                ))
                return
            }

            val accounts = parsed.getAccounts().filter { it.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens.any { it.accountSigninToken != null } }
            if (accounts.size > 1) {
                account_selection_data = AccountSelectionData(accounts, new_headers)
                return
            }

            coroutine_scope.launch {
                val result = YTMLogin.completeLogin(player.context, new_headers, api)
                result?.onFailure { error ->
                    if (error is YoutubeChannelNotCreatedException) {
                        channel_not_created_error = error
                        return@launch
                    }
                }

                onFinished(result)
            }
        }

        Crossfade(account_selection_data ?: channel_creation_form ?: if (channel_not_created_error != null) null else true, modifier) { state ->
            if (state is AccountSelectionData) {
                val coroutine_scope: CoroutineScope = rememberCoroutineScope()
                AccountSelectionPage(
                    state,
                    Modifier.fillMaxWidth().padding(content_padding)
                ) { account ->
                    coroutine_scope.launch(Dispatchers.IO) {
                        account_selection_data = null
                        onFinished(YTMLogin.completeLoginWithAccount(player.context, state.headers, account, api))
                    }
                }
            }
            else if (state is Result<*>) {
                val error = state.exceptionOrNull()
                if (error != null) {
                    ErrorInfoDisplay(
                        error,
                        isDebugBuild(),
                        Modifier.fillMaxWidth().padding(content_padding),
                        expanded_content_modifier = Modifier.fillMaxHeight(),
                        onDismiss = null
                    )
                }
            }
            else if (state == null) {
                Box(Modifier.fillMaxSize().padding(content_padding), contentAlignment = Alignment.Center) {
                    SubtleLoadingIndicator(message = getString("youtube_channel_creation_load_message"))
                }
            }
            else if (!manual && isWebViewLoginSupported()) {
                YoutubeMusicWebviewLogin(api, MUSIC_LOGIN_URL, Modifier.fillMaxSize()) { result ->
                    if (result == null) {
                        onFinished(null)
                        return@YoutubeMusicWebviewLogin
                    }

                    result.fold(
                        {
                            coroutine_scope.launch { with (api) {
                                onHeadersProvided(it)
                            }}
                        },
                        { onFinished(Result.failure(it)) }
                    )
                }
            }
            else {
                YoutubeMusicManualLogin(MUSIC_LOGIN_URL, content_padding, Modifier.fillMaxSize()) { result ->
                    if (result == null) {
                        onFinished(null)
                        return@YoutubeMusicManualLogin
                    }

                    result.fold(
                        {
                            coroutine_scope.launch { with (api) {
                                onHeadersProvided(it)
                            }}
                        },
                        { onFinished(Result.failure(it)) }
                    )
                }
            }
        }
    }

    @Composable
    override fun LoginConfirmationDialog(info_only: Boolean, manual_only: Boolean, onFinished: (param: Any?) -> Unit) {
        val player: PlayerState = LocalPlayerState.current

        AlertDialog(
            { onFinished(null) },
            confirmButton = {
                FilledTonalButton({
                    onFinished(if (info_only) null else if (manual_only) true else false)
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
                    LinkifyText(getString(if (info_only) "info_ytm_login" else "warning_ytm_login"), player.theme.accent)
                    if (!info_only && !manual_only) {
                        FilledTonalButton({ onFinished(true) }, Modifier.fillMaxWidth().padding(top = 5.dp).offset(y = 20.dp)) {
                            Text(getString("action_login_manually"))
                        }
                    }
                }
            }
        )
    }

    override fun getTitle(confirm_param: Any?): String? {
        return if (confirm_param == true) getString("youtube_manual_login_title") else null
    }

    override fun getIcon(confirm_param: Any?): ImageVector? {
        return if (confirm_param == true) Icons.Default.PlayCircle else null
    }

    override fun targetsDisabledPadding(confirm_param: Any?): Boolean = false
}
