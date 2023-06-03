package com.spectre7.utils

// Originally based on https://github.com/mainrs/android-compose-furigana

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.*
import com.spectre7.spmp.model.SongLyrics

data class ReadingTextData (
    val text: String,
    val reading: String? = null,
    val data: Any? = null
)

fun calculateReadingsAnnotatedString(
    text_content: List<ReadingTextData>,
    show_readings: Boolean,
    font_size: TextUnit,
    textElement: @Composable (is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier) -> Unit
): List<Pair<AnnotatedString, Map<String, InlineTextContent>>> {
    val ret = mutableListOf<Pair<AnnotatedString, Map<String, InlineTextContent>>>()
    val inline_content = mutableMapOf<String, InlineTextContent>()
    var string = AnnotatedString.Builder()

    for (element in text_content.withIndex()) {
        annotateString(element.value, element.index, inline_content, string, show_readings, font_size, textElement)

        var first = true
        for (char in element.value.text) {
            if (char == '\n') {
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
    }

    return ret
}

@Composable
fun BasicFuriganaText(
    terms: List<SongLyrics.Term>,
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
                            Modifier.fillMaxHeight().padding(bottom = with(LocalDensity.current) { reading_font_size.toDp() }),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(furi, Modifier.wrapContentWidth(unbounded = true), fontSize = reading_font_size, color = text_colour)
                            Text(text, fontSize = font_size, color = text_colour)
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

private fun annotateString(
    elem: ReadingTextData,
    index: Int,
    inline_content: MutableMap<String, InlineTextContent>,
    string: AnnotatedString.Builder,
    show_readings: Boolean,
    font_size: TextUnit,
    textElement: @Composable (is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier) -> Unit
) {
    val text = elem.text.filterNot { it == '\n' }
    if (text.isEmpty()) {
        return
    }

    val reading = elem.reading
    val reading_font_size = font_size / 2

    val width = (text.length.toDouble() + (text.length - 1) * 0.05).em
    string.appendInlineContent(text, index.toString())

    inline_content[text] = InlineTextContent(
        placeholder = Placeholder(
            width = width,
            height = 1.97.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
        ),
        children = {
            val child_index = it.toInt()
            val box_height = with(LocalDensity.current) { reading_font_size.toDp() }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(modifier = Modifier.requiredHeight(box_height + 3.dp)) {
                    if (show_readings && reading != null) {
                        textElement(true, reading, reading_font_size, child_index, Modifier.wrapContentWidth(unbounded = true))
                    }
                }

                textElement(false, text, font_size, child_index, Modifier)
            }
        }
    )
}
