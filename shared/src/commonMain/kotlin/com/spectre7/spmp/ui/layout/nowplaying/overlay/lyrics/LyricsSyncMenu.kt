package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.PlayerService
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.utils.AnnotatedReadingTerm

private const val SONG_SEEK_MS = 5000L
private val SYNC_MENU_LYRICS_SHOW_RANGE = -1 .. 0

fun AnnotatedReadingTerm.getLineRange(): LongRange =
    (text_data.data as SongLyrics.Term).line_range!!

@Composable
fun LyricsSyncMenu(
    song: Song,
    lyrics: SongLyrics,
    line_index: Int,
    lines: List<AnnotatedReadingTerm>,
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    require(lyrics.synced)

    val player = LocalPlayerState.current.player

    LaunchedEffect(line_index) {
        player.seekTo(
            lines[line_index].getLineRange().first - SONG_SEEK_MS
        )
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(getStringTODO("Press the center button when this line begins"))
        
        for (line in SYNC_MENU_LYRICS_SHOW_RANGE) {
            Text(
                lines[line_index + line].annotated_string,
                inlineContent = line.inline_content,
                style = getLyricsTextStyle(20.sp)
            )
        }

        PlayerControls(player) {
            val current_time: Long = player.current_position_ms
            song.song_reg_entry.lyrics_sync_offset = (line.getLineRange().first - current_time).toInt()

            song.saveRegistry()
            close()
        }
    }
}

@Composable
private fun PlayerControls(player: PlayerService, onSelected: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton({ player.seekBy(-SONG_SEEK_MS) }) {
            Icon(Icons.Default.Replay5, null)
        }

        IconButton(onSelected) {
            Icon(Icons.Default.CheckCircle, null)
        }

        IconButton({ player.seekBy(SONG_SEEK_MS) }) {
            Icon(Icons.Default.Forward5, null)
        }
    }
}
