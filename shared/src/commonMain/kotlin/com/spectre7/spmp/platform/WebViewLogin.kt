package com.spectre7.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.YoutubeMusicAuthInfo

expect fun isWebViewLoginSupported(): Boolean

@Composable
expect fun WebViewLogin(
    initial_url: String,
    modifier: Modifier = Modifier,
    shouldShowPage: (url: String) -> Boolean = { true },
    onRequestIntercepted: (WebResourceRequest, openUrl: (String) -> Unit) -> Boolean // if true, interception will stop
)
