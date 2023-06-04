package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.utils.AnnotatedReadingTerm

@Composable
fun LyricsSyncMenu(
    song: Song,
    lyrics: SongLyrics,
    line: AnnotatedReadingTerm,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            line.annotated_string,
            inlineContent = line.inline_content,
            style = getLyricsTextStyle(20.sp)
        )
    }
}
