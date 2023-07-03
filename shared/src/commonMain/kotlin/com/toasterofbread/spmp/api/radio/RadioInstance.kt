package com.toasterofbread.spmp.api.radio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.ui.component.MediaItemLayout
import com.toasterofbread.utils.ValueListeners
import kotlinx.coroutines.*
import okhttp3.Request

private const val RADIO_ID_PREFIX = "RDAMVM"
private const val MODIFIED_RADIO_ID_PREFIX = "RDAT"

class RadioInstance {
    var state: RadioState by mutableStateOf(RadioState())
        private set
    val active: Boolean get() = state.item != null
    var loading: Boolean by mutableStateOf(false)
        private set

    private val coroutine_scope = CoroutineScope(Dispatchers.IO)
    private val lock = coroutine_scope

    val filter_changed_listeners = ValueListeners<List<RadioModifier>?>()

    fun playMediaItem(item: MediaItem, index: Int? = null, shuffle: Boolean = false): RadioState {
        synchronized(lock) {
            cancelJob()
            val old = setRadioState(RadioState())
            state.item = Pair(item, index)
            state.shuffle = shuffle
            return old
        }
    }

    fun setFilter(filter_index: Int?) {
        if (filter_index == state.current_filter) {
            return
        }
        state.current_filter = filter_index
        state.continuation = null

        val filter = state.current_filter?.let { state.filters!![it] }
        filter_changed_listeners.call(filter)
    }

    fun onSongRemoved(index: Int) {
        synchronized(lock) {
            val current_index = state.item?.second ?: return
            if (index == current_index) {
                cancelRadio()
            }
            else if (index < current_index) {
                state.item = state.item?.copy(second = current_index - 1)
            }
        }
    }

    fun onSongMoved(from: Int, to: Int) {
        synchronized(lock) {
            if (from == to) {
                return
            }

            val current_index = state.item?.second ?: return

            if (from == current_index) {
                state.item = state.item?.copy(second = to)
            }
            else if (current_index in from..to) {
                state.item = state.item?.copy(second = current_index - 1)
            }
            else if (current_index in to .. from) {
                state.item = state.item?.copy(second = current_index + 1)
            }
        }
    }

    class RadioState {
        var item: Pair<MediaItem, Int?>? by mutableStateOf(null)
        var continuation: MediaItemLayout.Continuation? by mutableStateOf(null)
        var filters: List<List<RadioModifier>>? by mutableStateOf(null)
        var current_filter: Int? by mutableStateOf(null)
        var shuffle: Boolean = false
    }

    fun setRadioState(new_state: RadioState): RadioState {
        if (state == new_state) {
            return state
        }
        synchronized(lock) {
            cancelRadio()
            val old = state
            state = new_state
            return old
        }
    }

    fun cancelRadio(): RadioState {
        synchronized(lock) {
            val old_state = setRadioState(RadioState())
            cancelJob()
            return old_state
        }
    }

    fun cancelJob() {
        coroutine_scope.coroutineContext.cancelChildren()
        loading = false
    }

    private fun formatContinuationResult(result: Result<List<Song>>): Result<List<Song>> =
        result.fold(
            { songs ->
                if (state.shuffle) Result.success(songs.shuffled())
                else result
            },
            { result }
        )

    fun loadContinuation(onStart: (suspend () -> Unit)? = null, callback: suspend (Result<List<Song>>) -> Unit) {
        synchronized(lock) {
            check(!loading)

            coroutine_scope.launch {
                coroutineContext.job.invokeOnCompletion {
                    synchronized(lock) {
                        loading = false
                    }
                }
                synchronized(lock) {
                    loading = true
                }

                onStart?.invoke()

                if (state.continuation == null) {
                    val initial_songs = getInitialSongs()
                    val formatted = formatContinuationResult(initial_songs)
                    callback(formatted)
                    return@launch
                }

                val result = state.continuation!!.loadContinuation(state.current_filter?.let { state.filters?.get(it) } ?: emptyList())
                if (result.isFailure) {
                    callback(result.cast())
                    return@launch
                }

                val (items, cont) = result.getOrThrow()

                if (cont != null) {
                    state.continuation!!.update(cont)
                }
                else {
                    state.continuation = null
                }

                callback(formatContinuationResult(Result.success(items.filterIsInstance<Song>())))
            }
        }
    }

    private suspend fun getInitialSongs(): Result<List<Song>> {
        when (val item = state.item!!.first) {
            is Song -> {
                val result = getSongRadio(item.id, null, state.current_filter?.let { state.filters?.get(it) } ?: emptyList())
                return result.fold(
                    { data ->
                        state.continuation = data.continuation?.let { continuation ->
                            MediaItemLayout.Continuation(continuation, MediaItemLayout.Continuation.Type.SONG, item.id)
                        }

                        if (state.filters == null) {
                            state.filters = data.filters
                        }

                        Result.success(data.items)
                    },
                    { Result.failure(it) }
                )
            }
            is MediaItemWithLayouts -> {
                val feed_layouts = item.getFeedLayouts().fold(
                    { it },
                    { return Result.failure(it) }
                )

                val layout = feed_layouts.firstOrNull()
                if (layout == null) {
                    return Result.success(emptyList())
                }

                val layout_item = layout.view_more?.media_item
                if (layout_item is Playlist) {
                    state.continuation = MediaItemLayout.Continuation(layout_item.id, MediaItemLayout.Continuation.Type.PLAYLIST_INITIAL, layout.items.size)
                }
                else {
                    state.continuation = layout.continuation
                }

                return Result.success(layout.items.filterIsInstance<Song>())
            }
            else -> throw NotImplementedError(item.javaClass.name)
        }
    }
}
