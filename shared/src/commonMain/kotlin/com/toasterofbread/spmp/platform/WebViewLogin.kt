package com.toasterofbread.spmp.platform

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable

expect fun isWebViewLoginSupported(): Boolean

data class WebViewRequest(
    val url: String,
    val is_redirect: Boolean,
    val method: String,
    val headers: Map<String, String>
)

// Returns true if restart required
internal expect suspend fun initWebViewLogin(context: AppContext, onProgress: (Float, String?) -> Unit): Result<Boolean>

@Composable
expect fun WebViewLogin(
    initial_url: String,
    onClosed: () -> Unit,
    shouldShowPage: (url: String) -> Boolean,
    modifier: Modifier = Modifier,
    loading_message: String? = null,
    base_cookies: String = "",
    user_agent: String? = null,
    viewport_width: String? = null,
    viewport_height: String? = null,
    onRequestIntercepted: suspend (WebViewRequest, openUrl: (String) -> Unit, getCookies: suspend (String) -> List<Pair<String, String>>) -> Unit
)
