package com.toasterofbread.spmp.model.appaction.action.playback

import kotlinx.serialization.Serializable
import LocalAppState

@Serializable
class ShuffleQueuePlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.SHUFFLE_QUEUE

    override suspend fun execute(state: SpMp.State) {
        state.session.withPlayer{
            undoableAction {
                shuffleQueue(start = current_song_index + 1)
            }
        }
    }
}

@Serializable
class ClearQueuePlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.CLEAR_QUEUE

    override suspend fun execute(state: SpMp.State) {
        state.session.withPlayer {
            undoableAction {
                clearQueue(keep_current = state.session.status.m_song_count > 1)
            }
        }
    }
}
