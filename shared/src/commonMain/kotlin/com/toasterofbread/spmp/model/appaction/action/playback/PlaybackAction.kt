package com.toasterofbread.spmp.model.appaction.action.playback

import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_playback_action_toggle_play
import spmp.shared.generated.resources.appaction_playback_action_play
import spmp.shared.generated.resources.appaction_playback_action_pause
import spmp.shared.generated.resources.appaction_playback_action_shuffle_queue
import spmp.shared.generated.resources.appaction_playback_action_clear_queue
import spmp.shared.generated.resources.appaction_playback_action_seek_by_time
import spmp.shared.generated.resources.appaction_playback_action_seek_next
import spmp.shared.generated.resources.appaction_playback_action_seek_previous
import spmp.shared.generated.resources.appaction_playback_action_seek_random
import spmp.shared.generated.resources.appaction_playback_action_undo_seek

@Serializable
sealed interface PlaybackAction {
    fun getType(): Type
    suspend fun execute(player: PlayerState)

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onModification: (PlaybackAction) -> Unit) {}

    enum class Type {
        TOGGLE_PLAY,
        PLAY,
        PAUSE,
        SHUFFLE_QUEUE,
        CLEAR_QUEUE,
        SEEK_BY_TIME,
        SEEK_NEXT,
        SEEK_PREVIOUS,
        SEEK_RANDOM,
        UNDO_SEEK;

        companion object {
            val DEFAULT: Type = TOGGLE_PLAY
        }

        @Composable
        fun Preview(modifier: Modifier = Modifier) {
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(getIcon(), null)
                Text(getName(), softWrap = false)
            }
        }

        @Composable
        fun getName(): String =
            when (this) {
                TOGGLE_PLAY -> stringResource(Res.string.appaction_playback_action_toggle_play)
                PLAY -> stringResource(Res.string.appaction_playback_action_play)
                PAUSE -> stringResource(Res.string.appaction_playback_action_pause)
                SHUFFLE_QUEUE -> stringResource(Res.string.appaction_playback_action_shuffle_queue)
                CLEAR_QUEUE -> stringResource(Res.string.appaction_playback_action_clear_queue)
                SEEK_BY_TIME -> stringResource(Res.string.appaction_playback_action_seek_by_time)
                SEEK_NEXT -> stringResource(Res.string.appaction_playback_action_seek_next)
                SEEK_PREVIOUS -> stringResource(Res.string.appaction_playback_action_seek_previous)
                SEEK_RANDOM -> stringResource(Res.string.appaction_playback_action_seek_random)
                UNDO_SEEK -> stringResource(Res.string.appaction_playback_action_undo_seek)
            }

        fun getIcon(): ImageVector =
            when (this) {
                TOGGLE_PLAY -> Icons.Default.ToggleOn
                PLAY -> Icons.Default.PlayArrow
                PAUSE -> Icons.Default.Pause
                SHUFFLE_QUEUE -> Icons.Default.Shuffle
                CLEAR_QUEUE -> Icons.Default.CleaningServices
                SEEK_BY_TIME -> Icons.Default.FastForward
                SEEK_NEXT -> Icons.Default.SkipNext
                SEEK_PREVIOUS -> Icons.Default.SkipPrevious
                SEEK_RANDOM -> Icons.Default.Casino
                UNDO_SEEK -> Icons.Default.Undo
            }

        fun createAction(): PlaybackAction =
            when (this) {
                TOGGLE_PLAY -> TogglePlayPlaybackAppAction()
                PLAY -> PlayPlaybackAppAction()
                PAUSE -> PausePlaybackAppAction()
                SHUFFLE_QUEUE -> ShuffleQueuePlaybackAppAction()
                CLEAR_QUEUE -> ClearQueuePlaybackAppAction()
                SEEK_BY_TIME -> SeekByTimePlaybackAppAction()
                SEEK_NEXT -> SeekNextPlaybackAppAction()
                SEEK_PREVIOUS -> SeekPreviousPlaybackAppAction()
                SEEK_RANDOM -> SeekRandomPlaybackAppAction()
                UNDO_SEEK -> UndoSeekPlaybackAppAction()
            }
    }
}
