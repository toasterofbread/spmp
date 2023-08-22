package com.toasterofbread.spmp

import SpMp
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.thenIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

class ErrorReportActivity : ComponentActivity() {
    private val coroutine_scope = CoroutineScope(Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("message") ?: "No message"
        val stack_trace = intent.getStringExtra("stack_trace") ?: "No stack trace"

        val context: PlatformContext? =
            try {
                PlatformContext(this, coroutine_scope).init()
            }
            catch (_: Throwable) {
                null
            }

        setContent {
            if (context != null) {
                Theme.ApplicationTheme(context) {
                    ErrorDisplay(message, stack_trace, context)
                }
            }
            else {
                ErrorDisplay(message, stack_trace, null)
            }
        }
    }

    override fun onDestroy() {
        coroutine_scope.cancel()
        super.onDestroy()
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun ErrorDisplay(message: String, stack_trace: String, context: PlatformContext?) {
        val share_intent = remember {
            Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, stack_trace)
                type = "text/plain"
            }, null)
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            var width by remember { mutableStateOf(0) }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .onSizeChanged {
                        width = it.width
                    }
            ) {
                var wrap_text by remember { mutableStateOf(false) }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(getString("error_message_generic"), fontSize = 22.sp)

                        Row {
                            IconButton(onClick = { startActivity(share_intent) }) {
                                Icon(Icons.Outlined.Share, null)
                            }

                            val clipboard = LocalClipboardManager.current
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(stack_trace))
                                context?.sendToast("Copied stack trace to clipboard")
                            }) {
                                Icon(Icons.Outlined.ContentCopy, null)
                            }

                            val discord_webhook_url = ProjectBuildConfig.DISCORD_ERROR_REPORT_WEBHOOK
                            if (discord_webhook_url != null) {
                                val coroutine_scope = rememberCoroutineScope()

                                IconButton({
                                    coroutine_scope.launch(Dispatchers.IO) {
                                        val client = OkHttpClient()
                                        val klaxon = Klaxon()

                                        for (chunk in listOf("---\nMESSAGE: $message\n\nSTACKTRACE:") + stack_trace.chunked(2000)) {
                                            val body = klaxon.toJsonString(mapOf(
                                                "content" to chunk,
                                                "username" to message.take(78) + if (message.length > 78) ".." else "",
                                                "avatar_url" to "https://raw.githubusercontent.com/toasterofbread/spmp/main/androidApp/src/main/ic_launcher-playstore.png"
                                            )).toRequestBody("application/json".toMediaType())

                                            val request = Request.Builder()
                                                .url(discord_webhook_url)
                                                .post(body)
                                                .build()

                                            val response = client.newCall(request).execute()
                                            SpMp.context.sendToast(response.code.toString())

                                            if (!response.isSuccessful) {
                                                println(response.body!!.string())
                                            }

                                            response.close()

                                            delay(500)
                                        }
                                    }
                                }) {
                                    Icon(painterResource("drawable/ic_discord.xml"), null)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.requiredWidth(10.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(getString("wrap_text_switch_label"))
                        Switch(checked = wrap_text, onCheckedChange = { wrap_text = it })
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Scroll modifiers don't work here, no idea why
                LazyRow {
                    item {
                        LazyColumn(
                            Modifier.thenIf(wrap_text) {
                                width(with(LocalDensity.current) { width.toDp() })
                            },
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                SelectionContainer {
                                    Text(message, softWrap = wrap_text, fontSize = 20.sp)
                                }
                            }
                            item {
                                SelectionContainer {
                                    Text(stack_trace, softWrap = wrap_text, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
