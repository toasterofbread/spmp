package com.toasterofbread.spmp.model.appaction.action.playback

import kotlinx.serialization.Serializable
import LocalAppState

@Serializable
class PlayPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.PLAY

    override suspend fun execute(state: SpMp.State) {
        state.session.withPlayer {
            play()
        }
    }
}

@Serializable
class PausePlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.PAUSE

    override suspend fun execute(state: SpMp.State) {
        state.session.withPlayer {
            pause()
        }
    }
}

@Serializable
class TogglePlayPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.TOGGLE_PLAY

    override suspend fun execute(state: SpMp.State) {
        state.session.withPlayer {
            playPause()
        }
    }
}
