package com.spectre7.spmp.platform

import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import com.spectre7.spmp.api.*
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.YoutubeMusicAuthInfo
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.isDark
import okhttp3.Request

@Composable
actual fun YoutubeMusicLogin(modifier: Modifier, onFinished: (Result<YoutubeMusicAuthInfo>) -> Unit) {
    var web_view: WebView? by remember { mutableStateOf(null) }
    val is_dark by remember { derivedStateOf { Theme.current.background.isDark() } }

    var login_requested by remember { mutableStateOf(false) }
    OnChangedEffect(login_requested) {
        if (login_requested) {
            web_view?.loadUrl(YOUTUBE_MUSIC_LOGIN_URL)
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
                return@AndroidView WebView(context).apply {
                    settings.javaScriptEnabled = true

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        private var login_completed = false

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)

                            if (url.startsWith(YOUTUBE_MUSIC_URL)) {
                                show_webview = false
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            if (url?.startsWith(YOUTUBE_MUSIC_URL) == false) {
                                show_webview = true
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            if (request.url.host == "music.youtube.com" && request.url.path?.startsWith("/youtubei/v1/") == true) {
                                if (!request.requestHeaders.containsKey("Authorization")) {
                                    login_requested = true
                                    return null
                                }
                                if (login_completed) {
                                    return null
                                }

                                login_completed = true

                                val cookie = CookieManager.getInstance().getCookie(YOUTUBE_MUSIC_URL)
                                val account_request = Request.Builder()
                                    .url("https://music.youtube.com/youtubei/v1/account/account_menu")
                                    .addHeader("cookie", cookie)
                                    .apply {
                                        for (header in request.requestHeaders) {
                                            addHeader(header.key, header.value)
                                        }
                                    }
                                    .post(DataApi.getYoutubeiRequestBody())
                                    .build()

                                val result = DataApi.request(account_request)
                                if (result.isFailure) {
                                    onFinished(result.cast())
                                    return null
                                }

                                result.getOrThrow().also { response ->
                                    val parsed: AccountMenuResponse = DataApi.klaxon.parse(response.body!!.charStream())!!
                                    response.close()

                                    onFinished(Result.success(
                                        YoutubeMusicAuthInfo(
                                            parsed.getAritst()!!,
                                            cookie,
                                            request.requestHeaders
                                        )
                                    ))
                                }
                            }
                            return null
                        }
                    }

                    loadUrl(YOUTUBE_MUSIC_LOGIN_URL)
                    web_view = this
                }
            }
        )
    }
}

private class AccountMenuResponse(val actions: List<Action>) {
    class Action(val openPopupAction: OpenPopupAction)
    class OpenPopupAction(val popup: Popup)
    class Popup(val multiPageMenuRenderer: MultiPageMenuRenderer)
    class MultiPageMenuRenderer(val sections: List<Section>, val header: Header? = null)

    class Section(val multiPageMenuSectionRenderer: MultiPageMenuSectionRenderer)
    class MultiPageMenuSectionRenderer(val items: List<Item>)
    class Item(val compactLinkRenderer: CompactLinkRenderer)
    class CompactLinkRenderer(val navigationEndpoint: NavigationEndpoint? = null)

    class Header(val activeAccountHeaderRenderer: ActiveAccountHeaderRenderer)
    class ActiveAccountHeaderRenderer(val accountName: TextRuns, val accountPhoto: MusicThumbnailRenderer.Thumbnail)

    fun getAritst(): Artist? {
        val artist = Artist.fromId(getChannelId() ?: return null)
        val account = actions.first().openPopupAction.popup.multiPageMenuRenderer.header!!.activeAccountHeaderRenderer

        artist.supplyTitle(account.accountName.first_text)
        artist.supplyThumbnailProvider(MediaItem.ThumbnailProvider.fromThumbnails(account.accountPhoto.thumbnails))
        return artist
    }

    fun getChannelId(): String? {
        for (section in actions.first().openPopupAction.popup.multiPageMenuRenderer.sections) {
            for (item in section.multiPageMenuSectionRenderer.items) {
                val browse_endpoint = item.compactLinkRenderer.navigationEndpoint?.browseEndpoint
                if (browse_endpoint?.getPageType() == "MUSIC_PAGE_TYPE_USER_CHANNEL") {
                    return browse_endpoint.browseId
                }
            }
        }
        return null
    }
}
