package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import PlatformIO
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.WebViewLogin
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import io.ktor.http.Headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.youtube_login_load_message
import java.net.URI

@Composable
internal actual fun YoutubeMusicWebviewLogin(
    api: YoutubeiApi,
    login_url: String,
    modifier: Modifier,
    onFinished: (Result<Headers>?) -> Unit
) {
    var finished: Boolean by remember { mutableStateOf(false) }
    val lock: Object = remember { Object() }

    WebViewLogin(
        initial_url = "https://music.youtube.com",
        modifier = modifier,
        loading_message = stringResource(Res.string.youtube_login_load_message),
        base_cookies = "SOCS=CAESNQgREitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjQwNDE2LjAxX3AyGgJlbiACGgYIgNGWsQY",
        user_agent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.54 Mobile Safari/537.36",
        shouldShowPage = { !it.startsWith(api.api_url) },
        onClosed = { onFinished(null) }
    ) { request, openUrl, getCookies ->
        withContext(Dispatchers.PlatformIO) {
            synchronized(lock) {
                if (finished) {
                    return@withContext
                }

                val url: URI = URI(request.url)
                if (url.host != "music.youtube.com" || url.path?.startsWith("/youtubei/v1/") != true) {
                    return@withContext
                }

                if (!request.headers.containsKey("Authorization")) {
                    openUrl(login_url)
                    return@withContext
                }

                finished = true

                val cookies: List<Pair<String, String>> =
                    runBlocking {
                        getCookies("https://music.youtube.com")
                    }

                val new_headers: Headers =
                    Headers.build {
                        append("Cookie", cookies.map { "${it.first}=${it.second}" }.joinToString(";"))
                        for (header in request.headers) {
                            if (header.key.lowercase() == "cookie") {
                                continue
                            }
                            append(header.key, header.value)
                        }
                    }

                onFinished(Result.success(new_headers))
            }
        }
    }
}
