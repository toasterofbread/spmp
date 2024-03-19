package com.toasterofbread.spmp.model.radio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.song.Song
import dev.toastbits.ytmkt.endpoint.RadioBuilderModifier
import dev.toastbits.ytmkt.radio.RadioContinuation
import kotlinx.coroutines.*

internal typealias RadioFilter = List<RadioBuilderModifier>

internal data class RadioLoadResult(
    val songs: List<Song>,
    val continuation: RadioContinuation? = null,
    val filters: List<RadioFilter>? = null
)

abstract class RadioInstance(val context: AppContext) {
    var state: RadioState by mutableStateOf(RadioState())
        private set

    val is_active: Boolean get() = state.item_uid != null
    var is_loading: Boolean by mutableStateOf(false)
        private set
    var load_error: Throwable? by mutableStateOf(null)
        private set

    fun isContinuationAvailable(): Boolean =
        state.isContinuationAvailable()

    fun setRadioState(
        state: RadioState,
        onCompleted: (List<Song>) -> Unit = {}
    ) {
        cancelCurrentJob()
        this.state = state
        loadContinuation(onCompleted = onCompleted)
    }

    fun setFilter(filter_index: Int?) {
        if (filter_index == state.current_filter_index) {
            return
        }
        state = state.copy(current_filter_index = filter_index)
    }

    fun cancelRadio() {
        cancelCurrentJob()
        state = RadioState()
    }

    fun playMediaItem(
        item: MediaItem,
        index_in_queue: Int?,
        shuffle: Boolean = false,
        onCompleted: (List<Song>) -> Unit = {}
    ) {
        setRadioState(
            RadioState(
                item_uid = item.getUid(),
                item_queue_index = index_in_queue,
                shuffle = shuffle
            ),
            onCompleted = onCompleted
        )
    }

    fun loadContinuation(
        onCompletedOverride: ((List<Song>) -> Unit)? = null,
        onCompleted: (List<Song>) -> Unit = {}
    ) {
        if (is_loading || !isContinuationAvailable()) {
            return
        }

        val current_state: RadioState = state
        is_loading = true

        coroutine_scope.launchSingle(Dispatchers.IO) {
            val load_result: Result<RadioLoadResult> = current_state.loadContinuation(context)

            val processed_songs: List<Song> =
                load_result.fold(
                    { processLoadedSongs(it.songs) },
                    { emptyList() }
                )

            withContext(Dispatchers.Main) {
                load_result.fold(
                    onSuccess = {
                        state = current_state.copy(
                            continuation = it.continuation,
                            filters = it.filters,
                            initial_songs_loaded = true
                        )

                        if (onCompletedOverride != null) {
                            onCompletedOverride(processed_songs)
                        }
                        else {
                            onLoadCompleted(
                                songs = processed_songs,
                                is_continuation = current_state.continuation != null
                            )
                        }

                        onCompleted(processed_songs)
                    },
                    onFailure = {
                        load_error = it
                    }
                )

                is_loading = false
            }
        }
    }

    fun dismissLoadError() {
        load_error = null
    }

    fun onQueueSongAdded(at_index: Int) {
        val item_index: Int = state.item_queue_index ?: return
        if (at_index <= item_index) {
            state = state.copy(item_queue_index = item_index + 1)
        }
    }

    fun onQueueSongRemoved(from_index: Int, song: Song) {
        val item_index: Int = state.item_queue_index ?: return
        if (from_index == item_index) {
            if (song.getUid() == state.item_uid) {
                state = state.copy(item_queue_index = null)
            }
        }
        else if (from_index < item_index) {
            state = state.copy(item_queue_index = item_index - 1)
        }
    }

    fun onQueueSongMoved(from_index: Int, to_index: Int) {
        val item_index: Int = state.item_queue_index ?: return
        if (item_index == from_index) {
            state = state.copy(item_queue_index = to_index)
        }
        else if (from_index > item_index && to_index < item_index) {
            state = state.copy(item_queue_index = item_index + 1)
        }
        else if (from_index < item_index && to_index > item_index) {
            state = state.copy(item_queue_index = item_index - 1)
        }
    }

    protected abstract suspend fun onLoadCompleted(songs: List<Song>, is_continuation: Boolean)

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    private fun cancelCurrentJob() {
        coroutine_scope.coroutineContext.cancelChildren()
        is_loading = false
    }

    private fun processLoadedSongs(songs: List<Song>): List<Song> {
        val filtered: List<Song> =
            context.database.transactionWithResult {
                songs.filter { song ->
                    if (song is MediaItemData) {
                        song.saveToDatabase(context.database, subitems_uncertain = true)
                    }
                    return@filter !isMediaItemHidden(song, context.database)
                }
            }

        return filtered
    }
}

