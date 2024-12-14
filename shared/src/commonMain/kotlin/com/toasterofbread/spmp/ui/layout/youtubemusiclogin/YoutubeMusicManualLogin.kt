package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.util.indexOfOrNull
import com.toasterofbread.spmp.ui.layout.ManualLoginPage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.youtube_manual_login_suffix
import spmp.shared.generated.resources.youtube_manual_login_field
import spmp.shared.generated.resources.manual_login_error_missing_following_headers
import spmp.shared.generated.resources.youtube_manual_login_steps

@Composable
internal fun YoutubeMusicManualLogin(
    login_url: String,
    content_padding: PaddingValues,
    modifier: Modifier = Modifier,
    onFinished: (Result<Headers>?) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    ManualLoginPage(
        steps = stringArrayResource(Res.array.youtube_manual_login_steps),
        suffix = stringResource(Res.string.youtube_manual_login_suffix),
        entry_label = stringResource(Res.string.youtube_manual_login_field),
        modifier = modifier,
        login_url = login_url,
        content_padding = content_padding
    ) { entry ->
        if (entry == null) {
            onFinished(null)
            return@ManualLoginPage null
        }

        getHeadersFromManualEntry(entry).fold(
            { headers ->
                onFinished(Result.success(headers))
                return@fold null
            },
            { error ->
                if (error is MissingHeadersException) {
                    error.keys.joinToString("\n") { header ->
                        header.replaceFirstChar { it.uppercase() }
                    }
                    return@fold Pair(
                        getString(Res.string.manual_login_error_missing_following_headers),
                        error.keys.joinToString("\n") { header ->
                            header.replaceFirstChar { it.uppercase() }
                        }
                    )
                }
                else {
                    return@fold Pair(error::class.toString(), error.message ?: "")
                }
            }
        )
    }
}

private class MissingHeadersException(val keys: List<String>): RuntimeException()

private fun getHeadersFromManualEntry(text: String): Result<Headers> {
    val headers_text: String = text.trim()
    if (headers_text.startsWith("curl ")) {
        return getHeadersFromCurlCommand(headers_text)
    }

    val headers_builder: HeadersBuilder = HeadersBuilder()
    val required_keys: MutableList<String> = YoutubeiAuthenticationState.REQUIRED_HEADERS.toMutableList()

    for (line in headers_text.lines()) {
        val colon = line.indexOfOrNull(':') ?: continue
        val space = line.indexOf(' ')

        if (space != -1 && space < colon) {
            continue
        }

        val key: String = line.substring(0, colon).lowercase()
        if (key == "x-goog-authuser") {
            continue
        }

        headers_builder.append(key, line.substring(colon + 1).trim())
        required_keys.remove(key)
    }

    if (required_keys.isNotEmpty()) {
        return Result.failure(MissingHeadersException(required_keys))
    }

    return Result.success(headers_builder.build())
}

private fun getHeadersFromCurlCommand(command: String): Result<Headers> {
    val headers_builder: HeadersBuilder = HeadersBuilder()
    val required_keys: MutableList<String> = YoutubeiAuthenticationState.REQUIRED_HEADERS.toMutableList()

    var header_end: Int = -1
    while (true) {
        val header_start: Int = command.indexOf("-H '", header_end + 1) + 4
        if (header_start == 3) {
            break
        }

        header_end = command.indexOf("'", header_start + 4)
        if (header_end == -1) {
            break
        }

        val split_header: List<String> = command.substring(header_start, header_end).split(":", limit = 2)
        if (split_header.size != 2) {
            continue
        }

        val key: String = split_header[0].trim().lowercase()
        headers_builder.append(key, split_header[1].trim())
        required_keys.remove(key)
    }

    if (required_keys.isNotEmpty()) {
        return Result.failure(MissingHeadersException(required_keys))
    }

    return Result.success(headers_builder.build())
}
