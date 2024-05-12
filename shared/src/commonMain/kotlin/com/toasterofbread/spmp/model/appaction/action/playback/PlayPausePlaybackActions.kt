package com.toasterofbread.spmp.model.appaction.action.playback

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.service.playercontroller.PlayerState

@Serializable
class PlayPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.PLAY

    override suspend fun execute(player: PlayerState) {
        player.withPlayer {
            play()
        }
    }
}

@Serializable
class PausePlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.PAUSE

    override suspend fun execute(player: PlayerState) {
        player.withPlayer {
            pause()
        }
    }
}

@Serializable
class TogglePlayPlaybackAppAction: PlaybackAction {
    override fun getType(): PlaybackAction.Type =
        PlaybackAction.Type.TOGGLE_PLAY

    override suspend fun execute(player: PlayerState) {
        player.withPlayer {
            playPause()
        }
    }
}
