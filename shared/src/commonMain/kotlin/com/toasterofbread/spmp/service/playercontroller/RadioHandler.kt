package com.toasterofbread.spmp.service.playercontroller

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.model.radio.RadioState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.platform.playerservice.UndoRedoAction
import com.toasterofbread.spmp.platform.playerservice.PlayerService

// TODO Add setting
// Radio continuation will be added if the amount of remaining songs (including current) falls below this
private const val RADIO_MIN_LENGTH: Int = 10

open class RadioHandler(val player: PlayerServicePlayer, val context: AppContext) {
    val instance: RadioInstance =
        object : RadioInstance(context) {
            override suspend fun onLoadCompleted(result: RadioInstance.LoadResult, is_continuation: Boolean) {
                onRadioLoadCompleted(result, is_continuation)
            }

            override fun cancelRadio() {
                super.cancelRadio()
                onRadioCancelled()
            }
        }

    open fun onRadioCancelled() {}

    fun setUndoableRadioState(
        new_radio_state: RadioState,
        furtherAction: (PlayerServicePlayer.() -> UndoRedoAction?) -> Unit,
        insertion_index: Int = -1,
        skip_existing: Boolean = true,
        clear_after: Boolean = false,
        onSuccessfulLoad: (RadioInstance.LoadResult) -> Unit = {}
    ): UndoRedoAction {
        val old_radio_state: RadioState = instance.state

        return object : UndoRedoAction {
            var first_redo: Boolean = true

            override fun redo(service: PlayerService) {
                instance.setRadioState(
                    new_radio_state,
                    onCompleted =
                        if (first_redo) {
                            first_redo = false
                            { result ->
                                furtherAction {
                                    onSuccessfulLoad(result)
                                    return@furtherAction null
                                }
                            }
                        }
                        else {{}},
                    onCompletedOverride = { result ->
                        if (result.songs == null) {
                            return@setRadioState
                        }

                        furtherAction {
                            player.addMultipleToQueue(
                                result.songs,
                                if (insertion_index >= 0) insertion_index else player.item_count,
                                skip_existing = skip_existing,
                                clear_after = clear_after
                            )
                            return@furtherAction null
                        }
                    }
                )
            }

            override fun undo(service: PlayerService) {
                instance.setRadioState(old_radio_state)
            }
        }
    }

    fun setRadioFilter(filter_index: Int?) {
        val previous_filter_index: Int? = instance.state.current_filter_index
        if (filter_index == previous_filter_index) {
            return
        }

        instance.setFilter(filter_index)

        val item_queue_index: Int? = instance.state.item_queue_index
        val insertion_index: Int = maxOf(item_queue_index ?: -1, player.current_item_index) + 1

        player.customUndoableAction { furtherAction ->
            furtherAction {
                player.clearQueue(insertion_index, cancel_radio = false, save = false)
                return@furtherAction null
            }

            instance.loadContinuation(
                onCompletedOverride = { result ->
                    if (result.songs == null) {
                        return@loadContinuation
                    }

                    furtherAction {
                        player.addMultipleToQueue(result.songs, insertion_index, skip_existing = true)
                        return@furtherAction null
                    }
                }
            )

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
        if (!instance.is_active || instance.is_loading) {
            return
        }

        val remaining: Int = player.item_count - player.current_item_index
        if (remaining < RADIO_MIN_LENGTH) {
            instance.loadContinuation()
        }
    }

    private fun onRadioLoadCompleted(result: RadioInstance.LoadResult, is_continuation: Boolean) {
        if (result.songs == null) {
            return
        }

        player.addMultipleToQueue(
            result.songs,
            player.item_count,
            skip_existing = true
        )
    }
}
