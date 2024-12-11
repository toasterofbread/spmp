package com.toasterofbread.spmp.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import dev.toastbits.composekit.util.*
import com.toasterofbread.spmp.model.lyrics.SongLyrics

private const val BRACKET_CHARS: String = "「」[](){}<>"

@Composable
fun VerticalFuriganaText(
    terms: List<SongLyrics.Term>,
    modifier: Modifier = Modifier,
    show_readings: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
    reading_style: TextStyle = remember(style) {
        style.copy(
            fontSize = style.fontSize / 2,
            lineHeight = style.lineHeight / 2
        )
    }
) {
    val display_vertically: Boolean = remember(terms) {
        terms.any { term ->
            term.subterms.any { subterm ->
                subterm.text.any { it.isJa() }
            }
        }
    }

    if (display_vertically) {
        val density: Density = LocalDensity.current
        var largest_text_width: Dp by remember(terms) { mutableStateOf(0.dp) }

        Column(modifier, horizontalAlignment = Alignment.Start) {
            for (term in terms) {
                for (subterm in term.subterms) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        VerticalText(
                            subterm.text,
                            style,
                            Modifier
                                .widthIn(min = largest_text_width)
                                .onSizeChanged {
                                    val width: Dp = with (density) { it.width.toDp() }
                                    if (width > largest_text_width) {
                                        largest_text_width = width
                                    }
                                }
                        )

                        val reading: String? = subterm.reading
                        if (show_readings && reading != null) {
                            VerticalText(reading, reading_style)
                        }
                    }
                }
            }
        }
    }
    else {
        FlowRow(
            modifier,
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = when (style.textAlign) {
                TextAlign.Center -> Arrangement.Center
                TextAlign.End -> Arrangement.End
                else -> Arrangement.Start
            }
            // maxLines = max_lines
        ) {
            for (term in terms.flatMap { it.subterms }) {
                Column(
                    Modifier.alignBy(LastBaseline),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val reading: String? = if (show_readings) term.reading else null

                    if (reading != null) {
                        Text(
                            reading,
                            softWrap = false,
                            style = reading_style,
                            lineHeight = reading_style.lineHeight
                        )
                    }

                    Text(
                        term.text,
                        softWrap = reading == null,
                        style = style,
                        lineHeight = style.lineHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        for (char in text) {
            val is_bracket: Boolean = BRACKET_CHARS.contains(char)
            Text(
                char.toString(),
                style = style,
                modifier = Modifier.thenIf(is_bracket) { rotate(90f) },
                textAlign = TextAlign.Center
            )
        }
    }
}
