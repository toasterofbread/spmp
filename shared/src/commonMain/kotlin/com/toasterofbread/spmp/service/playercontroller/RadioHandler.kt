package com.toasterofbread.spmp.service.playercontroller

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.platform.playerservice.UndoRedoAction
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO Add setting
// Radio continuation will be added if the amount of remaining songs (including current) falls below this
private const val RADIO_MIN_LENGTH: Int = 10

class RadioHandler(val player: PlayerServicePlayer, val context: AppContext) {
    val instance: RadioInstance = RadioInstance(context)
    fun getRadioChangeUndoRedo(
        previous_radio_state: RadioInstance.RadioState,
        continuation_index: Int,
        save: Boolean = true,
        skip_existing: Boolean = true,
        onLoad: (suspend (success: Boolean) -> Unit)? = null,
        furtherAction: (PlayerServicePlayer.() -> UndoRedoAction?) -> Unit
    ): UndoRedoAction {
        val radio_state: RadioInstance.RadioState = instance.state

        synchronized(instance) {
            instance.loadContinuation(
                context,
                can_retry = true
            ) { result, is_retry ->
                result.fold(
                    { songs ->
                        withContext(Dispatchers.Main) {
                            furtherAction {
                                player.addMultipleToQueue(
                                    songs,
                                    continuation_index,
                                    save = save,
                                    skip_existing = skip_existing
                                )
                                null
                            }
                        }
                    },
                    {
                        if (save) {
                            player.savePersistentQueue()
                        }
                    }
                )

                onLoad?.invoke(result.isSuccess)
            }
        }

        return object : UndoRedoAction {
            override fun redo(service: PlayerService) {
                instance.setRadioState(radio_state)
            }

            override fun undo(service: PlayerService) {
                instance.setRadioState(previous_radio_state)
            }
        }
    }

    fun setRadioFilter(filter_index: Int?) = synchronized(instance) {
        val previous_filter_index = instance.state.current_filter
        if (filter_index == previous_filter_index) {
            return
        }

        instance.setFilter(filter_index)

        val item = instance.state.item
        val add_index = maxOf(item?.second ?: -1, player.current_song_index) + 1

        player.customUndoableAction { furtherAction ->
            instance.loadContinuation(
                context,
                onStart = {
                    withContext(Dispatchers.Main) {
                        furtherAction {
                            player.clearQueue(add_index, cancel_radio = false, save = false)
                            null
                        }
                    }
                },
                can_retry = true
            ) { result, is_retry ->
                result.onSuccess { songs ->
                    withContext(Dispatchers.Main) {
                        furtherAction {
                            player.addMultipleToQueue(songs, add_index, skip_existing = true)
                            null
                        }
                    }
                }
            }

            return@customUndoableAction object : UndoRedoAction {
                override fun redo(service: PlayerService) {
                    instance.setFilter(filter_index)
                }

                override fun undo(service: PlayerService) {
                    instance.setFilter(previous_filter_index)
                }
            }
        }
    }

    fun checkAutoRadioContinuation() {
        if (!instance.active || instance.loading) {
            return
        }

        val remaining = player.song_count - player.current_song_index
        if (remaining < RADIO_MIN_LENGTH) {
            player.continueRadio()
        }
    }
}
