package com.toasterofbread.spmp.youtubeapi.radio

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.composekit.utils.common.synchronizedBlock
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job

class RadioInstance(val context: AppContext) {
    var state: RadioState by mutableStateOf(RadioState())

    val active: Boolean get() = state.item != null
    val loading: Boolean get() = state.loading

    private val coroutine_scope = CoroutineScope(Dispatchers.IO)
    private val lock = coroutine_scope
    private var failed_load_retry_callback: (suspend (Result<List<Song>>, Boolean) -> Unit)? by mutableStateOf(null)

    fun dismissRadioLoadError() {
        state = state.copy(
            load_error = null
        )
        failed_load_retry_callback = null
    }

    data class RadioLoadError(
        val message: String,
        val stack_trace: String,
        val can_retry: Boolean
    )

    data class RadioState(
        val item: Pair<String, Int?>? = null,
        val continuation: MediaItemLayout.Continuation? = null,
        val initial_songs_loaded: Boolean = false,
        val filters: List<List<RadioBuilderModifier>>? = null,
        val current_filter: Int? = null,
        val shuffle: Boolean = false,
        val loading: Boolean = false,
        val load_error: RadioLoadError? = null
    ) {
        fun getCurrentFilters(): List<RadioBuilderModifier> {
            return current_filter?.let { filter ->
                if (filter == -1) listOf(RadioBuilderModifier.Internal.ARTIST)
                else filters?.get(filter)
            } ?: emptyList()
        }

        internal fun copyWithFilter(filter_index: Int?): RadioState =
            copy(
                current_filter = filter_index,
                continuation = null,
                initial_songs_loaded = false
            )

        internal fun copyWithLoadError(error: Throwable?, can_retry: Boolean): RadioState =
            copy(
                load_error = error?.let {
                    RadioLoadError(it.message.toString(), it.stackTraceToString(), can_retry)
                }
            )
    }

    fun playMediaItem(item: MediaItem, index: Int? = null, shuffle: Boolean = false): RadioState {
        synchronized(lock) {
            return setRadioState(
                RadioState(
                    item = Pair(item.getUid(), index),
                    shuffle = shuffle
                )
            )
        }
    }

