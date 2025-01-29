package com.toasterofbread.spmp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anggrayudi.storage.extension.count
import com.toasterofbread.spmp.model.JsonHttpClient
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.resources.stringResourceTODO
import com.toasterofbread.spmp.ui.component.uploadErrorToPasteEe
import dev.toastbits.composekit.application.ApplicationTheme
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.context.ApplicationContext
import dev.toastbits.composekit.theme.core.ThemeManager
import dev.toastbits.composekit.util.thenIf
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.error_message_generic
import spmp.shared.generated.resources.ic_discord
import spmp.shared.generated.resources.upload_to_paste_dot_ee
import spmp.shared.generated.resources.wrap_text_switch_label

private const val LOGCAT_LINES_TO_DISPLAY: Int = 100

class ErrorReportActivity : ComponentActivity() {
    private val coroutine_scope = CoroutineScope(Job())
    private var logcat_output: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message: String = intent.getStringExtra("message") ?: "No message"
        val stack_trace: String = intent.getStringExtra("stack_trace") ?: "No stack trace"

        val context: AppContext? =
            try {
                runBlocking {
                    AppContext.create(this@ErrorReportActivity, coroutine_scope, ApplicationContext(this@ErrorReportActivity))
                }
            }
            catch (_: Throwable) {
                null
            }

        val logcat_lines = LOGCAT_LINES_TO_DISPLAY + stack_trace.count("\n")

        coroutine_scope.launch(Dispatchers.IO) {
            logcat_output = retrieveLogcat(logcat_lines)
        }

        setContent {
            logcat_output?.let { logcat ->
                val error_text = remember(stack_trace, logcat) {
                    "---STACK TRACE---\n$stack_trace\n---LOGCAT (last $logcat_lines lines)---\n$logcat"
                }

                val theme: ThemeManager? = context?.theme
                if (theme != null) {
                    theme.ApplicationTheme(context, context.settings) {
                        ErrorDisplay(context, message, stack_trace, logcat, error_text)
                    }
                }
                else {
                    ErrorDisplay(null, message, stack_trace, logcat, error_text)
                }
                return@setContent
            }

            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                SubtleLoadingIndicator()
                Text(stringResourceTODO("Retrieving crash logcat..."))
            }
        }
    }

    override fun onDestroy() {
        coroutine_scope.cancel()
        super.onDestroy()
    }

    @Composable
    fun ErrorDisplay(context: AppContext?, message: String, stack_trace: String, logs: String, error_text: String) {
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
                            Text(stringResource(Res.string.error_message_generic), fontSize = 22.sp)

                            Row {
                                IconButton(onClick = { startActivity(share_intent) }) {
                                    Icon(Icons.Outlined.Share, null)
                                }

                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(error_text))
                                    context?.sendToast("Copied stack trace to clipboard // TODO")
                                }) {
                                    Icon(Icons.Outlined.ContentCopy, null)
                                }

                                val discord_webhook_url = ProjectBuildConfig.DISCORD_ERROR_REPORT_WEBHOOK
                                if (discord_webhook_url != null) {
                                    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

                                    IconButton({
                                        coroutine_scope.launch {
                                            sendErrorWebhook(context, message, error_text, discord_webhook_url)
                                        }
                                    }) {
                                        Icon(painterResource(Res.drawable.ic_discord), null)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.requiredWidth(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(Res.string.wrap_text_switch_label))
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
                    Text(stringResource(Res.string.upload_to_paste_dot_ee))
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

private suspend fun sendErrorWebhook(
    context: AppContext?,
    message: String,
    error_text: String,
    webhook_url: String
) {
    val client: HttpClient = JsonHttpClient

    for (chunk in listOf("---\nMESSAGE: $message\n\n") + error_text.chunked(2000)) {
        val body: JsonObject = buildJsonObject {
            put("content", chunk)
            put("username", message.take(78) + if (message.length > 78) ".." else "")
            put("avatar_url", "https://raw.githubusercontent.com/toasterofbread/spmp/7ebb7c1525bc9c8f7e9fb7cd0d0ff8108dde9345/metadata/en-US/images/icon.png")
        }

        val response: HttpResponse = client.request(webhook_url) {
            method = HttpMethod.Post
            setBody(body)
        }

        context?.sendToast(response.status.value.toString())

        delay(500)
    }
}
