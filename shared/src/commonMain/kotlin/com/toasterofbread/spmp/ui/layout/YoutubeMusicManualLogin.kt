package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringArray
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.utils.composable.Marquee
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.indexOfOrNull
import com.toasterofbread.utils.setAlpha
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun YoutubeMusicManualLogin(modifier: Modifier = Modifier, onFinished: (Result<YoutubeMusicAuthInfo>?) -> Unit) {
    val player = LocalPlayerState.current

    HeadersEntry(onFinished)

    Column(
        modifier
            .padding(
                horizontal = SpMp.context.getDefaultHorizontalPadding(),
                vertical = SpMp.context.getDefaultVerticalPadding()
            )
            .padding(bottom = player.nowPlayingBottomPadding(true))
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        Text(
            getString("info_youtube_manual_login_title"),
            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 25.dp),
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            getString("info_youtube_manual_login_prefix"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        @Composable
        fun step(text: String, index: Int, modifier: Modifier = Modifier, shrink: Boolean = false) {
            Row(modifier.alpha(0.85f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    index.toString(),
                    style = MaterialTheme.typography.bodySmall
                )

                if (shrink) {
                    WidthShrinkText(
                        text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }

        val steps = getStringArray("info_youtube_manual_login_steps")
        val suffix = getString("info_youtube_manual_login_suffix")

        Marquee {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (i in 0 until if (suffix.isBlank()) steps.size else steps.size - 1) {
                    step(steps[i], i)
                }
            }
        }

        if (suffix.isNotBlank()) {
            Column(
                Modifier
                    .border(
                        1.dp,
                        LocalContentColor.current.setAlpha(0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(10.dp)
            ) {
                step(steps.last(), steps.lastIndex, Modifier.fillMaxWidth(), shrink = true)

                Text(
                    '\n' + suffix,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeadersEntry(onFinished: (Result<YoutubeMusicAuthInfo>?) -> Unit) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    var parse_error: Throwable? by remember { mutableStateOf(null) }

    parse_error?.also { error ->
        ParseErrorDialog(error) { parse_error = null }
    }

    DisposableEffect(Unit) {
        var headers_value by mutableStateOf("")
        val action: @Composable PillMenu.Action.(Int) -> Unit = {
            ActionButton(Icons.Default.Done) {
                getHeadersFromManualEntry(headers_value).fold(
                    { headers ->
                        coroutine_scope.launch {
                            onFinished(YoutubeMusicAuthInfo.fromHeaders(headers))
                        }
                    },
                    { parse_error = it }
                )
            }
        }
        val field_action: @Composable PillMenu.Action.() -> Unit = {
            TextField(
                headers_value,
                { headers_value = it },
                label = {
                    Text(getString("info_youtube_manual_login_field"))
                },
                singleLine = true
            )
        }

        player.pill_menu.addExtraAction(false, action)
        player.pill_menu.addAlongsideAction(field_action)

        onDispose {
            player.pill_menu.removeExtraAction(action)
            player.pill_menu.removeAlongsideAction(field_action)
        }
    }
}

private class MissingHeadersException(val keys: List<String>): RuntimeException()

@Composable
private fun ParseErrorDialog(error: Throwable, close: () -> Unit) {
    PlatformAlertDialog(
        onDismissRequest = close,
        confirmButton = {
            Button(close) {
                Text(getString("action_close"))
            }
        },
        title = {
            WidthShrinkText(
                if (error is MissingHeadersException) getString("manual_login_error_missing_following_headers")
                else error.javaClass.simpleName
            )
        },
        text = {
            if (error is MissingHeadersException) {
                Column {
                    for (missing_key in error.keys) {
                        Text(missing_key.replaceFirstChar { it.uppercase() }, fontSize = 15.sp)
                    }
                }
            }
            else {
                Text(error.message ?: "", fontSize = 15.sp)
            }
        }
    )
}

private fun getHeadersFromManualEntry(headers_text: String): Result<Map<String, String>> {
    val ret: MutableMap<String, String> = mutableMapOf()
    val required_keys = YoutubeMusicAuthInfo.REQUIRED_KEYS.toMutableList()

    for (line in headers_text.lines()) {
        val colon = line.indexOfOrNull(':') ?: continue
        val space = line.indexOf(' ')

        if (space != -1 && space < colon) {
            continue
        }

        val key = line.substring(0, colon).lowercase()
        ret[key] = line.substring(colon + 1).trim()
        required_keys.remove(key)
    }

    if (required_keys.isNotEmpty()) {
        return Result.failure(MissingHeadersException(required_keys))
    }

    return Result.success(ret)
}
