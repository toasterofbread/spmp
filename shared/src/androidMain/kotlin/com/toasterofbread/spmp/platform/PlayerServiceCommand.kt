package com.toasterofbread.spmp.platform

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.session.SessionCommand
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res

sealed class PlayerServiceCommand {
    fun getSessionCommand(): SessionCommand =
        SessionCommand(
            "com.toasterofbread.spmp.${this::class.simpleName}",
            bundleOf("data" to serialise())
        )

    protected abstract fun serialise(): String

    @Serializable
    data class SetLiked(val value: SongLikedStatus): PlayerServiceCommand() {
        override fun serialise(): String = Json.encodeToString(this)
    }

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
                "SetLiked" -> Json.decodeFromString<SetLiked>(data)
                else -> null
            }
        }
    }
}
