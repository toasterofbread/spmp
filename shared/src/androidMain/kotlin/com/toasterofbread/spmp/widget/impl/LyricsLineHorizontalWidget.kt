package com.toasterofbread.spmp.widget.impl

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import com.toasterofbread.spmp.ui.util.LyricsLineState

internal class LyricsLineHorizontalWidget: LyricsWidget() {
    @Composable
    override fun LyricsContent(lyrics: LyricsLineState, modifier: GlanceModifier) {
        lyrics.LyricsDisplay { show, line ->
            if (show && line != null) {
                LyricsLine(line)
            }
        }
    }
}