    fun setFilter(filter_index: Int?) {
        synchronized(lock) {
            if (filter_index == state.current_filter) {
                return
            }
            state = state.copyWithFilter(filter_index)
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
                state = state.copy(
                    item = state.item?.copy(second = current_index - 1)
                )
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
                state = state.copy(
                    item = state.item?.copy(second = to)
                )
            }
            else if (current_index in from..to) {
                state = state.copy(
                    item = state.item?.copy(second = current_index - 1)
                )
            }
            else if (current_index in to .. from) {
                state = state.copy(
                    item = state.item?.copy(second = current_index + 1)
                )
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
        state = state.copy(loading = false)
    }

    fun loadContinuation(
        context: AppContext,
        onStart: (suspend () -> Unit)? = null,
        can_retry: Boolean = false,
        is_retry: Boolean = false,
        callback: suspend (result: Result<List<Song>>, is_retry: Boolean) -> Unit
    ) {
        val use_callback: suspend (Result<List<Song>>, Boolean) -> Unit =
            if (is_retry) {{ a, b ->
                callback(a, b)
                failed_load_retry_callback?.invoke(a, b)
                failed_load_retry_callback = null
            }}
            else callback

        synchronized(lock) {
            coroutine_scope.launchSingle {
                coroutineContext.job.invokeOnCompletion { cause ->
                    if (cause !is CancellationException) {
                        synchronized(lock) {
                            state = state.copy(loading = false)
                        }
                    }
                }
                synchronized(lock) {
                    state = state.copy(loading = true)
                    failed_load_retry_callback = null
                }

                onStart?.invoke()

                if (state.continuation == null) {
                    if (state.initial_songs_loaded) {
                        return@launchSingle
                    }

                    val initial_songs: Result<List<Song>> = getInitialSongs()
                    initial_songs.onFailure { error ->
                        synchronized(lock) {
                            state = state.copyWithLoadError(error, can_retry)
                            failed_load_retry_callback = if (can_retry) callback else null
                        }
                    }

                    val formatted = formatContinuationResult(initial_songs)
                    use_callback(formatted, is_retry)

                    state = state.copy(initial_songs_loaded = true)
                    return@launchSingle
                }

                val continuation: MediaItemLayout.Continuation? = state.continuation
                if (continuation == null) {
                    use_callback(Result.failure(NullPointerException("State continuation is null")), is_retry)
                    return@launchSingle
                }

                val result = continuation.loadContinuation(
                    context,
                    state.getCurrentFilters()
                )
                val (items, cont) = result.fold(
                    { it },
                    { error ->
                        synchronized(lock) {
                            state = state.copyWithLoadError(error, can_retry)
                            failed_load_retry_callback = if (can_retry) callback else null
                        }
                        use_callback(result.cast(), is_retry)
                        return@launchSingle
                    }
                )

                if (cont != null) {
                    state.continuation?.update(cont)
                }
                else {
                    state = state.copy(continuation = null)
                }

                use_callback(formatContinuationResult(Result.success(items.filterIsInstance<SongData>())), is_retry)
            }
        }
    }

    private fun formatContinuationResult(result: Result<List<Song>>): Result<List<Song>> =
        result.fold(
            { songs ->
                val filtered = context.database.transactionWithResult {
                    songs.filter { song ->
                        if (song is MediaItemData) {
                            song.saveToDatabase(context.database, subitems_uncertain = true)
                        }
                        !isMediaItemHidden(song, context.database)
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
        val item_uid: String = state.item?.first
            ?: return Result.failure(NullPointerException("State item is null"))

        when (val item = getMediaItemFromUid(item_uid)) {
            is Song -> {
                val result = context.ytapi.SongRadio.getSongRadio(
                    item.id,
                    null,
                    state.getCurrentFilters()
                )
                return result.fold(
                    { data ->
                        state = state.copy(
                            continuation = data.continuation?.let { continuation ->
                                MediaItemLayout.Continuation(continuation, MediaItemLayout.Continuation.Type.SONG, item.id)
                            },
                            filters = state.filters ?: data.filters
                        )

                        Result.success(data.items)
                    },
                    { Result.failure(it) }
                )
            }
            is Artist -> TODO()
            is RemotePlaylist -> {
                val (items, continuation) = context.database.transactionWithResult {
                    Pair(item.Items.get(context.database), item.Continuation.get(context.database))
                }

                if (items == null) {
                    state = state.copy(
                        continuation = continuation ?: MediaItemLayout.Continuation(item.id, MediaItemLayout.Continuation.Type.PLAYLIST_INITIAL)
                    )
                    return Result.success(emptyList())
                }

                state = state.copy(
                    continuation = continuation
                )

                return Result.success(items)
            }
            is LocalPlaylist -> {
                val data: LocalPlaylistData = item.loadData(context).fold(
                    { it },
                    { return Result.failure(it) }
                )
                return Result.success(data.items ?: emptyList())
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


@Composable
fun RadioInstance.RadioState.LoadStatus(
    modifier: Modifier = Modifier,
    expanded_modifier: Modifier = Modifier,
    disable_parent_scroll: Boolean = false
) {
    val player = LocalPlayerState.current

    Box(modifier, contentAlignment = Alignment.Center) {
        Crossfade(Pair(loading, load_error)) {
            if (it.first) {
                SubtleLoadingIndicator()
            }
            else if (it.second != null) {
                val (message, stack_trace, can_retry) = it.second ?: return@Crossfade
                ErrorInfoDisplay(
                    null,
                    isDebugBuild(),
                    pair_error = Pair(message, stack_trace),
                    expanded_content_modifier = expanded_modifier,
                    disable_parent_scroll = disable_parent_scroll,
                    onRetry =
                        if (can_retry) {{
                            player.controller?.service_player?.continueRadio(is_retry = true)
                        }}
                        else null,
                    onDismiss = {
                        player.controller?.service_player?.radio?.instance?.dismissRadioLoadError()
                    }
                )
            }
        }
    }
}
