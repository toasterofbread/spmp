package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.util.composable.WidthShrinkText
import dev.toastbits.composekit.components.utils.modifier.background
import dev.toastbits.composekit.components.utils.modifier.disableParentScroll
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.model.JsonHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import SpMp.isDebugBuild
import dev.toastbits.composekit.theme.core.onAccent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_load_retry
import spmp.shared.generated.resources.wrap_text_switch_label
import spmp.shared.generated.resources.upload_to_paste_dot_ee
import spmp.shared.generated.resources.throw_error

const val ERROR_INFO_DISPLAY_DEFAULT_EXPANDED_HEIGHT_DP: Float = 500f

@Composable
fun ErrorInfoDisplay(
    error: Throwable?,
    show_throw_button: Boolean = isDebugBuild(),
    modifier: Modifier = Modifier,
    message: String? = null,
    pair_error: Pair<String, String>? = null,
    expanded_content_modifier: Modifier = Modifier.height(ERROR_INFO_DISPLAY_DEFAULT_EXPANDED_HEIGHT_DP.dp),
    disable_parent_scroll: Boolean = true,
    start_expanded: Boolean = false,
    getAccentColour: @Composable PlayerState.() -> Color = { theme.accent },
    extraButtonContent: (@Composable () -> Unit)? = null,
    onExtraButtonPressed: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)?
) {
    LaunchedEffect(error) {
        error?.printStackTrace()
    }

    if (error == null && pair_error == null) {
        return
    }

    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    var expanded: Boolean by remember { mutableStateOf(start_expanded) }
    val shape: Shape = RoundedCornerShape(20.dp)

    CompositionLocalProvider(LocalContentColor provides player.theme.background) {
        var width: Dp by remember { mutableStateOf(0.dp) }

        Column(
            modifier
                .animateContentSize()
                .background(getAccentColour(player), shape)
                .padding(horizontal = 10.dp)
                .onSizeChanged {
                    width = with (density) { it.width.toDp() }
                },
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
                        null,
                        tint = player.theme.onAccent
                    )
                }

                WidthShrinkText(
                    message ?: pair_error?.first ?: error!!::class.simpleName ?: error!!::class.toString(),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    style = LocalTextStyle.current.copy(color = player.theme.onAccent),
                    max_lines = 2
                )

                val button_colours = ButtonDefaults.buttonColors(
                    containerColor = player.theme.background,
                    contentColor = player.theme.onBackground
                )

                if (onExtraButtonPressed != null) {
                    Button(
                        onExtraButtonPressed,
                        shape = shape,
                        colors = button_colours
                    ) {
                        extraButtonContent!!.invoke()
                    }
                }

                if (onRetry != null) {
                    Button(
                        onRetry,
                        shape = shape,
                        colors = button_colours
                    ) {
                        Text(stringResource(Res.string.action_load_retry))
                    }
                }

                if (onDismiss != null) {
                    ShapedIconButton(
                        onDismiss,
                        shape = shape,
                        colours = IconButtonDefaults.iconButtonColors(
                            containerColor = player.theme.background,
                            contentColor = player.theme.onBackground
                        )
                    ) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }

            AnimatedVisibility(
                expanded,
                Modifier.requiredWidth(width),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandedContent(error, pair_error, shape, disable_parent_scroll, show_throw_button, expanded_content_modifier)
            }
        }
    }
}

@Composable
private fun LongTextDisplay(text: String, wrap_text: Boolean, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val limited_text: String = remember(text) { text.take(10000) }

    SelectionContainer {
        Text(
            limited_text,
            modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 50.dp),
            color = player.theme.onBackground,
            softWrap = wrap_text
        )
    }
}

@Composable
private fun ExpandedContent(
    error: Throwable?,
    pair_error: Pair<String, String>?,
    shape: Shape,
    disable_parent_scroll: Boolean,
    show_throw_button: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutine_scope = rememberCoroutineScope()
    val player = LocalPlayerState.current

    var text_to_show: String? by remember { mutableStateOf(null) }
    var wrap_text by remember { mutableStateOf(false) }
    val button_colours = ButtonDefaults.buttonColors(
        containerColor = player.theme.accent,
        contentColor = player.theme.onAccent
    )

    var current_error: Throwable? by remember(error) { mutableStateOf(error) }

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .padding(bottom = 10.dp)
            .background({ player.theme.background })
            .padding(10.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides player.theme.onBackground) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(Res.string.wrap_text_switch_label),
                        color = player.theme.onBackground
                    )
                    Switch(wrap_text, { wrap_text = !wrap_text }, Modifier.padding(end = 10.dp))

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    Button(
                        {
                            coroutine_scope.launch {
                                uploadErrorToPasteEe(
                                    (current_error?.message ?: pair_error?.first).toString(),
                                    (current_error?.stackTraceToString() ?: pair_error?.second).toString(),
                                    ProjectBuildConfig.PASTE_EE_TOKEN,
                                    error = current_error
                                ).fold(
                                    { text_to_show = it },
                                    { current_error = it }
                                )
                            }
                        },
                        colors = button_colours,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(stringResource(Res.string.upload_to_paste_dot_ee), textAlign = TextAlign.Center, style = LocalTextStyle.current.copy(color = player.theme.onAccent), softWrap = false)
                    }
                }

                Crossfade(text_to_show ?: current_error?.stackTraceToString() ?: pair_error!!.second) { text ->
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
                if (show_throw_button && current_error != null) {
                    Button(
                        { current_error?.also { throw it } },
                        colors = button_colours
                    ) {
                        Text(stringResource(Res.string.throw_error))
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
): Result<String> = runCatching {
    val sections: MutableList<Map<String, String>> =
        mutableListOf(
            mapOf(
                "name" to "VERSION",
                "contents" to
                    if (ProjectBuildConfig.GIT_TAG != null) "Tag: ${ProjectBuildConfig.GIT_TAG}"
                    else "Commit: ${ProjectBuildConfig.GIT_COMMIT_HASH}"
            ),
            mapOf("name" to "MESSAGE", "contents" to message),
            mapOf("name" to "STACKTRACE", "contents" to stack_trace)
        )

    if (logs != null) {
        sections.add(
            mapOf(
                "name" to "LOGS",
                "contents" to logs
            )
        )
    }

    val response: HttpResponse =
        JsonHttpClient.post("https://api.paste.ee/v1/pastes") {
            headers {
                append("X-Auth-Token", token)
            }
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(mapOf("sections" to sections)))
        }

    val data: NewPasteResponse = response.body()
    return@runCatching data.link
}

@Serializable
data class NewPasteResponse(val link: String)
