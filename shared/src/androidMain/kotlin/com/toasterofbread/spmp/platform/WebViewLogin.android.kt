package com.toasterofbread.spmp.platform

import android.webkit.WebStorage
import android.webkit.CookieManager
import com.toasterofbread.spmp.platform.AppContext

actual suspend fun initWebViewLogin(context: AppContext, onProgress: (Float, String?) -> Unit): Result<Boolean> {
    clearStorage()
    return Result.success(false)
}

private fun clearStorage() {
    WebStorage.getInstance().deleteAllData()
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }
}

// private fun WebResourceRequest.toWebViewRequest(): WebViewRequest =
//     WebViewRequest(
//         url.toString(),
//         isRedirect,
//         method,
//         requestHeaders
//     )

// @SuppressLint("SetJavaScriptEnabled")
// @Composable
// fun OldWebViewLogin(
//     initial_url: String,
//     modifier: Modifier,
//     onClosed: () -> Unit,
//     shouldShowPage: (url: String) -> Boolean,
//     loading_message: String?,
//     onRequestIntercepted: suspend (WebViewRequest, openUrl: (String) -> Unit) -> Unit
// ) {
//     val player: PlayerState = LocalPlayerState.current
//     var web_view: WebView? by remember { mutableStateOf(null) }
//     val is_dark: Boolean by remember { derivedStateOf { player.theme.background.isDark() } }

//     var requested_url: String? by remember { mutableStateOf(null) }
//     OnChangedEffect(requested_url) {
//         requested_url?.also {
//             web_view?.loadUrl(it)
//         }
//     }

//     OnChangedEffect(web_view, is_dark) {
//         web_view?.apply {
//             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                 settings.isAlgorithmicDarkeningAllowed = is_dark
//             }
//             else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                 @Suppress("DEPRECATION")
//                 settings.forceDark = if (is_dark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
//             }
//         }
//     }

//     DisposableEffect(Unit) {
//         onDispose {
//             clearStorage()
//         }
//     }

//     BackHandler(web_view?.canGoBack() == true) {
//         val web: WebView = web_view ?: return@BackHandler

//         val back_forward_list = web.copyBackForwardList()
//         if (back_forward_list.currentIndex > 0) {
//             val previous_url = back_forward_list.getItemAtIndex(back_forward_list.currentIndex - 1).url
//             if (previous_url == initial_url) {
//                 onClosed()
//                 clearStorage()
//                 return@BackHandler
//             }
//         }

//         web.goBack()
//     }

//     var show_webview by remember { mutableStateOf(false) }

//     Box(contentAlignment = Alignment.Center) {
//         AnimatedVisibility(!show_webview, enter = fadeIn(), exit = fadeOut()) {
//             SubtleLoadingIndicator(message = loading_message)
//         }

//         AndroidView(
//             modifier = modifier.graphicsLayer {
//                 alpha = if (show_webview) 1f else 0f
//             },
//             factory = { context ->
//                 WebView(context).apply {
//                     clearStorage()

//                     settings.javaScriptEnabled = true
//                     settings.domStorageEnabled = true

//                     layoutParams = ViewGroup.LayoutParams(
//                         ViewGroup.LayoutParams.MATCH_PARENT,
//                         ViewGroup.LayoutParams.MATCH_PARENT
//                     )

//                     webChromeClient = object : WebChromeClient() {
//                         override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
//                             return true
//                         }
//                     }
//                     webViewClient = object : WebViewClient() {
//                         override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
//                             super.onPageStarted(view, url, favicon)

//                             if (!shouldShowPage(url)) {
//                                 show_webview = false
//                             }
//                         }

//                         override fun onPageFinished(view: WebView?, url: String?) {
//                             super.onPageFinished(view, url)

//                             if (url != null && shouldShowPage(url)) {
//                                 show_webview = true
//                             }
//                         }

//                         override fun shouldInterceptRequest(
//                             view: WebView,
//                             request: WebResourceRequest
//                         ): WebResourceResponse? {
//                             runBlocking {
//                                 onRequestIntercepted(
//                                     request.toWebViewRequest(),
//                                     {
//                                         requested_url = it
//                                     }
//                                 )
//                             }
//                             return null
//                         }
//                     }

//                     loadUrl(initial_url)
//                     web_view = this
//                 }
//             }
//         )
//     }
// }
