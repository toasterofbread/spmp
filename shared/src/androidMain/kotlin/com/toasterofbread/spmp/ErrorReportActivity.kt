package com.toasterofbread.spmp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.anggrayudi.storage.extension.count
import com.google.gson.Gson
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.component.uploadErrorToPasteEe
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
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

private const val LOGCAT_LINES_TO_DISPLAY: Int = 100

class ErrorReportActivity : ComponentActivity() {
    private val coroutine_scope = CoroutineScope(Job())
    private var logcat_output: String? by mutableStateOf(null)
    private var context: AppContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("message") ?: "No message"
        val stack_trace = intent.getStringExtra("stack_trace") ?: "No stack trace"

        try {
            context = AppContext(this, coroutine_scope).init()
        }
        catch (_: Throwable) {}

        val logcat_lines = LOGCAT_LINES_TO_DISPLAY + stack_trace.count("\n")

        coroutine_scope.launch(Dispatchers.IO) {
            logcat_output = retrieveLogcat(logcat_lines)
        }

        setContent {
            logcat_output?.let { logcat ->
                val error_text = remember(stack_trace, logcat) {
                    "---STACK TRACE---\n$stack_trace\n---LOGCAT (last $logcat_lines lines)---\n$logcat"
                }

                if (context != null) {
                    context!!.theme.ApplicationTheme(context!!) {
                        ErrorDisplay(message, stack_trace, logcat, error_text)
                    }
                }
                else {
                    ErrorDisplay(message, stack_trace, logcat, error_text)
                }
                return@setContent
            }

            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                SubtleLoadingIndicator()
                Text(getStringTODO("Retrieving crash logcat..."))
            }
        }
    }

    override fun onDestroy() {
        coroutine_scope.cancel()
        super.onDestroy()
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun ErrorDisplay(message: String, stack_trace: String, logs: String, error_text: String) {
        val share_intent = remember {
            Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, error_text)
                type = "text/plain"
            }, null)
        }
        val clipboard = LocalClipboardManager.current

        Surface(modifier = Modifier.fillMaxSize()) {
            var width by remember { mutableIntStateOf(0) }

            Box(Modifier.fillMaxSize()) {
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

                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(error_text))
                                    context?.sendToast(getStringTODO("Copied stack trace to clipboard"))
                                }) {
                                    Icon(Icons.Outlined.ContentCopy, null)
                                }

                                val discord_webhook_url = ProjectBuildConfig.DISCORD_ERROR_REPORT_WEBHOOK
                                if (discord_webhook_url != null) {
                                    val coroutine_scope = rememberCoroutineScope()

                                    IconButton({
                                        coroutine_scope.launch(Dispatchers.IO) {
                                            val client = OkHttpClient()
                                            val gson = Gson()

                                            for (chunk in listOf("---\nMESSAGE: $message\n\n") + error_text.chunked(2000)) {
                                                val body = gson.toJson(mapOf(
                                                    "content" to chunk,
                                                    "username" to message.take(78) + if (message.length > 78) ".." else "",
                                                    "avatar_url" to "https://raw.githubusercontent.com/toasterofbread/spmp/main/androidApp/src/main/ic_launcher-playstore.png"
                                                )).toRequestBody("application/json".toMediaType())

                                                val request = Request.Builder()
                                                    .url(discord_webhook_url)
                                                    .post(body)
                                                    .build()

                                                val response = client.newCall(request).execute()
                                                context?.sendToast(response.code.toString())

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

                    val chunked_error = remember(error_text) { error_text.chunked(10000) }

                    // Scroll modifiers don't work here, no idea why
                    LazyRow {
                        item {
                            LazyColumn(
                                Modifier.thenIf(wrap_text) {
                                    width(with(LocalDensity.current) { width.toDp() })
                                },
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                item {
                                    SelectionContainer {
                                        Text(message, softWrap = wrap_text, fontSize = 20.sp)
                                    }
                                }

                                items(chunked_error) { chunk ->
                                    SelectionContainer {
                                        Text(chunk, softWrap = wrap_text, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    {
                        coroutine_scope.launch {
                            coroutine_scope.launch {
                                uploadErrorToPasteEe(
                                    message,
                                    stack_trace,
                                    ProjectBuildConfig.PASTE_EE_TOKEN,
                                    logs = logs
                                ).fold(
                                    { url ->
                                        clipboard.setText(AnnotatedString(url))
                                        context?.sendToast(getStringTODO("URL copied to clipboard"))
                                    },
                                    { error ->
                                        context?.sendToast(getStringTODO("Failed: ") + error.toString())
                                    }
                                )
                            }
                        }
                    },
                    Modifier.align(Alignment.BottomStart).padding(10.dp)
                ) {
                    Text(getString("upload_to_paste_dot_ee"))
                }
            }
        }
    }
}

private fun retrieveLogcat(lines: Int): String {
    println("Retrieving logcat output...")
    
    val process = ProcessBuilder()
        .command(listOf("logcat", "-d"))
        .redirectErrorStream(true)
        .start()
    try {
        val reader = process.inputStream.reader()
        return reader.readLines().takeLast(lines).joinToString("\n")
    }
    finally {
        process.destroy()
    }
}
