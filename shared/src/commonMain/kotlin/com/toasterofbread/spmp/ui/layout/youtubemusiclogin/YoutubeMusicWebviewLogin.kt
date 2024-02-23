package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.platform.WebViewLogin
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import okhttp3.Headers
import okhttp3.Request
import okhttp3.OkHttpClient
import okhttp3.Response
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.AccountSwitcherEndpoint
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.AccountSelectionData
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeChannelNotCreatedException
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YTMLogin
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.executeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.toasterofbread.spmp.youtubeapi.fromJson

@Composable
internal fun YoutubeMusicWebviewLogin(
    api: YoutubeMusicApi,
    login_url: String,
    modifier: Modifier = Modifier,
    onFinished: (Result<Headers>?) -> Unit
) {
    var finished: Boolean by remember { mutableStateOf(false) }
    val lock: Object = remember { Object() }

    WebViewLogin(
        api.api_url,
        Modifier.fillMaxSize(),
        loading_message = getString("youtube_login_load_message"),
        shouldShowPage = { !it.startsWith(api.api_url) },
        onClosed = { onFinished(null) }
    ) { request, openUrl, getCookie ->
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                if (finished) {
                    return@withContext
                }

                val url: URI = URI(request.url)
                if (url.host != "music.youtube.com" || url.path?.startsWith("/youtubei/v1/") != true) {
                    return@withContext
                }

                if (!request.requestHeaders.containsKey("Authorization")) {
                    openUrl(login_url)
                    return@withContext
                }

                finished = true

                val cookie: String = getCookie(api.api_url)
                val headers_builder: Headers.Builder = Headers.Builder()
                    .add("Cookie", cookie)
                    .apply {
                        for (header in request.requestHeaders) {
                            add(header.key, header.value)
                        }
                    }

                onFinished(Result.success(headers_builder.build()))
            }
        }
    }
}
