package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.spmp.model.mediaitem.Song

@Composable
fun LyricsSyncMenu(
    song: Song,
    lyrics: SongLyrics,
    line_index: Int,
    modifier: Modifier = Modifier
) {
    val line = lyrics.lines[line_index]
    TODO()
}