package com.toasterofbread.spmp.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.lyrics.SongLyrics

@Composable
fun HorizontalFuriganaText(
    terms: List<SongLyrics.Term>,
    modifier: Modifier = Modifier,
    show_readings: Boolean = true,
    // max_lines: Int = 1,
    // preallocate_needed_space: Boolean = false,
    style: TextStyle = LocalTextStyle.current,
    reading_style: TextStyle = remember(style) {
        style.copy(
            fontSize = style.fontSize / 2,
            lineHeight = style.lineHeight / 2
        )
    }
) {
    FlowRow(
        modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalArrangement = when (style.textAlign) {
            TextAlign.Center -> Arrangement.Center
            TextAlign.End -> Arrangement.End
            else -> Arrangement.Start
        },
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
                        Modifier.padding(bottom = 3.dp),
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
