package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal suspend fun SpMsPlayerService.applyServerState(state: SpMsServerState, coroutine_scope: CoroutineScope) {
    assert(playlist.isEmpty())

    val items: Array<Song?> = arrayOfNulls(state.queue.size)

    state.queue.mapIndexed { i, id ->
        coroutine_scope.launch {
            val song: Song = SongRef(id)
            tryTransaction {
                if (song.Loaded.get(context.database)) {
                    return@tryTransaction
                }

                song.loadData(context).onSuccess { data ->
                    data.saveToDatabase(context.database)

                    data.artist?.also { artist ->
                        coroutine_scope.launch {
                            tryTransaction {
                                if (artist.Loaded.get(context.database)) {
                                    return@tryTransaction
                                }

                                artist.loadData(context).onSuccess { artist_data ->
                                    artist_data.saveToDatabase(context.database)
                                }
                            }
                        }
                    }
                }
            }
            items[i] = song
        }
    }.joinAll()

    playlist.addAll(items.filterNotNull())

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
    if (state.volume != _volume) {
        _volume = state.volume
        listeners.forEach {
            it.onVolumeChanged(_volume)
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

private inline fun tryTransaction(transaction: () -> Unit) {
    while (true) {
        try {
            transaction()
            break
        }
        catch (e: Throwable) {
            if (e.javaClass.name != "org.sqlite.SQLiteException") {
                throw e
            }
        }
    }
}
