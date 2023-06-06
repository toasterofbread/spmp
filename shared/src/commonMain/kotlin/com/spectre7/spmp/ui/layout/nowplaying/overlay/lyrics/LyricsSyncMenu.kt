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
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    require(lyrics.synced)

    val player = LocalPlayerState.current.player

    LaunchedEffect(line) {
        player.pause()
        player.seekBy(-500)
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(getStringTODO("Press the center button when this line begins"))
        
        Text(
            line.annotated_string,
            inlineContent = line.inline_content,
            style = getLyricsTextStyle(20.sp)
        )

        PlayerControls(player) {
            val current_time: Long = player.current_position_ms
            val line_range: LongRange = (line.text_data.data as SongLyrics.Term).line_range!!

            song.song_reg_entry.lyrics_sync_offset = long_range.start - current_time
            song.saveRegistry()

            close()
        }
    }
}

@Composable
private fun PlayerControls(player: PlayerService, onSelected: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton({ player.seekBy(-500) }) {
            Icon(Icons.Default.Rewind5, null)
        }

        IconButton(onSelected) {
            Icon(Icons.Default.CheckCircle, null)
        }

        IconButton({ player.seekBy(500) }) {
            Icon(Icons.Default.Forward5)
        }
    }
}
