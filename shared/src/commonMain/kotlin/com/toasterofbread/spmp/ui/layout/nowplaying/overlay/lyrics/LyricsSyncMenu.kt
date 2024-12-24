package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.ui.component.HorizontalFuriganaText
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.util.thenIf
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lyrics_sync_press_button_when_line_begins

private const val SONG_SEEK_MS: Long = 5000L
private val SYNC_MENU_LYRICS_SHOW_RANGE: IntRange = -3 .. 0

private fun List<SongLyrics.Term>.getLineRange(): LongRange = first().line_range!!

@Composable
fun LyricsSyncMenu(
    song: Song,
    lyrics: SongLyrics,
    line_index: Int,
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    require(lyrics.synced)

    val player: PlayerState = LocalPlayerState.current
    val service: PlayerService? = player.controller

    LaunchedEffect(line_index) {
        service?.seekToTime(
            lyrics.lines[line_index].getLineRange().first - SONG_SEEK_MS
        )
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (line_offset in SYNC_MENU_LYRICS_SHOW_RANGE) {
            val line: List<SongLyrics.Term> = lyrics.lines.getOrNull(line_index + line_offset) ?: continue

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
                    Text(stringResource(Res.string.lyrics_sync_press_button_when_line_begins))
                }

                HorizontalFuriganaText(
                    line,
                    Modifier.thenIf(line_offset != 0) {
                        alpha(0.65f)
                    },
                    style = getLyricsTextStyle(20.sp)
                )

                if (line_offset == 0) {
                    PlayerControls(service) {
                        val current_time: Long = service?.current_position_ms ?: return@PlayerControls
                        player.database.songQueries
                            .updateLyricsSyncOffsetById(
                                lyrics.lines[line_index].getLineRange().first - current_time,
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
private fun PlayerControls(service: PlayerService?, onSelected: () -> Unit) {
    val button_modifier = Modifier.size(40.dp)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton({ service?.service_player?.seekBy(-SONG_SEEK_MS) }) {
            Icon(Icons.Default.Replay5, null, button_modifier)
        }

        IconButton(onSelected) {
            Icon(Icons.Default.Done, null, button_modifier)
        }

        IconButton({ service?.service_player?.seekBy(SONG_SEEK_MS) }) {
            Icon(Icons.Default.Forward5, null, button_modifier)
        }
    }
}
