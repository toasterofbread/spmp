package com.toasterofbread.spmp.platform

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.session.SessionCommand
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus

sealed class PlayerServiceCommand {
    fun getSessionCommand(): SessionCommand {
        return SessionCommand("com.toasterofbread.spmp.${this::class.simpleName}", getDataBundle())
    }

    data class SetLiked(private val value: SongLikedStatus): PlayerServiceCommand() {
        override fun getDataBundle(): Bundle =
            bundleOf("value" to value.ordinal)
    }

    data object SavePersistentQueue: PlayerServiceCommand() {
        override fun getDataBundle(): Bundle = Bundle.EMPTY
    }

    data class SetStopAfterCurrentSong(private val value: Boolean): PlayerServiceCommand() {
        override fun getDataBundle(): Bundle =
            bundleOf("value" to value)
    }

    protected abstract fun getDataBundle(): Bundle

    companion object {
        fun getBaseSessionCommands(): List<SessionCommand> =
            PlayerServiceCommand::class.nestedClasses.map { cls ->
                SessionCommand("com.toasterofbread.spmp.${cls.simpleName}", Bundle.EMPTY)
            }

        fun fromSessionCommand(command: SessionCommand): PlayerServiceCommand? =
            when (command.customAction.substring(24)) {
                "SetLiked" ->
                    SetLiked(SongLikedStatus.values()[command.customExtras.getInt("value")])

                "SavePersistentQueue" -> SavePersistentQueue

                "SetStopAfterCurrentSong" ->
                    SetStopAfterCurrentSong(command.customExtras.getBoolean("value"))

                else -> null
            }
    }
}
