package com.toasterofbread.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun isWebViewLoginSupported(): Boolean = false

internal actual suspend fun initWebViewLogin(
    context: AppContext,
    onProgress: (Float, String?) -> Unit
): Result<Boolean> = Result.failure(IllegalStateException())

@Composable
actual fun WebViewLogin(
    initial_url: String,
    onClosed: () -> Unit,
    shouldShowPage: (url: String) -> Boolean,
    modifier: Modifier,
    loading_message: String?,
    base_cookies: String,
    user_agent: String?,
    onRequestIntercepted: suspend (WebViewRequest, openUrl: (String) -> Unit, getCookies: suspend (String) -> List<Pair<String, String>>) -> Unit
) {
    throw IllegalStateException()
}
