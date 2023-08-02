package com.toasterofbread.spmp.api.radio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.RadioModifier
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.utils.launchSingle
import com.toasterofbread.utils.synchronizedBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import androidx.compose.animation.Crossfade
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.resources.getString
import androidx.compose.material3.Text
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.isMediaItemHidden

class RadioInstance(
    val database: Database
) {
    var state: RadioState by mutableStateOf(RadioState())
        private set
    val active: Boolean get() = state.item != null
    var loading: Boolean by mutableStateOf(false)
        private set

    private val coroutine_scope = CoroutineScope(Dispatchers.IO)
    private val lock = coroutine_scope
    private var failed_load: Pair<Throwable, (suspend (Result<List<Song>>, Boolean) -> Unit)?>? by mutableStateOf(null)

    class RadioState {
        var item: Pair<MediaItem, Int?>? by mutableStateOf(null)
        var continuation: MediaItemLayout.Continuation? by mutableStateOf(null)
        var initial_songs_loaded = false
        var filters: List<List<RadioModifier>>? by mutableStateOf(null)
        var current_filter: Int? by mutableStateOf(null)
        var shuffle: Boolean = false

        override fun toString(): String {
            return "RadioState(item=$item, continuation=$continuation)"
        }
    }

    fun playMediaItem(item: MediaItem, index: Int? = null, shuffle: Boolean = false): RadioState {
        synchronized(lock) {
            return setRadioState(RadioState().also { state ->
                state.item = Pair(item, index)
                state.shuffle = shuffle
            })
        }
    }

    fun setFilter(filter_index: Int?) {
        synchronized(lock) {
            if (filter_index == state.current_filter) {
                return
            }
            state.current_filter = filter_index
            state.continuation = null

            cancelJob()
        }
    }

    fun onSongRemoved(index: Int) {
        synchronizedBlock(lock) {
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

    fun setRadioState(new_state: RadioState): RadioState {
        synchronized(lock) {
            if (state == new_state) {
                return state
            }

            cancelJob()
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

    fun loadContinuation(
        onStart: (suspend () -> Unit)? = null, 
        can_retry: Boolean = false,
        is_retry: Boolean = false,
        callback: suspend (result: Result<List<Song>>, is_retry: Boolean) -> Unit
    ) {
        synchronized(lock) {
            coroutine_scope.launchSingle {
                coroutineContext.job.invokeOnCompletion {
                    synchronized(lock) {
                        loading = false
                    }
                }
                synchronized(lock) {
                    loading = true
                    failed_load = null
                }

                onStart?.invoke()

                if (state.continuation == null) {
                    if (state.initial_songs_loaded) {
                        return@launchSingle
                    }

                    val initial_songs = getInitialSongs()
                    initial_songs.onFailure { error ->
                        synchronized(lock) {
                            failed_load = Pair(
                                error,
                                if (can_retry) callback else null
                            )
                        }
                    }

                    val formatted = formatContinuationResult(initial_songs)
                    callback(formatted, is_retry)

                    state.initial_songs_loaded = true
                    return@launchSingle
                }

                val result = state.continuation!!.loadContinuation(
                    database,
                    state.current_filter?.let { state.filters?.get(it) } ?: emptyList()
                )
                val (items, cont) = result.fold(
                    { it },
                    { error ->
                        synchronized(lock) {
                            failed_load = Pair(
                                error,
                                if (can_retry) callback else null
                            )
                        }
                        callback(result.cast(), is_retry)
                        return@launchSingle
                    }
                )

                if (cont != null) {
                    state.continuation!!.update(cont)
                }
                else {
                    state.continuation = null
                }

                callback(formatContinuationResult(Result.success(items.filterIsInstance<SongData>())), is_retry)
            }
        }
    }

    @Composable
    fun LoadStatus(
        modifier: Modifier = Modifier, 
        expanded_modifier: Modifier = Modifier,
        disable_parent_scroll: Boolean = false
    ) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Crossfade(Pair(loading, failed_load)) {
                if (it.first) {
                    SubtleLoadingIndicator()
                }
                else if (it.second != null) {
                    val (error, callback) = it.second!!
                    ErrorInfoDisplay(
                        error,
                        expanded_modifier = expanded_modifier,
                        disable_parent_scroll = disable_parent_scroll,
                        extraButtonContent = { Text(getString("radio_action_retry_failed_load")) },
                        onExtraButtonPressed = 
                            if (callback != null) {{
                                loadContinuation(
                                    null,
                                    can_retry = true,
                                    is_retry = true,
                                    callback = callback
                                )
                            }} 
                            else null,
                        onDismiss = {
                            synchronized(lock) {
                                failed_load = null
                            }
                        }
                    )
                }
            }
        }
    }

    private fun formatContinuationResult(result: Result<List<Song>>): Result<List<Song>> =
        result.fold(
            { songs ->
                val filtered = database.transactionWithResult {
                    songs.filter { song ->
                        if (song is MediaItemData) {
                            song.saveToDatabase(database)
                        }
                        !isMediaItemHidden(song, database)
                    }
                }
                Result.success(
                    if (state.shuffle) filtered.shuffled()
                    else filtered
                )
            },
            { result }
        )

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
            is Artist -> TODO()
            is Playlist -> {
                val (items, continuation) = database.transactionWithResult {
                    Pair(item.Items.get(database), item.Continuation.get(database))
                }

                if (items == null) {
                    state.continuation = continuation ?: MediaItemLayout.Continuation(item.id, MediaItemLayout.Continuation.Type.PLAYLIST_INITIAL)
                    return Result.success(emptyList())
                }

                state.continuation = continuation

                return Result.success(items)
            }
//            is MediaItemWithLayouts -> {
//                val feed_layouts = item.getFeedLayouts().fold(
//                    { it },
//                    { return Result.failure(it) }
//                )
//
//                val layout = feed_layouts.firstOrNull()
//                if (layout == null) {
//                    return Result.success(emptyList())
//                }
//
//                val view_more = layout.view_more
//                if (view_more is MediaItemLayout.MediaItemViewMore && view_more.media_item is Playlist) {
//                    state.continuation = MediaItemLayout.Continuation(view_more.media_item.id, MediaItemLayout.Continuation.Type.PLAYLIST_INITIAL, layout.items.size)
//                }
//                else {
//                    state.continuation = layout.continuation
//                }
//
//                return Result.success(layout.items.filterIsInstance<SongData>())
//            }
            else -> throw NotImplementedError(item::class.toString())
        }
    }
}
