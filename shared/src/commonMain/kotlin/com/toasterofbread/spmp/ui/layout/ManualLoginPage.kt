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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.utils.composable.Marquee
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.setAlpha

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

    InfoEntry(entry_label, onFinished)

    Column(
        modifier
            .padding(
                vertical = SpMp.context.getDefaultVerticalPadding()
            )
            .padding(
                top = 40.dp,
                bottom = player.nowPlayingBottomPadding(true)
            )
            .fillMaxHeight(),
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
private fun InfoEntry(label: String, onFinished: (String?) -> Pair<String, String>?) {
    val player = LocalPlayerState.current
    var parse_error: Pair<String, String>? by remember { mutableStateOf(null) }

    parse_error?.also { error ->
        ErrorDialog(error) { parse_error = null }
    }

    DisposableEffect(Unit) {
        var headers_value by mutableStateOf("")
        val action: @Composable PillMenu.Action.(Int) -> Unit = {
            ActionButton(Icons.Default.Done) {
                parse_error = onFinished(headers_value)
            }
        }
        val field_action: @Composable PillMenu.Action.() -> Unit = {
            TextField(
                headers_value,
                { headers_value = it },
                label = {
                    Text(label)
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
