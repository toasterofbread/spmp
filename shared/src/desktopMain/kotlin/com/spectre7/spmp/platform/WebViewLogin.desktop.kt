package com.spectre7.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun isWebViewLoginSupported(): Boolean = false

@Composable
actual fun WebViewLogin(
    initial_url: String,
    modifier: Modifier,
    shouldShowPage: (url: String) -> Boolean,
    onRequestIntercepted: (WebViewRequest, openUrl: (String) -> Unit, getCookie: (String) -> String) -> Unit
) {
    throw NotImplementedError("Not supported")
}