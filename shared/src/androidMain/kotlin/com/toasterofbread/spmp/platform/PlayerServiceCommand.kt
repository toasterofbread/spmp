package com.toasterofbread.spmp.platform

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.google.gson.Gson
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.youtubeapi.fromJson

sealed class PlayerServiceCommand {
    fun getSessionCommand(): SessionCommand =
        SessionCommand("com.toasterofbread.spmp.${this::class.simpleName}", Bundle.EMPTY)

    fun sendCommand(controller: MediaController) {
        controller.sendCustomCommand(
            getSessionCommand(),
            bundleOf("data" to Gson().toJson(this))
        )
    }

    data object CancelSession: PlayerServiceCommand()

    data class ContinueRadio(val is_retry: Boolean): PlayerServiceCommand()
    data object DismissRadioLoadError: PlayerServiceCommand()
    data class SetRadioFilter(val filter_index: Int?): PlayerServiceCommand()

    data class StartRadio(
        val index: Int,
        val item_uid: String? = null,
        val item_index: Int? = null,
        val add_item: Boolean = false,
        val skip_first: Boolean = false,
        val shuffle: Boolean = false,
        val on_load_seek_index: Int? = null
    ): PlayerServiceCommand() {
        init {
            require(item_index == null || item_uid != null)
        }
    }
    
    data class AddSong(val song_id: String, val to: Int, val is_active_queue: Boolean = false, val start_radio: Boolean = false): PlayerServiceCommand()
    data class AddMultipleSongs(
        val song_ids: List<String>,
        val index: Int = 0,
        val skip_first: Boolean = false,
        val is_active_queue: Boolean = false,
        val skip_existing: Boolean = false,
        val clear: Boolean = false
    ): PlayerServiceCommand()

    data class MoveSong(val from: Int, val to: Int): PlayerServiceCommand()
    data class RemoveSong(val from: Int): PlayerServiceCommand()
    data class RemoveMultipleSongs(val indices: List<Int>): PlayerServiceCommand()

    data class ClearQueue(val from: Int = 0, val keep_current: Boolean = false, val cancel_radio: Boolean = true): PlayerServiceCommand()
    data class ShuffleQueue(val start: Int = 0, val end: Int = -1): PlayerServiceCommand()
    data class ShuffleQueueIndices(val indices: List<Int>): PlayerServiceCommand()

    data class SetLiked(val value: SongLikedStatus): PlayerServiceCommand()

    data object SavePersistentQueue: PlayerServiceCommand()
    data object Undo: PlayerServiceCommand()
    data object Redo: PlayerServiceCommand()
    data object UndoAll: PlayerServiceCommand()
    data object RedoAll: PlayerServiceCommand()

    data class SetStopAfterCurrentSong(val value: Boolean): PlayerServiceCommand()

    data class UpdateActiveQueueIndex(val delta: Int): PlayerServiceCommand()
    data class SetActiveQueueIndex(val value: Int): PlayerServiceCommand()

    companion object {
        fun getBaseSessionCommands(): List<SessionCommand> =
            listOf(
                CancelSession::class.simpleName,
                ContinueRadio::class.simpleName,
                DismissRadioLoadError::class.simpleName,
                SetRadioFilter::class.simpleName,
                StartRadio::class.simpleName,
                AddSong::class.simpleName,
                AddMultipleSongs::class.simpleName,
                MoveSong::class.simpleName,
                RemoveSong::class.simpleName,
                RemoveMultipleSongs::class.simpleName,
                ClearQueue::class.simpleName,
                ShuffleQueue::class.simpleName,
                ShuffleQueueIndices::class.simpleName,
                SetLiked::class.simpleName,
                SavePersistentQueue::class.simpleName,
                Undo::class.simpleName,
                Redo::class.simpleName,
                UndoAll::class.simpleName,
                RedoAll::class.simpleName,
                SetStopAfterCurrentSong::class.simpleName,
                UpdateActiveQueueIndex::class.simpleName,
                SetActiveQueueIndex::class.simpleName
            ).map { cls ->
                SessionCommand("com.toasterofbread.spmp.$cls", Bundle.EMPTY)
            }

        fun fromSessionCommand(command: SessionCommand, args: Bundle): PlayerServiceCommand? {
            val data = args.getString("data")!!

            return when (command.customAction.substring(24)) {
                "CancelSession" -> CancelSession
                "ContinueRadio" -> Gson().fromJson<ContinueRadio>(data)
                "DismissRadioLoadError" -> DismissRadioLoadError
                "SetRadioFilter" -> Gson().fromJson<SetRadioFilter>(data)
                "StartRadio" -> Gson().fromJson<StartRadio>(data)
                "AddSong" -> Gson().fromJson<AddSong>(data)
                "AddMultipleSongs" -> Gson().fromJson<AddMultipleSongs>(data)
                "MoveSong" -> Gson().fromJson<MoveSong>(data)
                "RemoveSong" -> Gson().fromJson<RemoveSong>(data)
                "RemoveMultipleSongs" -> Gson().fromJson<RemoveMultipleSongs>(data)
                "ClearQueue" -> Gson().fromJson<ClearQueue>(data)
                "ShuffleQueue" -> Gson().fromJson<ShuffleQueue>(data)
                "ShuffleQueueIndices" -> Gson().fromJson<ShuffleQueueIndices>(data)
                "SetLiked" -> Gson().fromJson<SetLiked>(data)
                "SavePersistentQueue" -> SavePersistentQueue
                "Undo" -> Undo
                "Redo" -> Redo
                "UndoAll" -> UndoAll
                "RedoAll" -> RedoAll
                "SetStopAfterCurrentSong" -> Gson().fromJson<SetStopAfterCurrentSong>(data)
                "UpdateActiveQueueIndex" -> Gson().fromJson<UpdateActiveQueueIndex>(data)
                "SetActiveQueueIndex" -> Gson().fromJson<SetActiveQueueIndex>(data)

                else -> null
            }
        }
    }
}
