package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringArray
import com.toasterofbread.spmp.ui.layout.ManualLoginPage
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.toastercomposetools.utils.common.indexOfOrNull
import kotlinx.coroutines.launch
import okhttp3.Headers

@Composable
fun YoutubeMusicManualLogin(modifier: Modifier = Modifier, onFinished: (Result<YoutubeApi.UserAuthState>?) -> Unit) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    ManualLoginPage(
        steps = getStringArray("youtube_manual_login_steps"),
        suffix = getString("youtube_manual_login_suffix"),
        entry_label = getString("youtube_manual_login_field"),
        modifier = modifier
    ) { entry ->
        if (entry == null) {
            onFinished(null)
            return@ManualLoginPage null
        }

        getHeadersFromManualEntry(entry).fold(
            { headers ->
                coroutine_scope.launch {
                    onFinished(
                        player.context.ytapi.UpdateUserAuthState.byHeaders(headers)
                    )
                }
                null
            },
            { error ->
                if (error is MissingHeadersException) {
                    error.keys.joinToString("\n") { header ->
                        header.replaceFirstChar { it.uppercase() }
                    }
                    Pair(
                        getString("manual_login_error_missing_following_headers"),
                        error.keys.joinToString("\n") { header ->
                            header.replaceFirstChar { it.uppercase() }
                        }
                    )
                }
                else Pair(error.javaClass.simpleName, error.message ?: "")
            }
        )
    }
}

private class MissingHeadersException(val keys: List<String>): RuntimeException()

private fun getHeadersFromManualEntry(headers_text: String): Result<Headers> {
    val headers_builder = Headers.Builder()
    val required_keys = YoutubeMusicAuthInfo.REQUIRED_HEADERS.toMutableList()

    for (line in headers_text.lines()) {
        val colon = line.indexOfOrNull(':') ?: continue
        val space = line.indexOf(' ')

        if (space != -1 && space < colon) {
            continue
        }

        val key = line.substring(0, colon).lowercase()
        headers_builder.add(key, line.substring(colon + 1).trim())
        required_keys.remove(key)
    }

    if (required_keys.isNotEmpty()) {
        return Result.failure(MissingHeadersException(required_keys))
    }

    return Result.success(headers_builder.build())
}
