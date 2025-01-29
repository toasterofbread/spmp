package com.toasterofbread.spmp.platform

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.WebContent
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.components.utils.composable.animatedvisibility.NullableValueAnimatedVisibility
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.webview_restart_required

actual fun isWebViewLoginSupported(): Boolean = true

@Composable
actual fun WebViewLogin(
    initial_url: String,
    onClosed: () -> Unit,
    shouldShowPage: (url: String) -> Boolean,
    modifier: Modifier,
    loading_message: String?,
    base_cookies: String,
    user_agent: String?,
    viewport_width: String?,
    viewport_height: String?,
    onRequestIntercepted: suspend (WebViewRequest, openUrl: (String) -> Unit, getCookies: suspend (String) -> List<Pair<String, String>>) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    var initialised: Boolean by remember { mutableStateOf(false) }
    var init_progress: Float? by remember { mutableStateOf(null) }
    var init_message: String? by remember { mutableStateOf(null) }
    var init_error: Throwable? by remember { mutableStateOf(null) }
    var restart_required: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        initWebViewLogin(player.context) { progress, message ->
            init_progress = progress
            init_message = message
        }.fold(
            { restart_required = it },
            { init_error = it }
        )
        initialised = true
    }

    init_error?.also { error ->
        Box(modifier, contentAlignment = Alignment.Center) {
            ErrorInfoDisplay(
                error,
                isDebugBuild(),
                onDismiss = onClosed
            )
        }
        return
    }

    if (!initialised) {
        Column(
            modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NullableValueAnimatedVisibility(init_message) { message ->
                if (message != null) {
                    Text(message)
                }
            }

            InitProgressIndicator(
                init_progress,
                colour = player.theme.accent,
                track_colour = player.theme.accent.copy(alpha = 0.5f)
            )
        }
        return
    }

    if (restart_required) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.webview_restart_required))
        }
        return
    }

    val state: WebViewState = remember {
        val content: WebContent = WebContent.Url(
            url = initial_url,
            additionalHttpHeaders = if (base_cookies.isNotBlank()) mapOf("Cookie" to base_cookies) else emptyMap()
        )
        WebViewState(content).also { state ->
            state.webSettings.apply {
                isJavaScriptEnabled = true
                customUserAgentString = user_agent

                androidWebSettings.apply {
                    isAlgorithmicDarkeningAllowed = true
                    useWideViewPort = true

                    if (viewport_width != null) {
                        viewportWidth = viewport_width
                    }
                    if (viewport_height != null) {
                        viewportHeight = viewport_height
                    }
                }
            }

            state.content = content
        }
    }

    val navigator: WebViewNavigator = remember {
        WebViewNavigator(
            coroutine_scope,
            urlRequestInterceptor = object : RequestInterceptor {
                override fun onInterceptRequest(request: WebRequest, navigator: WebViewNavigator): WebRequestInterceptResult {
                    return WebRequestInterceptResult.Allow
                }
            },
            resourceRequestInterceptor = object : RequestInterceptor {
                override fun onInterceptRequest(request: WebRequest, navigator: WebViewNavigator): WebRequestInterceptResult {
                    coroutine_scope.launch {
                        onRequestIntercepted(
                            request.toWebViewRequest(),
                            { url ->
                                navigator.loadUrl(url)
                            },
                            { url ->
                                val header_cookies: List<Pair<String, String>> =
                                    request.headers["Cookie"]?.split(';')?.map { cookie ->
                                        val split: List<String> = cookie.split('=', limit = 2)
                                        Pair(split[0].trim(), split[1].trim())
                                    }.orEmpty()

                                val state_cookies: List<Pair<String, String>> =
                                    state.cookieManager.getCookies(url).map { Pair(it.name, it.value) }

                                return@onRequestIntercepted header_cookies + state_cookies
                            }
                        )
                    }

                    if (Platform.DESKTOP.isCurrent()) {
                        val cookies: String = (request.headers["Cookie"]?.plus(";") ?: "") + base_cookies
                        return WebRequestInterceptResult.Modify(
                            request.copy(headers = request.headers.toMutableMap().also { it["Cookie"] = cookies })
                        )
                    }
                    else {
                        return WebRequestInterceptResult.Allow
                    }
                }
            }
        )
    }

    val show: Boolean by remember { derivedStateOf {
        state.lastLoadedUrl?.let { shouldShowPage(it) } ?: false
    } }

    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(!show, enter = fadeIn(), exit = fadeOut()) {
            SubtleLoadingIndicator(message = loading_message)
        }

        val minimised_now_playing_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp

        WebView(
            state = state,
            navigator = navigator,
            modifier =
                Modifier
                    .fillMaxSize()
                    .run {
                        if (!show) {
                            if (Platform.DESKTOP.isCurrent())
                                offset {
                                    IntOffset(0, player.screen_size.height.roundToPx() + 100)
                                }
                            else drawWithContent {}
                        }
                        else if (Platform.DESKTOP.isCurrent()) {
                            offset {
                                IntOffset(0, (player.getNowPlayingExpansionOffset(this).notNaN() + minimised_now_playing_height).roundToPx())
                            }
                        }
                        else this
                    }
        )
    }
}

fun Dp.notNaN(): Dp =
    if (this == Dp.Unspecified) 0.dp else this

@Composable
private fun InitProgressIndicator(
    progress: Float?,
    colour: Color,
    track_colour: Color,
    modifier: Modifier = Modifier
) {
    if (progress == null) {
        LinearProgressIndicator(
            color = colour,
            trackColor = track_colour,
            modifier = modifier,
        )
    }
    else {
        LinearProgressIndicator(
            progress = { progress },
            color = colour,
            trackColor = track_colour,
            modifier = modifier
        )
    }
}

private fun WebRequest.toWebViewRequest(): WebViewRequest =
    WebViewRequest(
        url,
        isRedirect,
        method,
        headers.toMap()
    )
