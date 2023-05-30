package com.spectre7.utils

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.*
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.utils.composable.MeasureUnconstrainedView
import com.spectre7.utils.composable.SubtleLoadingIndicator

data class TextData (
    val text: String,
    val reading: String? = null,
    val data: Any? = null
)

private fun calculateAnnotatedString(
    text_content: List<TextData>,
    show_readings: Boolean,
    font_size: TextUnit,
    text_element: (@Composable (is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier) -> Unit)?,
    receiveTermRect: ((TextData, Rect) -> Unit)?
): List<Pair<AnnotatedString, Map<String, InlineTextContent>>> {
    val reading_font_size = font_size / 2

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
            placeholder = Placeholder(
                width = width,
                height = 1.97.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
            ),
            children = {
                val index = it.toInt()
                val box_height = with(LocalDensity.current) { reading_font_size.toDp() }

                val TextElement = text_element ?: { _, text, font_size, index, modifier ->
                    Text(
                        text,
                        modifier,
                        fontSize = font_size
                    )
                }

                MeasureUnconstrainedView({
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        TextElement(false, text, font_size, index, Modifier)
                    }
                }) { width: Int, _height: Int ->
                    val column_modifier = remember(receiveTermRect != null) {
                        Modifier.fillMaxSize().run {
                            if (receiveTermRect != null) {
                                onPlaced { coords ->
                                    receiveTermRect(text_content[index],
                                        Rect(
                                            coords.localPositionOf(
                                                coords.parentCoordinates!!,
                                                coords.positionInRoot()
                                            ),
                                            Size(width.toFloat(), 70f)
                                        )
                                    )
                                }
                            }
                            else {
                                this
                            }
                        }
                    }

                    Column(
                        modifier = column_modifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(modifier = Modifier.requiredHeight(box_height + 3.dp)) {
                            if (show_readings && reading != null) {
                                TextElement(true, reading, reading_font_size, index, Modifier.wrapContentWidth(unbounded = true))
                            }
                        }

                        TextElement(false, text, font_size, index, Modifier)
                    }
                }

            }
        )
    }

    val ret = mutableListOf<Pair<AnnotatedString, Map<String, InlineTextContent>>>()

    val inline_content = mutableMapOf<String, InlineTextContent>()
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

@Composable
fun BasicFuriganaText(
    terms: List<Song.Lyrics.Term>,
    show_readings: Boolean = true,
    font_size: TextUnit = LocalTextStyle.current.fontSize,
    text_colour: Color = LocalContentColor.current
) {
    val reading_font_size = font_size / 2
    val line_height = with(LocalDensity.current) { (font_size.value + (reading_font_size.value * 2)).sp.toDp() }

    val string_builder = AnnotatedString.Builder()
    val inline_content: MutableMap<String, InlineTextContent> = mutableMapOf()

    for (term in terms) {
        for (subterm in term.subterms) {
            if (subterm.furi == null || !show_readings) {
                string_builder.append(subterm.text)
            }
            else {
                val furi = subterm.furi!!
                string_builder.appendInlineContent(subterm.text, furi)

                inline_content.putIfAbsent(subterm.text) {
                    val text = subterm.text
                    val width = (text.length.toDouble() + (text.length - 1) * 0.05).em

                    InlineTextContent(
                        placeholder = Placeholder(
                            width = width,
                            height = (font_size.value + (reading_font_size.value * 2)).sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        )
                    ) { furi ->
                        Column(
                            Modifier.fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(furi, Modifier.wrapContentWidth(unbounded = true), fontSize = reading_font_size, color = text_colour)
                            Text(text, fontSize = font_size, color = text_colour)
                            Text("", fontSize = reading_font_size) // Add spacing at bottom to keep main text centered
                        }
                    }
                }
            }
        }
    }

    Box(Modifier.height(line_height), contentAlignment = Alignment.CenterStart) {
        Text(
            string_builder.toAnnotatedString(),
            inlineContent = inline_content,
            color = text_colour
        )
    }
}

@Composable
fun LongFuriganaText(
    text_content: List<TextData>,
    show_readings: Boolean = true,
    line_spacing: Dp = 0.dp,
    space_wrapped_lines: Boolean = true,
    column_modifier: Modifier = Modifier,
    font_size: TextUnit = TextUnit.Unspecified,
    receiveTermRect: ((TextData, Rect) -> Unit)? = null,
    chunk_size: Int = 1,

    text_element: (@Composable (is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier) -> Unit)? = null,
    list_element: (@Composable (content: LazyListScope.() -> Unit) -> Unit)? = null,
    loadingIndicator: @Composable () -> Unit = { SubtleLoadingIndicator() }
) {
    val _font_size = if (font_size == TextUnit.Unspecified) LocalTextStyle.current.fontSize else font_size
    val reading_font_size = _font_size / 2

    var data_with_readings: List<Pair<AnnotatedString, Map<String, InlineTextContent>>>? by remember(text_content) { mutableStateOf(null) }
    var data_without_readings: List<Pair<AnnotatedString, Map<String, InlineTextContent>>>? by remember(text_content) { mutableStateOf(null) }

    LaunchedEffect(text_content) {
        data_with_readings = null
        data_without_readings = null
        
        if (show_readings) {
            data_with_readings = calculateAnnotatedString(text_content, true, _font_size, text_element, receiveTermRect)
            data_without_readings = calculateAnnotatedString(text_content, false, _font_size, text_element, receiveTermRect)
        }
        else {
            data_without_readings = calculateAnnotatedString(text_content, false, _font_size, text_element, receiveTermRect)
            data_with_readings = calculateAnnotatedString(text_content, true, _font_size, text_element, receiveTermRect)
        }
    }

    Crossfade(if (show_readings) data_with_readings else data_without_readings) { text_data ->
        if (text_data == null) {
            loadingIndicator()
        }
        else {
            var text_line_height = _font_size.value + reading_font_size.value + 10
            if (space_wrapped_lines) {
                text_line_height += with(LocalDensity.current) { line_spacing.toSp() }.value
            }

            (list_element ?: { content ->
                LazyColumn(column_modifier) {
                    content()
                }
            }) {
                val chunks = (text_data.size / chunk_size).coerceAtLeast(1)

                for (chunk in 0 until chunks) {
                    items(chunks, { it }) { i ->
                        for (line in i + (chunk * chunk_size) until i + ((chunk + 1) * chunk_size)) {
                            if (line >= text_data.size) {
                                break
                            }
                            val item = text_data[line]

                            if (line > 0) {
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

//                val chunks = (text_data.size / chunk_size).coerceAtLeast(1)
//
//                for (chunk in 0 until chunks) {
//                    items(chunks, { it }) { i ->
//                        for (line in i + (chunk * chunk_size) until i + ((chunk + 1) * chunk_size)) {
//                            if (line >= text_data.size) {
//                                break
//                            }
//                            val item = text_data[line]
//
//                            if (line > 0) {
//                                Spacer(Modifier.requiredHeight(line_spacing))
//                            }
//
//                            Text(
//                                item.first,
//                                fontSize = _font_size,
//                                inlineContent = item.second,
//                                lineHeight = text_line_height.sp
//                            )
//                        }
//                    }
//                }
            }
        }
    }
}
