package com.toasterofbread.spmp.platform

import android.os.Bundle
import androidx.media3.session.SessionCommand

enum class PlayerServiceCommand {
    SetLikeTrue,
    SetLikeNeutral,
    TriggerPersistentQueueSave,
    SetStopAfterCurrentSong;

    fun getSessionCommand(args: Bundle = Bundle.EMPTY): SessionCommand {
        return SessionCommand("com.toasterofbread.spmp.$name", args)
    }

    companion object {
        fun fromIdOrNull(id: String): PlayerServiceCommand? {
            return values().firstOrNull {
                it.getSessionCommand().customAction == id
            }
        }
    }
}
