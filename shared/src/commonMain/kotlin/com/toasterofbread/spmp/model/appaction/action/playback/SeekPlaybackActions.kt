package com.toasterofbread.spmp.model.appaction.action.playback

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import kotlin.random.Random
import kotlin.random.nextInt
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_config_playback_seek_to_time_seek_ms

private const val SEEK_AMOUNT_STEP_MS: Long = 500

@Serializable
data class SeekByTimePlaybackAppAction(
    val seek_ms: Long = 5000
): PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.SEEK_BY_TIME

    override suspend fun execute(player: PlayerState) {
        if (seek_ms == 0L) {
            return
        }

        player.withPlayer{
            seekBy(seek_ms)
        }
    }

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (PlaybackAction) -> Unit) {
        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.appaction_config_playback_seek_to_time_seek_ms),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ onModification(copy(seek_ms = seek_ms - SEEK_AMOUNT_STEP_MS)) }) {
                    Icon(Icons.Default.Remove, null)
                }

                Text(seek_ms.toString() + "ms")

                IconButton({ onModification(copy(seek_ms = seek_ms + SEEK_AMOUNT_STEP_MS)) }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    }
}

@Serializable
class SeekNextPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.SEEK_NEXT

    override suspend fun execute(player: PlayerState) {
        player.withPlayer{
            seekToNext()
        }
    }
}

@Serializable
class SeekPreviousPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.SEEK_PREVIOUS

    override suspend fun execute(player: PlayerState) {
        player.withPlayer{
            seekToPrevious()
        }
    }
}

@Serializable
class SeekRandomPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.SEEK_RANDOM

    override suspend fun execute(player: PlayerState) {
        player.withPlayer{
            seekToSong(Random.nextInt(0 until player.status.m_song_count))
        }
    }
}

@Serializable
class UndoSeekPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.UNDO_SEEK

    override suspend fun execute(player: PlayerState) {
        player.withPlayer {
            undoSeek()
        }
    }
}
