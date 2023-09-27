package com.toasterofbread.utils.common

// Originally based on https://github.com/mainrs/android-compose-furigana

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.SongLyrics

data class ReadingTextData(
    val text: String,
    val reading: String? = null,
    val data: Any? = null
)

data class AnnotatedReadingTerm(
    val annotated_string: AnnotatedString,
    val inline_content: Map<String, InlineTextContent>,
    val text_data: ReadingTextData
)

fun calculateReadingsAnnotatedString(
    text_content: List<ReadingTextData>,
    show_readings: Boolean,
    text_style: TextStyle,
    textElement: @Composable (
        is_reading: Boolean,
        text: String,
        text_style: TextStyle,
        index: Int,
        modifier: Modifier,
        getLine: () -> Pair<Int, List<AnnotatedReadingTerm>>
    ) -> Unit,
    getLine: (term_index: Int) -> Int
): List<AnnotatedReadingTerm> {
    val ret = mutableListOf<AnnotatedReadingTerm>()
    val inline_content = mutableMapOf<String, InlineTextContent>()
    var string = AnnotatedString.Builder()

    for (element in text_content.withIndex()) {
        annotateString(element.value, element.index, inline_content, string, show_readings, text_style, textElement) { Pair(getLine(it), ret) }

        var first = true
        for (char in element.value.text) {
            if (char == '\n') {
                if (first) {
                    first = false
                }
                else {
                    string.appendInlineContent("\n", "\n")
                }

                ret.add(AnnotatedReadingTerm(string.toAnnotatedString(), inline_content, element.value))
                string = AnnotatedString.Builder()
            }
        }
    }

    return ret
}

@Composable
fun BasicFuriganaText(
    terms: List<SongLyrics.Term>,
    show_readings: Boolean = true,
    max_lines: Int = 1,
    font_size: TextUnit = LocalTextStyle.current.fontSize,
    text_colour: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current
) {
    val reading_font_size = font_size / 2
    val line_height = with(LocalDensity.current) { (font_size.value + (reading_font_size.value * 2)).sp.toDp() }

    var annotated_string: AnnotatedString by remember { mutableStateOf(AnnotatedString("")) }
    var inline_content: Map<String, InlineTextContent> by remember { mutableStateOf(emptyMap()) }

    val density = LocalDensity.current
    var width: Dp by remember { mutableStateOf(Dp.Unspecified) }
    val height: Dp by animateDpAsState(line_height * max_lines)

    LaunchedEffect(terms, max_lines) {
        val string_builder = AnnotatedString.Builder()
        val content: MutableMap<String, InlineTextContent> = mutableMapOf()

        for (term in terms) {
            for (subterm in term.subterms) {
                string_builder.appendInlineContent(subterm.text)

                content.putIfAbsent(subterm.text) {
                    getLyricsInlineTextContent(
                        subterm.text, subterm.furi, show_readings, font_size, reading_font_size
                    ) { is_reading, text, alternate_text, font_size, modifier ->
                        Text(
                            text,
                            modifier.widthIn(max = width),
                            fontSize = font_size,
                            color = text_colour,
                            softWrap = true,
                            overflow = TextOverflow.Visible,
                            maxLines = max_lines
                        )
                    }
                }
            }
        }

        annotated_string = string_builder.toAnnotatedString()
        inline_content = content
    }

    Box(
        Modifier.requiredHeight(height).fillMaxWidth().onSizeChanged {
            width = with(density) { it.width.toDp() }
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            annotated_string,
            inlineContent = inline_content,
            color = text_colour,
            style = style,
            maxLines = max_lines,
            overflow = TextOverflow.Visible
        )
    }
}

private fun annotateString(
    elem: ReadingTextData,
    index: Int,
    inline_content: MutableMap<String, InlineTextContent>,
    string: AnnotatedString.Builder,
    show_readings: Boolean,
    text_style: TextStyle,
    textElement: @Composable (
        is_reading: Boolean,
        text: String,
        text_style: TextStyle,
        index: Int,
        modifier: Modifier,
        getLine: () -> Pair<Int, List<AnnotatedReadingTerm>>,
    ) -> Unit,
    getLine: (term_index: Int) -> Pair<Int, List<AnnotatedReadingTerm>>
) {
    val text = elem.text.filterNot { it == '\n' }
    if (text.isEmpty()) {
        return
    }

    val reading = elem.reading
    val reading_font_size = text_style.fontSize / 2

    string.appendInlineContent(text, index.toString())

    inline_content[text] = getLyricsInlineTextContent(
        text, reading, show_readings, text_style.fontSize, reading_font_size
    ) { is_reading, text, alternate_text, font_size, modifier ->
        val child_index = alternate_text.toInt()
        textElement(is_reading, text, text_style.copy(fontSize = font_size), child_index, modifier) {
            getLine(child_index)
        }
    }
}

private fun getLyricsInlineTextContent(
    text: String,
    reading: String?,
    show_readings: Boolean,
    font_size: TextUnit,
    reading_font_size: TextUnit,
    textElement: @Composable (is_reading: Boolean, text: String, alternate_text: String, font_size: TextUnit, modifier: Modifier) -> Unit
): InlineTextContent {
    return InlineTextContent(
        placeholder = Placeholder(
            width = (text.length.toDouble() + (text.length - 1) * 0.05).em,
            height = (font_size.value + (reading_font_size.value * 2)).sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom
        ),
        children = { alternate_text ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (show_readings && reading != null) {
                    textElement(
                        true,
                        reading,
                        alternate_text,
                        reading_font_size,
                        Modifier
                            .wrapContentWidth(unbounded = true)
                            .offset(
                                y = with(LocalDensity.current) {
                                    (font_size.toDp() * -0.5f) - reading_font_size.toDp() + 3.dp
                                }
                            )
                    )
                }

                textElement(false, text, alternate_text, font_size, Modifier)
            }
        }
    )
}
