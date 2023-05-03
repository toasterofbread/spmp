package com.spectre7.spmp.platform

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.YoutubeMusicAuthInfo

actual fun isWebViewLoginSupported(): Boolean get() = false

@Composable
actual fun WebViewLogin(
    initial_url: String,
    modifier: Modifier = Modifier,
    shouldShowPage: (url: String) -> Boolean,
    onRequestIntercepted: (WebResourceRequest, openUrl: (String) -> Unit) -> Boolean
) {
    throw NotImplementedException()
}