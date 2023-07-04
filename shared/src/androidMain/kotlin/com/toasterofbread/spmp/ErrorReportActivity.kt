package com.toasterofbread.spmp

import SpMp
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

class ErrorReportActivity : ComponentActivity() {

    @OptIn(ExperimentalResourceApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("message") ?: "No message"
        val stack_trace = intent.getStringExtra("stack_trace") ?: "No stack trace"

        val share_intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, stack_trace)
            type = "text/plain"
        }, null)

        val context = PlatformContext(this)

        setContent {
            ApplicationTheme(context) {
                Surface(modifier = Modifier.fillMaxSize()) {

                    Column(
                        Modifier
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
                                        context.sendToast("Copied stack trace to clipboard")
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

                                                for (chunk in listOf("--------------\nMESSAGE: $message\n\nSTACKTRACE:") + stack_trace.chunked(2000)) {
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
                                                }
                                            }
                                        }) {
                                            Icon(painterResource("drawable/ic_discord.xml"), null)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .then(
                                    if (stack_wrap_enabled) Modifier else Modifier.horizontalScroll(
                                        rememberScrollState()
                                    )
                                )
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