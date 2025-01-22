package com.toasterofbread.spmp.model.radio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.toastbits.composekit.util.platform.launchSingle
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.song.Song
import dev.toastbits.ytmkt.endpoint.RadioBuilderModifier
import dev.toastbits.ytmkt.radio.RadioContinuation
import kotlinx.coroutines.*
import PlatformIO

internal typealias RadioFilter = List<RadioBuilderModifier>

internal data class RadioLoadResult(
    val songs: List<Song>,
    val continuation: RadioContinuation? = null,
    val filters: List<RadioFilter>? = null
)

abstract class RadioInstance(val context: AppContext) {
    data class LoadResult(val songs: List<Song>?, val has_continuation: Boolean)

    var state: RadioState by mutableStateOf(RadioState())
        private set

    val is_active: Boolean get() = state.source != null
    var is_loading: Boolean by mutableStateOf(false)
        private set
    var load_error: Throwable? by mutableStateOf(null)
        private set

    fun isContinuationAvailable(): Boolean =
        state.isContinuationAvailable()

    fun setRadioState(
        state: RadioState,
        onCompleted: (LoadResult) -> Unit = {},
        onCompletedOverride: ((LoadResult) -> Unit)? = null
    ) {
        cancelCurrentJob()
        this.state = state
        loadContinuation(onCompleted = onCompleted, onCompletedOverride = onCompletedOverride)
    }

    fun setFilter(filter_index: Int?) {
        if (filter_index == state.current_filter_index) {
            return
        }
        state = state.copy(current_filter_index = filter_index)
    }

    open fun cancelRadio() {
        cancelCurrentJob()
        state = RadioState()
    }

    fun playMediaItem(
        item: MediaItem,
        index_in_queue: Int?,
        shuffle: Boolean = false,
        onCompleted: (LoadResult) -> Unit = {}
    ) {
        setRadioState(
            RadioState(
                source = RadioState.RadioStateSource.ItemUid(item.getUid()),
                item_queue_index = index_in_queue,
                shuffle = shuffle
            ),
            onCompleted = onCompleted
        )
    }

    fun loadContinuation(
        onCompletedOverride: ((LoadResult) -> Unit)? = null,
        onCompleted: (LoadResult) -> Unit = {}
    ) {
        if (is_loading || !isContinuationAvailable()) {
            return
        }

        val current_state: RadioState = state
        is_loading = true
        load_error = null

        coroutine_scope.launchSingle(Dispatchers.PlatformIO) {
            println("LOADCONT $current_state")
            val load_result: Result<RadioLoadResult?> = current_state.loadContinuation(context)

            val processed_songs: List<Song>? =
                load_result.fold(
                    { result -> result?.songs?.let { processLoadedSongs(it) } },
                    {
                        RuntimeException("Failed to load radio continuation from $current_state", it).printStackTrace()
                        emptyList()
                    }
                )

            withContext(Dispatchers.Main) {
                load_result.fold(
                    onSuccess = {
                        state = current_state.copy(
                            continuation = it?.continuation,
                            filters = it?.filters,
                            initial_songs_loaded = true
                        )

                        val result: LoadResult =
                            LoadResult(
                                songs = processed_songs,
                                has_continuation = it?.continuation != null
                            )

                        if (onCompletedOverride != null) {
                            onCompletedOverride(result)
                        }
                        else {
                            onLoadCompleted(
                                result = result,
                                is_continuation = current_state.continuation != null
                            )
                        }

                        onCompleted(result)
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
            if (state.source?.isItem(song) == true) {
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

    protected abstract suspend fun onLoadCompleted(result: LoadResult, is_continuation: Boolean)

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    private fun cancelCurrentJob() {
        coroutine_scope.coroutineContext.cancelChildren()
        is_loading = false
    }

    private suspend fun processLoadedSongs(songs: List<Song>): List<Song> {
        context.database.transaction {
            for (song in songs) {
                if (song is MediaItemData) {
                    song.saveToDatabase(context.database, subitems_uncertain = true)
                }
            }
        }

        return songs.filter { song ->
            !isMediaItemHidden(song, context)
        }
    }

    override fun toString(): String =
        "RadioInstance(state=$state, is_loading=$is_loading, load_error=$load_error)"
}

