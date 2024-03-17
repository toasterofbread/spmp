package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.platform.WebViewLogin
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import io.ktor.http.Headers

@Composable
internal fun YoutubeMusicWebviewLogin(
    api: YoutubeiApi,
    login_url: String,
    modifier: Modifier = Modifier,
    onFinished: (Result<Headers>?) -> Unit
) {
    var finished: Boolean by remember { mutableStateOf(false) }
    val lock: Object = remember { Object() }

    WebViewLogin(
        api.api_url,
        modifier,
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
                val new_headers: Headers = Headers.build {
                    append("Cookie", cookie)
                    for (header in request.requestHeaders) {
                        append(header.key, header.value)
                    }
                }

                onFinished(Result.success(new_headers))
            }
        }
    }
}
