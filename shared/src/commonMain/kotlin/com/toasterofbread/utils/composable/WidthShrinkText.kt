package com.toasterofbread.utils.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WidthShrinkText(
    string: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    inline_content: Map<String, InlineTextContent> = mapOf(),
    alignment: Alignment = Alignment.TopStart
) {
	var text_style by remember(style) { mutableStateOf(style) }
	var text_style_large: TextStyle? by remember(style) { mutableStateOf(null) }
	var ready_to_draw by remember { mutableStateOf(false) }

	val delta = 0.05

    Box(modifier, contentAlignment = alignment) {
        Text(
            string,
            Modifier.fillMaxWidth().drawWithContent { if (ready_to_draw) drawContent() },
            maxLines = 1,
            softWrap = false,
            style = text_style,
            inlineContent = inline_content,
            onTextLayout = { layout_result ->
                if (!layout_result.didOverflowWidth) {
                    ready_to_draw = true
                    text_style_large = text_style
                } else {
                    text_style = text_style.copy(fontSize = text_style.fontSize * (1.0 - delta))
                }
            }
        )

        text_style_large?.also {
            Text(
                string,
                Modifier.fillMaxWidth().drawWithContent {}.requiredHeight(1.dp),
                maxLines = 1,
                softWrap = false,
                style = it,
                inlineContent = inline_content,
                onTextLayout = { layout_result ->
                    if (!layout_result.didOverflowWidth) {
                        text_style_large = it.copy(fontSize = minOf(style.fontSize.value, it.fontSize.value * (1.0f + delta.toFloat())).sp)
                        text_style = it
                    }
                }
            )
        }
    }
}

@Composable
fun WidthShrinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    alignment: Alignment = Alignment.TopStart
) {
    WidthShrinkText(
        AnnotatedString(text),
        modifier,
        style,
        alignment = alignment
    )
}

@Composable
fun WidthShrinkText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    colour: Color = LocalContentColor.current,
    alignment: Alignment = Alignment.TopStart
) {
    WidthShrinkText(
        text,
        modifier,
        LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = fontWeight, color = colour),
        alignment
    )
}
