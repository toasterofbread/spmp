package com.spectre7.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun isWebViewLoginSupported(): Boolean

interface WebViewRequest {
    val url: String
    val isRedirect: Boolean
    val method: String
    val requestHeaders: Map<String, String>
}

@Composable
expect fun WebViewLogin(
    initial_url: String,
    modifier: Modifier = Modifier,
    shouldShowPage: (url: String) -> Boolean = { true },
    onRequestIntercepted: (WebViewRequest, openUrl: (String) -> Unit, getCookie: (String) -> String) -> Unit // if true, interception will stop
)
