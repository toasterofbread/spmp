package com.toasterofbread.spmp.service.playerservice

import com.toasterofbread.spmp.platform.MediaPlayerService
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Radio continuation will be added if the amount of remaining songs (including current) falls below this
// TODO Add setting
private const val RADIO_MIN_LENGTH: Int = 10

internal class RadioHandler(val player: PlayerService, val context: PlatformContext) {
    val instance: RadioInstance = RadioInstance(context)
    
    fun getRadioChangeUndoRedo(
        previous_radio_state: RadioInstance.RadioState,
        continuation_index: Int,
        save: Boolean = true,
        skip_existing: Boolean = true,
        onLoad: (suspend (success: Boolean) -> Unit)? = null,
        furtherAction: (MediaPlayerService.() -> MediaPlayerService.UndoRedoAction?) -> Unit
    ): MediaPlayerService.UndoRedoAction {
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
                            withContext(Dispatchers.Main) {
                                player.persistent_queue.savePersistentQueue()
                            }
                        }
                    }
                )

                onLoad?.invoke(result.isSuccess)
            }
        }

        return object : MediaPlayerService.UndoRedoAction {
            override fun redo() {
                instance.setRadioState(radio_state)
            }

            override fun undo() {
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

            return@customUndoableAction object : MediaPlayerService.UndoRedoAction {
                override fun redo() {
                    instance.setFilter(filter_index)
                }

                override fun undo() {
                    instance.setFilter(previous_filter_index)
                }
            }
        }
    }

    fun checkRadioContinuation() {
        if (!instance.active || instance.loading) {
            return
        }

        val remaining = player.song_count - player.current_song_index
        if (remaining < RADIO_MIN_LENGTH) {
            player.continueRadio()
        }
    }
}
