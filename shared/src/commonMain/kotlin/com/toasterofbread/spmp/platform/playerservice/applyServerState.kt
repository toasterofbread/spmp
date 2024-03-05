package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import kotlinx.coroutines.*
import spms.socketapi.shared.SpMsServerState

internal suspend fun SpMsPlayerService.applyServerState(
    state: SpMsServerState,
    coroutine_scope: CoroutineScope,
    onProgress: (String?) -> Unit = {}
) = withContext(Dispatchers.Default) {
    assert(playlist.isEmpty())

    onProgress(null)

    val items: Array<Song?> = arrayOfNulls(state.queue.size)
    var completed: Int = 0

    val loaded_items: Map<String, Boolean> =
        context.database.mediaItemQueries.loaded().executeAsList()
            .associate { it.id to (it.loaded != null) }

    state.queue.mapIndexedNotNull { i, item_id ->
        val song: Song = SongRef(item_id)
        if (loaded_items[item_id] == true) {
            items[i] = song
            completed++
            return@mapIndexedNotNull null
        }

        coroutine_scope.launch(Dispatchers.IO) {
            song.loadData(context, force = true, save = false).onSuccess { data ->
                items[i] = data
            }
            onProgress("${++completed}/${items.size}")
        }
    }.joinAll()

    context.database.transaction {
        for (item in items) {
            if (item == null) {
                continue
            }

            if (item is MediaItemData) {
                item.saveToDatabase(context.database)
            }

            playlist.add(item)
        }
    }

    if (playlist.isNotEmpty()) {
        service_player.session_started = true
    }

    if (state.state != _state) {
        _state = state.state
        listeners.forEach {
            it.onStateChanged(_state)
            it.onEvents()
        }
    }
    if (state.is_playing != _is_playing) {
        _is_playing = state.is_playing
        listeners.forEach {
            it.onPlayingChanged(_is_playing)
            it.onEvents()
        }
    }
    if (state.current_item_index != _current_song_index) {
        _current_song_index = state.current_item_index

        val song = playlist.getOrNull(_current_song_index)
        listeners.forEach {
            it.onSongTransition(song, false)
            it.onEvents()
        }
    }
    if (state.repeat_mode != _repeat_mode) {
        _repeat_mode = state.repeat_mode
        listeners.forEach {
            it.onRepeatModeChanged(_repeat_mode)
            it.onEvents()
        }
    }

    _duration_ms = state.duration_ms.toLong()
    updateCurrentSongPosition(state.current_position_ms.toLong())

    listeners.forEach {
        it.onDurationChanged(_duration_ms)
        it.onEvents()
    }
}
