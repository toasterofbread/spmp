package com.spectre7.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.*
import net.zerotask.libraries.android.compose.furigana.TermInfo
import net.zerotask.libraries.android.compose.furigana.TextData

@Composable
fun LongFuriganaText(
    text_content: List<TextData>,
    show_readings: Boolean = true,
    line_spacing: Dp = 0.dp,
    space_wrapped_lines: Boolean = true,
    column_modifier: Modifier = Modifier,
    font_size: TextUnit = TextUnit.Unspecified,
    text_positions: MutableList<TermInfo>? = null,
    highlight_term_range: IntRange? = null,

    text_element: (@Composable (is_reading: Boolean, text: String, font_size: TextUnit, modifier: Modifier) -> Unit)? = null,
    list_element: (@Composable (content: LazyListScope.() -> Unit) -> Unit)? = null,
) {

    val _font_size = if (font_size == TextUnit.Unspecified) LocalTextStyle.current.fontSize else font_size
    val reading_font_size = _font_size / 2

    var child_index = remember { 0 }

    fun calculateAnnotatedString(show_readings: Boolean): List<Pair<AnnotatedString, Map<String, InlineTextContent>>> {

        fun annotateString(elem: TextData, index: Int, inline_content: MutableMap<String, InlineTextContent>, string: AnnotatedString.Builder) {
            val text = elem.text.filterNot { it == '\n' }
            val reading = elem.reading

            if (text.isEmpty()) {
                return
            }

            // Words larger than one character/kanji need a small amount of additional space in their
            // x-dimension.
            val width = (text.length.toDouble() + (text.length - 1) * 0.05).em
            string.appendInlineContent(text, index.toString())

            inline_content[text] = InlineTextContent(
                // TODO: find out why height and width need magic numbers.
                placeholder = Placeholder(
                    width = width,
                    height = 1.97.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
                ),
                children = {
                    val index = it.toInt()
                    val box_height = with(LocalDensity.current) { reading_font_size.toDp() }

//                    val term_index = remember { child_index++ }
//                    val term = remember { text_positions?.getOrNull(term_index) }

                    val TextElement = text_element ?: { _, text, font_size, modifier ->
                        Text(
                            text,
                            modifier = modifier,
                            fontSize = font_size
                        )
                    }

                    net.zerotask.libraries.android.compose.furigana.MeasureUnconstrainedView({
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            TextElement(false, text, font_size, Modifier)
                        }
                    }) { width: Int, _height: Int ->
                        val column_modifier = remember(text_positions == null) {
                            Modifier.fillMaxSize().run {
//                                if (text_positions != null) {
//                                    onPlaced { coords ->
//                                        if (term != null && term.rect.isEmpty) {
//                                            term.rect = Rect(coords.positionInRoot(), Size(width.toFloat(), 70f))
//                                        }
//                                    }
//                                }
//                                else {
                                    this
//                                }
                            }
                        }

                        Column(
                            modifier = column_modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(modifier = Modifier.requiredHeight(box_height + 3.dp)) {
                                if (show_readings && reading != null) {
                                    TextElement(true, reading, reading_font_size, Modifier.wrapContentWidth(unbounded = true))
                                }
                            }

                            println("RECOMP $index")

                            TextElement(false, text, font_size, if (highlight_term_range?.contains(index) == true) Modifier.background(Color.Green, CircleShape) else Modifier)
                        }
                    }

                }
            )
        }

        val ret = mutableListOf<Pair<AnnotatedString, Map<String, InlineTextContent>>>()

        var inline_content = mutableMapOf<String, InlineTextContent>()
        var string = AnnotatedString.Builder()

        for (element in text_content.withIndex()) {
            annotateString(element.value, element.index, inline_content, string)

            var first = true
            for (char in element.value.text) {
                if (char != '\n') {
                    continue
                }

                if (first) {
                    first = false
                }
                else {
                    string.appendInlineContent("\n", "\n")
                }

                ret.add(string.toAnnotatedString() to inline_content)
                string = AnnotatedString.Builder()
            }
        }

        return ret
    }

    val data_with_readings = remember(text_content, highlight_term_range) {
        if (text_positions != null) {
            text_positions.clear()
            for (elem in text_content) {
                text_positions.add(TermInfo(elem.text, elem.data))
            }
        }
        calculateAnnotatedString(true)
    }

    val data_without_readings = remember(text_content) {
        calculateAnnotatedString(false)
    }

    val text_data = if (show_readings) data_with_readings else data_without_readings

    var text_line_height = _font_size.value + reading_font_size.value + 10
    if (space_wrapped_lines) {
        text_line_height += with(LocalDensity.current) { line_spacing.toSp() }.value
    }

    (list_element ?: { content ->
        LazyColumn(column_modifier) {
            content()
        }
    }) {
        items(text_data.size) { i ->
            val item = text_data[i]

            if (i > 0) {
                Spacer(Modifier.requiredHeight(line_spacing))
            }

            Text(
                item.first,
                fontSize = _font_size,
                inlineContent = item.second,
                lineHeight = text_line_height.sp
            )
        }
    }
}
