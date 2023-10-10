package com.toasterofbread.spmp.platform

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.session.SessionCommand
import com.google.gson.Gson
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.youtubeapi.fromJson

sealed class PlayerServiceCommand {
    fun getSessionCommand(): SessionCommand =
        SessionCommand("com.toasterofbread.spmp.${this::class.simpleName}", bundleOf("data" to Gson().toJson(this)))

    data class SetLiked(val value: SongLikedStatus): PlayerServiceCommand()

    companion object {
        fun getBaseSessionCommands(): List<SessionCommand> =
            listOf(
                SetLiked::class.simpleName
            ).map { cls ->
                SessionCommand("com.toasterofbread.spmp.$cls", Bundle.EMPTY)
            }

        fun fromSessionCommand(command: SessionCommand, args: Bundle): PlayerServiceCommand? {
            val data = args.getString("data") ?: command.customExtras.getString("data") ?: "{}"

            return when (command.customAction.substring(24)) {
                "SetLiked" -> Gson().fromJson<SetLiked>(data)
                else -> null
            }
        }
    }
}
