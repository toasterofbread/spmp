package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.google.gson.Gson
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.utils.common.isDebugBuild
import com.toasterofbread.utils.common.thenIf
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.modifier.background
import com.toasterofbread.utils.modifier.disableParentScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
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
    expanded_content_modifier: Modifier = Modifier.height(500.dp),
    disable_parent_scroll: Boolean = true,
    start_expanded: Boolean = false,
    extraButtonContent: (@Composable () -> Unit)? = null,
    onExtraButtonPressed: (() -> Unit)? = null,
    onDismiss: (() -> Unit)?
) {
    var expanded: Boolean by remember { mutableStateOf(start_expanded) }
    val shape = RoundedCornerShape(20.dp)

    CompositionLocalProvider(LocalContentColor provides Theme.background) {
        Column(
            modifier
                .animateContentSize()
                .background(shape, Theme.accent_provider)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        expanded = !expanded
                    }
                    .height(50.dp)
                    .padding(vertical = 5.dp),
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

                if (onExtraButtonPressed != null) {
                    Button(
                        onExtraButtonPressed,
                        shape = shape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Theme.background,
                            contentColor = Theme.on_background
                        )
                    ) {
                        extraButtonContent!!.invoke()
                    }
                }

                if (onDismiss != null) {
                    ShapedIconButton(
                        onDismiss,
                        shape = shape,
                        colours = IconButtonDefaults.iconButtonColors(
                            containerColor = Theme.background,
                            contentColor = Theme.on_background
                        )
                    ) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }

            AnimatedVisibility(
                expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandedContent(error, shape, disable_parent_scroll, expanded_content_modifier)
            }
        }
    }
}

@Composable
private fun LongTextDisplay(text: String, wrap_text: Boolean, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val split_text = remember(text) {
        text.chunked(10000)
    }

    LazyColumn(modifier) {
        items(split_text) { segment ->
            SelectionContainer {
                Text(
                    segment,
                    color = Theme.on_background,
                    softWrap = wrap_text
                )
            }
        }


        if (text.none { it == '\n' }) {
            item {
                Row {
                    player.context.CopyShareButtons() {
                        text
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(50.dp))
        }
    }
}

@Composable
private fun ExpandedContent(error: Throwable, shape: Shape, disable_parent_scroll: Boolean, modifier: Modifier = Modifier) {
    val coroutine_scope = rememberCoroutineScope()
    val player = LocalPlayerState.current

    var text_to_show: String? by remember { mutableStateOf(null) }
    var wrap_text by remember { mutableStateOf(false) }
    val button_colours = ButtonDefaults.buttonColors(
        containerColor = Theme.accent,
        contentColor = Theme.background
    )

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .padding(bottom = 10.dp)
            .background(Theme.background_provider)
            .padding(10.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Theme.on_background) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        getString("wrap_text_switch_label"),
                        color = Theme.on_background
                    )
                    Switch(wrap_text, { wrap_text = !wrap_text }, Modifier.padding(end = 10.dp))

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    Button(
                        {
                            coroutine_scope.launch {
                                text_to_show = uploadErrorToPasteEe(
                                    error.message.toString(),
                                    error.stackTraceToString(),
                                    ProjectBuildConfig.PASTE_EE_TOKEN,
                                    error = error
                                ).getOrElse { it.toString() }
                            }
                        },
                        colors = button_colours,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        WidthShrinkText(getString("upload_to_paste_dot_ee"), alignment = TextAlign.Center)
                    }
                }

                Crossfade(text_to_show ?: error.stackTraceToString()) { text ->
                    LongTextDisplay(
                        text,
                        wrap_text,
                        Modifier
                            .thenIf(!wrap_text) { horizontalScroll(rememberScrollState()) }
                            .thenIf(disable_parent_scroll) { disableParentScroll(disable_x = false) }
                    )
                }
            }

            Row(Modifier.align(Alignment.BottomStart), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val extra_button_text =
                    if (text_to_show != null) getString("action_cancel")
                    else when (error) {
                        is com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException -> getString("error_info_display_show_json_data")
                        else -> null
                    }

                var cause_data_loading by remember { mutableStateOf(false) }

                if (extra_button_text != null) {
                    Button(
                        {
                            if (cause_data_loading) {
                                return@Button
                            }

                            if (text_to_show != null) {
                                text_to_show = null
                            }
                            else {
                                when (error) {
                                    is com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException -> {
                                        coroutine_scope.launch {
                                            coroutineContext.job.invokeOnCompletion {
                                                cause_data_loading = false
                                            }

                                            cause_data_loading = true

                                            error.getCauseData().fold(
                                                { text_to_show = it },
                                                { player.context.sendToast(it.toString()) }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        shape = shape,
                        colors = button_colours
                    ) {
                        Crossfade(cause_data_loading) { loading ->
                            if (loading) {
                                SubtleLoadingIndicator()
                            }
                            else {
                                Text(extra_button_text, softWrap = false)
                            }
                        }
                    }
                }

                if (isDebugBuild()) {
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

suspend fun uploadErrorToPasteEe(
    message: String,
    stack_trace: String,
    token: String,
    error: Throwable? = null,
    logs: String? = null,
): Result<String> = withContext(Dispatchers.IO) {
    val sections = mutableListOf(
        mapOf("name" to "MESSAGE", "contents" to message),
        mapOf("name" to "STACKTRACE", "contents" to stack_trace),
    )

    if (error is com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException) {
        val cause_data_result = error.getCauseData()
        val cause_data = cause_data_result.getOrNull() ?: return@withContext cause_data_result.cast()

        sections.add(
            mapOf(
                "name" to "DATA",
                "contents" to cause_data
            )
        )
    }

    if (logs != null) {
        sections.add(
            mapOf(
                "name" to "LOGS",
                "contents" to logs
            )
        )
    }

    val request = Request.Builder()
        .url("https://api.paste.ee/v1/pastes")
        .header("X-Auth-Token", token)
        .post(
            Gson().toJson(mapOf("sections" to sections))
                .toRequestBody("application/json".toMediaType())
        )
        .build()

    try {
        val result = OkHttpClient().newCall(request).execute()

        val gson = Gson()
        val response: Map<String, Any?> = result.use {
            gson.fromJson(it.body!!.charStream())
        }

        if (response["success"] != true || response["link"] == null) {
            return@withContext Result.failure(RuntimeException(gson.toJson(response)))
        }

        return@withContext Result.success(response["link"] as String)
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
}
