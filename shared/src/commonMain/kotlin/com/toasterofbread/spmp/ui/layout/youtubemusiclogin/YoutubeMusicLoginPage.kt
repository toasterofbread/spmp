package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import LocalPlayerState
import PlatformIO
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.AccountSwitcherEndpoint
import com.toasterofbread.spmp.youtubeapi.YTMLogin
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.components.utils.composable.LinkifyText
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import dev.toastbits.ytmkt.model.YtmApi
import io.ktor.client.HttpClient
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
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_confirm_action
import spmp.shared.generated.resources.action_deny_action
import spmp.shared.generated.resources.action_login_manually
import spmp.shared.generated.resources.info_ytm_login
import spmp.shared.generated.resources.prompt_confirm_action
import spmp.shared.generated.resources.warning_ytm_login
import spmp.shared.generated.resources.youtube_manual_login_title

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

        var account_selection_data: AccountSelectionData? by remember { mutableStateOf(null) }

        suspend fun YtmApi.onHeadersProvided(provided_headers: Headers, account_selected: Boolean = false) {
            val final_headers: Headers
            val headers: StringValues = provided_headers.filter { key, value ->
                YoutubeiAuthenticationState.INCLUDED_HEADERS.contains(key.lowercase())
            }

            if (!account_selected) {
                val switcher_response: HttpResponse =
                    HttpClient().get("https://music.youtube.com/getAccountSwitcherEndpoint") {
                        expectSuccess = true
                        headers {
                            appendAll(headers)
                        }
                    }

                val new_cookies: List<String> = switcher_response.headers.flattenEntries().mapNotNull { header ->
                    if (header.first.lowercase() == "set-cookie") header.second
                    else null
                }

                final_headers = Headers.build {
                    appendAll(headers)
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

                val accounts = parsed.getAccounts().filter { it.serviceEndpoint.selectActiveIdentityEndpoint?.supportedTokens?.any { it.accountSigninToken != null } == true }
                if (accounts.size > 1) {
                    account_selection_data = AccountSelectionData(accounts, final_headers)
                    return
                }
            }
            else {
                final_headers = Headers.build { appendAll(headers) }
            }

            coroutine_scope.launch {
                onFinished(YTMLogin.completeLogin(player.context, final_headers, api))
            }
        }

        Crossfade(account_selection_data, modifier) { state ->
            if (state is AccountSelectionData) {
                AccountSelectionPage(
                    state,
                    Modifier.fillMaxWidth().padding(content_padding)
                ) { account ->
                    coroutine_scope.launch(Dispatchers.PlatformIO) {
                        account_selection_data = null
                        onFinished(YTMLogin.completeLoginWithAccount(player.context, state.headers, account, api))
                    }
                }
            }
            else if (!manual && isWebViewLoginSupported()) {
                YoutubeMusicWebviewLogin(
                    api,
                    MUSIC_LOGIN_URL,
                    Modifier
                        .fillMaxSize()
                        .thenIf(Platform.ANDROID.isCurrent()) {
                            val v_padding: Dp = 30.dp
                            padding(
                                top = (content_padding.calculateTopPadding() - v_padding).coerceAtLeast(0.dp),
                                bottom = (content_padding.calculateBottomPadding() - v_padding).coerceAtLeast(0.dp)
                            )
                        }
                    ) { result ->
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
                                onHeadersProvided(it, account_selected = true)
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
                    Text(stringResource(Res.string.action_confirm_action))
                }
            },
            dismissButton = if (info_only) null else ({
                TextButton({ onFinished(null) }) { Text(stringResource(Res.string.action_deny_action)) }
            }),
            title = if (info_only) null else ({ Text(stringResource(Res.string.prompt_confirm_action)) }),
            text = {
                Column {
                    LinkifyText(stringResource(if (info_only) Res.string.info_ytm_login else Res.string.warning_ytm_login), player.theme.accent)
                    if (!info_only && !manual_only) {
                        FilledTonalButton({ onFinished(true) }, Modifier.fillMaxWidth().padding(top = 5.dp).offset(y = 20.dp)) {
                            Text(stringResource(Res.string.action_login_manually))
                        }
                    }
                }
            }
        )
    }

    @Composable
    override fun getTitle(confirm_param: Any?): String? =
        if (confirm_param == true) stringResource(Res.string.youtube_manual_login_title) else null

    override fun getIcon(confirm_param: Any?): ImageVector? =
        if (confirm_param == true) Icons.Default.PlayCircle else null

    override fun targetsDisabledPadding(confirm_param: Any?): Boolean = false
}
