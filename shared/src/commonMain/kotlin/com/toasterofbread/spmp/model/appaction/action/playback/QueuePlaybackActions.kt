package com.toasterofbread.spmp.model.appaction.action.playback

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.service.playercontroller.PlayerState

@Serializable
class ShuffleQueuePlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.SHUFFLE_QUEUE

    override suspend fun execute(player: PlayerState) {
        player.withPlayer{
            undoableAction {
                shuffleQueue(start = current_item_index + 1)
            }
        }
    }
}

@Serializable
class ClearQueuePlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.CLEAR_QUEUE

    override suspend fun execute(player: PlayerState) {
        player.withPlayer {
            undoableAction {
                clearQueue(keep_current = player.status.m_song_count > 1)
            }
        }
    }
}
