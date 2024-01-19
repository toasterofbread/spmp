package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.composekit.utils.composable.Marquee
import com.toasterofbread.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

@Composable
fun ManualLoginPage(
    steps: List<String>,
    suffix: String,
    entry_label: String,
    modifier: Modifier = Modifier,
    login_url: String? = null,
    desktop_browser_needed: Boolean = true,
    onFinished: (String?) -> Pair<String, String>?,
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current

    Box(modifier) {
        var info_entry_position: Dp by remember { mutableStateOf(0.dp) }

        Column(
            Modifier
                .padding(horizontal = player.getDefaultHorizontalPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            if (desktop_browser_needed || login_url != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (desktop_browser_needed) {
                        Text(
                            getString("manual_login_desktop_browser_may_be_needed"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (login_url != null) {
                        IconButton({
                            player.context.openUrl(login_url)
                        }) {
                            Icon(Icons.Default.OpenInNew, null)
                        }
                    }
                }
            }

            @Composable
            fun step(text: String, index: Int, modifier: Modifier = Modifier, shrink: Boolean = false) {
                Row(modifier.alpha(0.85f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        (index + 1).toString(),
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (shrink) {
                        WidthShrinkText(
                            text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    else {
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyLarge,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

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
                            LocalContentColor.current.copy(alpha = 0.5f),
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

            Spacer(Modifier.height((player.screen_size.height - info_entry_position).coerceAtLeast(0.dp)))
        }

        InfoEntry(
            entry_label,
            player.nowPlayingTopOffset(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
            .onGloballyPositioned {
                info_entry_position = with (density) {
                    it.positionInWindow().y.toDp()
                }
            },
            onFinished
        )
    }
}

@Composable
private fun InfoEntry(label: String, modifier: Modifier = Modifier, onFinished: (String?) -> Pair<String, String>?) {
    var headers_value by remember { mutableStateOf("") }
    
    var parse_error: Pair<String, String>? by remember { mutableStateOf(null) }
    parse_error?.also { error ->
        ErrorDialog(error) { parse_error = null }
    }

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton({ parse_error = onFinished(null) }) {
            Icon(Icons.Default.Close, null)
        }
        
        TextField(
            headers_value,
            { headers_value = it },
            Modifier.fillMaxWidth().weight(1f),
            label = {
                Text(label)
            },
            singleLine = true
        )

        IconButton({ parse_error = onFinished(headers_value) }) {
            Icon(Icons.Default.Done, null)
        }
    }
}

@Composable
private fun ErrorDialog(error: Pair<String, String>, close: () -> Unit) {
    AlertDialog(
        onDismissRequest = close,
        confirmButton = {
            Button(close) {
                Text(getString("action_close"))
            }
        },
        title = {
            WidthShrinkText(error.first)
        },
        text = {
            Text(error.second, fontSize = 15.sp)
        }
    )
}
