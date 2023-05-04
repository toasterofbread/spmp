package com.spectre7.spmp.platform

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.isDark

actual fun isWebViewLoginSupported(): Boolean = true

class WebResourceRequestReader(private val request: WebResourceRequest): WebViewRequest {
    override val url: String
        get() = request.url.toString()
    override val isRedirect: Boolean
        get() = request.isRedirect
    override val method: String
        get() = request.method
    override val requestHeaders: Map<String, String>
        get() = request.requestHeaders
}

@Composable
actual fun WebViewLogin(
    initial_url: String,
    modifier: Modifier,
    shouldShowPage: (url: String) -> Boolean,
    onRequestIntercepted: (WebViewRequest, openUrl: (String) -> Unit, getCookie: (String) -> String) -> Unit
) {
    var web_view: WebView? by remember { mutableStateOf(null) }
    val is_dark by remember { derivedStateOf { Theme.current.background.isDark() } }

    var requested_url: String? by remember { mutableStateOf(null) }
    OnChangedEffect(requested_url) {
        requested_url?.also {
            web_view?.loadUrl(it)
        }
    }

    OnChangedEffect(web_view, is_dark) {
        web_view?.apply {
            @Suppress("DEPRECATION")
            settings.forceDark = if (is_dark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
        }
    }

    BackHandler(web_view?.canGoBack() == true) {
        web_view?.goBack()
    }

    var show_webview by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.Center) {
        AnimatedVisibility(!show_webview) {
            CircularProgressIndicator()
        }

        AndroidView(
            modifier = modifier.graphicsLayer {
                alpha = if (show_webview) 1f else 0f
            },
            factory = { context ->
                WebView(context).apply {
                    WebStorage.getInstance().deleteAllData()

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)

                            if (!shouldShowPage(url)) {
                                show_webview = false
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            if (url != null && shouldShowPage(url)) {
                                show_webview = true
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            onRequestIntercepted(
                                WebResourceRequestReader(request),
                                {
                                    requested_url = it
                                },
                                { url ->
                                    CookieManager.getInstance().getCookie(url)
                                }
                            )
                            return null
                        }
                    }

                    loadUrl(initial_url)
                    web_view = this
                }
            }
        )
    }
}
