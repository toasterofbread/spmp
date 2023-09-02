package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import SpMp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playerservice.PlayerService
import com.toasterofbread.utils.AnnotatedReadingTerm
import com.toasterofbread.utils.setAlpha

private const val SONG_SEEK_MS = 5000L
private val SYNC_MENU_LYRICS_SHOW_RANGE = -3 .. 0

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
        player?.seekTo(
            lines[line_index].getLineRange().first - SONG_SEEK_MS
        )
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (line_offset in SYNC_MENU_LYRICS_SHOW_RANGE) {
            val line = lines.getOrNull(line_index + line_offset) ?: continue

            if (line_offset == 0) {
                Spacer(Modifier.height(15.dp))
            }

            Column(
                if (line_offset == 0) Modifier
                    .border(1.dp, LocalContentColor.current, RoundedCornerShape(16.dp))
                    .padding(10.dp)
                    .fillMaxWidth(0.9f)
                else Modifier,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (line_offset == 0) {
                    Text(getString("lyrics_sync_press_button_when_line_begins"))
                }

                CompositionLocalProvider(
                    LocalContentColor provides LocalContentColor.current
                        .setAlpha(if (line_offset == 0) 0f else 0.1f)
                ) {
                    Text(
                        line.annotated_string,
                        inlineContent = line.inline_content,
                        style = getLyricsTextStyle(20.sp)
                    )
                }

                if (line_offset == 0) {
                    PlayerControls(player) {
                        val current_time: Long = player?.current_position_ms ?: return@PlayerControls
                        SpMp.context.database.songQueries
                            .updateLyricsSyncOffsetById(
                                lines[line_index].getLineRange().first - current_time,
                                song.id
                            )
                        close()
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerControls(player: PlayerService?, onSelected: () -> Unit) {
    val button_modifier = Modifier.size(40.dp)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton({ player?.seekBy(-SONG_SEEK_MS) }) {
            Icon(Icons.Default.Replay5, null, button_modifier)
        }

        IconButton(onSelected) {
            Icon(Icons.Default.Done, null, button_modifier)
        }

        IconButton({ player?.seekBy(SONG_SEEK_MS) }) {
            Icon(Icons.Default.Forward5, null, button_modifier)
        }
    }
}
