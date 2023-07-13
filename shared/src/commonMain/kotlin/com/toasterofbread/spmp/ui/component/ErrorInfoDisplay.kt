package com.toasterofbread.spmp.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.JsonParseException
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.modifier.background
import com.toasterofbread.utils.modifier.disableParentScroll
import com.toasterofbread.utils.thenIf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun ErrorInfoDisplay(
    error: Throwable,
    modifier: Modifier = Modifier,
    message: String? = null,
    expanded_modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    var expanded: Boolean by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)

    CompositionLocalProvider(LocalContentColor provides Theme.current.background) {
        Column(
            (if (expanded) expanded_modifier else modifier)
                .heightIn(min = 50.dp)
                .animateContentSize()
                .background(shape, Theme.current.accent_provider)
                .padding(
                    vertical = 3.dp,
                    horizontal = 10.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    expanded = !expanded
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Crossfade(expanded) { expanded ->
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null
                    )
                }

                WidthShrinkText(
                    message ?: error::class.java.simpleName,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )

                if (onDismiss != null) {
                    ShapedIconButton(
                        onDismiss,
                        shape = shape,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Theme.current.background,
                            contentColor = Theme.current.on_background
                        )
                    ) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }

            if (expanded) {
                ExpandedContent(error, shape)
            }
        }
    }
}

@Composable
private fun ExpandedContent(error: Throwable, shape: Shape) {
    val coroutine_scope = rememberCoroutineScope()
    var text_to_show: String? by remember { mutableStateOf(null) }
    var wrap_text by remember { mutableStateOf(false) }
    val button_colours = ButtonDefaults.buttonColors(
        containerColor = Theme.current.accent,
        contentColor = Theme.current.background
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .padding(bottom = 10.dp)
            .background(Theme.current.background_provider)
            .padding(10.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Theme.current.on_background) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        getString("wrap_text_switch_label"),
                        color = Theme.current.on_background
                    )
                    Switch(wrap_text, { wrap_text = !wrap_text }, Modifier.padding(end = 10.dp))

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    val paste_token = ProjectBuildConfig.PASTE_EE_TOKEN
                    if (paste_token != null) {
                        Button(
                            {
                                coroutine_scope.launch {
                                    text_to_show = uploadErrorToPasteEe(error, paste_token).getOrElse { it.toString() }
                                }
                            },
                            colors = button_colours,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            WidthShrinkText(getString("upload_to_paste_dot_ee"), alignment = TextAlign.Center)
                        }
                    }
                }

                Crossfade(text_to_show ?: error.stackTraceToString()) { text ->
                    Column(
                        Modifier
                            .disableParentScroll(disable_x = false)
                            .verticalScroll(rememberScrollState())
                            .thenIf(!wrap_text) { horizontalScroll(rememberScrollState()) }
                    ) {
                        SelectionContainer {
                            Text(
                                text,
                                color = Theme.current.on_background,
                                softWrap = wrap_text
                            )
                        }

                        if (text.none { it == '\n' }) {
                            Row {
                                SpMp.context.CopyShareButtons() {
                                    text
                                }
                            }
                        }

                        Spacer(Modifier.height(50.dp))
                    }
                }
            }

            Row(Modifier.align(Alignment.BottomStart), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val extra_button_text =
                    if (text_to_show != null) getString("action_cancel")
                    else when (error) {
                        is JsonParseException -> getString("error_info_display_show_json_data")
                        else -> null
                    }

                if (extra_button_text != null) {
                    Button(
                        {
                            if (text_to_show != null) {
                                text_to_show = null
                            } else {
                                when (error) {
                                    is JsonParseException -> {
                                        text_to_show = error.json_obj.toJsonString(true)
                                    }
                                }
                            }
                        },
                        shape = shape,
                        colors = button_colours
                    ) {
                        Text(extra_button_text, softWrap = false)
                    }
                }

                if (ProjectBuildConfig.IS_DEBUG) {
                    Button(
                        { throw error },
                        colors = button_colours
                    ) {
                        Text(getString("throw_error"))
                    }
                }
            }
        }
    }
}

private suspend fun uploadErrorToPasteEe(error: Throwable, token: String): Result<String> = withContext(Dispatchers.IO) {
    val data = mapOf(
        "sections" to listOf(
            mapOf("name" to "MESSAGE", "contents" to error.message.toString()),
            mapOf("name" to "STACKTRACE", "contents" to error.stackTraceToString()),
        ) + if (error is JsonParseException) listOf(mapOf("name" to "JSON DATA", "syntax" to "json", "contents" to error.json_obj.toJsonString()))
            else emptyList()
    )

    val request = Request.Builder()
        .url("https://api.paste.ee/v1/pastes")
        .header("X-Auth-Token", token)
        .post(Api.klaxon.toJsonString(data).toRequestBody("application/json".toMediaType()))
        .build()

    try {
        val result = OkHttpClient().newCall(request).execute()
        val response = result.use {
            Api.klaxon.parseJsonObject(it.body!!.charStream())
        }

        if (response["success"] != true || response["link"] == null) {
            return@withContext Result.failure(RuntimeException(response.toJsonString(true)))
        }

        return@withContext Result.success(response["link"] as String)
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
}
