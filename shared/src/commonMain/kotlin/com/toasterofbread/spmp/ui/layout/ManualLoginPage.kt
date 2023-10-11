package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultPaddingValues
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.composable.Marquee
import com.toasterofbread.utils.composable.WidthShrinkText

@Composable
fun ManualLoginPage(
    steps: List<String>,
    suffix: String,
    entry_label: String,
    modifier: Modifier = Modifier,
    desktop_browser_needed: Boolean = true,
    onFinished: (String?) -> Pair<String, String>?,
) {
    val player = LocalPlayerState.current

    Box(modifier) {
        Column(
            Modifier
                .padding(horizontal = player.getDefaultHorizontalPadding())
                .padding(top = 30.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            if (desktop_browser_needed) {
                Text(
                    getString("manual_login_desktop_browser_may_be_needed"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
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

        InfoEntry(
            entry_label,
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
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
    PlatformAlertDialog(
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
