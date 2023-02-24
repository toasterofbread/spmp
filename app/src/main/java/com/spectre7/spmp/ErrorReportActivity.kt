package com.spectre7.spmp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.ui.theme.ApplicationTheme
import com.spectre7.utils.sendToast

class ErrorReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("message") ?: "No message"
        val stack_trace = intent.getStringExtra("stack_trace") ?: "No stack trace"

        val share_intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, stack_trace)
            type = "text/plain"
        }, null)

        setContent {
            ApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    Column(Modifier
                        .fillMaxSize()
                        .padding(10.dp)) {
                        var stack_wrap_enabled by remember { mutableStateOf(false) }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("An error occurred", fontSize = 22.sp)
                                Text(message)
                            }

                            Spacer(Modifier.requiredWidth(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Text wrap")
                                    Switch(checked = stack_wrap_enabled, onCheckedChange = { stack_wrap_enabled = it })
                                }

                                Column {
                                    IconButton(onClick = { startActivity(share_intent) }) {
                                        Icon(Icons.Outlined.Share, null)
                                    }

                                    val clipboard = LocalClipboardManager.current
                                    IconButton(onClick = {
                                        clipboard.setText(AnnotatedString(stack_trace))
                                        sendToast("Copied stack trace to clipboard", context = this@ErrorReportActivity)
                                    }) {
                                        Icon(Icons.Outlined.ContentCopy, null)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .then(if (stack_wrap_enabled) Modifier else Modifier.horizontalScroll(
                                    rememberScrollState()))
                        ) {
                            SelectionContainer {
                                Text(stack_trace, softWrap = stack_wrap_enabled, fontSize = 15.sp)
                            }
                        }
                    }

                }
            }
        }
    }
}