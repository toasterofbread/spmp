package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlatformBinder
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.startPlatformService
import com.toasterofbread.spmp.platform.unbindPlatformService
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance

private class PlayerServiceBinder(val service: PlatformPlayerService): PlatformBinder()

actual class PlatformPlayerService: ZmqSpMsPlayerService(), PlayerService {
    actual val load_state: PlayerServiceLoadState get() = socket_load_state
    actual override val context: AppContext get() = super.context

    override val listeners: List<PlayerListener>
        get() = Companion.listeners
    private lateinit var _service_player: PlayerServicePlayer
    actual override val service_player: PlayerServicePlayer
        get() = _service_player

    actual override val state: MediaPlayerState
        get() = _state
    actual override val is_playing: Boolean
        get() = _is_playing
    actual override val song_count: Int
        get() = playlist.size
    actual override val current_song_index: Int
        get() = _current_song_index
    actual override val current_position_ms: Long
        get() {
            if (current_song_time < 0) {
                return 0
            }
            if (!_is_playing) {
                return current_song_time
            }
            return System.currentTimeMillis() - current_song_time
        }
    actual override val duration_ms: Long
        get() = _duration_ms
    actual override val has_focus: Boolean
        get() = true // TODO
    actual override val radio_state: RadioInstance.RadioState
        get() = _radio_state
    actual override var repeat_mode: MediaPlayerRepeatMode
        get() = _repeat_mode
        set(value) {
            if (value == _repeat_mode) {
                return
            }
//            _repeat_mode = value
//            onEvent { it.onRepeatModeChanged(_repeat_mode) }
            sendRequest("setRepeatMode", value.ordinal)
        }
    actual override var volume: Float
        get() = _volume
        set(value) {
            if (value == _volume) {
                return
            }
//            _volume = value
//            onEvent { it.onVolumeChanged(_volume) }
            sendRequest("setVolume", value)
        }

    actual override fun isPlayingOverLatentDevice(): Boolean = false // TODO

    private fun onEvent(action: (PlayerListener) -> Unit) {
        for (listener in listeners) {
            action(listener)
            listener.onEvents()
        }
    }
    
    actual override fun play() {
//        if (playlist.isEmpty() || _is_playing) {
//            return
//        }
//
//        _is_playing = true
//        updateCurrentSongPosition(current_song_time)
//
//        onEvent { it.onPlayingChanged(_is_playing) }

        sendRequest("play")
    }

    actual override fun pause() {
//        if (playlist.isEmpty() || !_is_playing) {
//            return
//        }
//
//        _is_playing = false
//        updateCurrentSongPosition(System.currentTimeMillis() - current_song_time)
//
//        onEvent { it.onPlayingChanged(_is_playing) }

        sendRequest("pause")
    }

    actual override fun playPause() {
//        if (playlist.isEmpty()) {
//            return
//        }
//
//        val pos_ms = current_position_ms
//        _is_playing = !_is_playing
//        updateCurrentSongPosition(pos_ms)
//
//        onEvent { it.onPlayingChanged(_is_playing) }

        sendRequest("playPause")
    }

    actual override fun seekTo(position_ms: Long) {
//        if (playlist.isEmpty()) {
//            return
//        }
//
//        updateCurrentSongPosition(position_ms)
//
//        onEvent { it.onSeeked(position_ms) }
        sendRequest("seekToTime", position_ms)
    }

    actual override fun seekToSong(index: Int) {
//        require(index in playlist.indices) { "$index | ${playlist.toList()}" }
//
//        _current_song_index = index
//        _duration_ms = 0
//        updateCurrentSongPosition(0)
//
//        onEvent {
//            it.onSongTransition(playlist[index], true)
//            it.onEvents()
//        }

        sendRequest("seekToItem", index)
    }

    actual override fun seekToNext() {
//        if (playlist.isEmpty()) {
//            return
//        }
//
//        val target_index = when (_repeat_mode) {
//            MediaPlayerRepeatMode.NONE -> if (_current_song_index + 1 == playlist.size) return else _current_song_index + 1
//            MediaPlayerRepeatMode.ONE -> _current_song_index
//            MediaPlayerRepeatMode.ALL -> if (_current_song_index + 1 == playlist.size) 0 else _current_song_index + 1
//        }
//
//        _current_song_index = target_index
//        current_song_time = 0
//        _duration_ms = -1
//
//        onEvent {
//            it.onSongTransition(playlist[_current_song_index], true)
//            it.onEvents()
//        }

        sendRequest("seekToNext")
    }

    actual override fun seekToPrevious() {
//        if (playlist.isEmpty()) {
//            return
//        }
//
//        val target_index = when (_repeat_mode) {
//            MediaPlayerRepeatMode.NONE -> if (_current_song_index == 0) return else _current_song_index - 1
//            MediaPlayerRepeatMode.ONE -> _current_song_index
//            MediaPlayerRepeatMode.ALL -> if (_current_song_index == 0) playlist.size - 1 else _current_song_index - 1
//        }
//
//        _current_song_index = target_index
//        current_song_time = 0
//        _duration_ms = -1
//
//        onEvent {
//            it.onSongTransition(playlist[_current_song_index], true)
//            it.onEvents()
//        }

        sendRequest("seekToPrevious")
    }

    actual override fun getSong(): Song? = playlist.getOrNull(_current_song_index)

    actual override fun getSong(index: Int): Song? = playlist.getOrNull(index)

    actual override fun addSong(song: Song, index: Int) {
//        require(index in 0..song_count)

//        playlist.add(index, song)
        sendRequest("addItem", song.id, index)

//        if (_current_song_index < 0) {
//            _current_song_index = 0
//        }
//        service_player.session_started = true
//
//        onEvent {
//            it.onSongAdded(index, song)
//            it.onEvents()
//        }
    }

    actual override fun moveSong(from: Int, to: Int) {
//        require(from in 0 until song_count)
//        require(to in 0 until song_count)
//
//        if (playlist.size < 2) {
//            return
//        }
//
//        playlist.add(to, playlist.removeAt(from))
//
//        if (from == _current_song_index) {
//            _current_song_index = to
//        }
//        else if (to == _current_song_index) {
//            if (from < _current_song_index) {
//                _current_song_index--
//            }
//            else {
//                _current_song_index++
//            }
//        }
//
//        onEvent { it.onSongMoved(from, to) }
        sendRequest("moveItem", from, to)
    }

    actual override fun removeSong(index: Int) {
//        playlist.removeAt(index)
//        if (_current_song_index == playlist.size) {
//            _current_song_index--
//        }
//
//        onEvent { it.onSongRemoved(index) }
        sendRequest("removeItem", index)
    }

    actual override fun addListener(listener: PlayerListener) {}

    actual override fun removeListener(listener: PlayerListener) {}

    @Composable
    actual override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
    }

    override fun onBind(): PlatformBinder? {
        return PlayerServiceBinder(this)
    }

    actual override fun onCreate() {
        _service_player = object : PlayerServicePlayer(this) {
            override fun onUndoStateChanged() {
                for (listener in listeners) {
                    listener.onUndoStateChanged()
                }
            }
        }

        super.onCreate()
    }

    actual override fun onDestroy() {
        super.onDestroy()
    }

    actual companion object {
        private val listeners: MutableList<PlayerListener> = mutableListOf()

        actual fun isServiceRunning(context: AppContext): Boolean = true

        actual fun addListener(listener: PlayerListener) {
            listeners.add(listener)
        }

        actual fun removeListener(listener: PlayerListener) {
            listeners.remove(listener)
        }

        actual fun connect(
            context: AppContext,
            instance: PlatformPlayerService?,
            onConnected: (PlatformPlayerService) -> Unit,
            onDisconnected: () -> Unit,
        ): Any {
            return startPlatformService(
                context,
                PlatformPlayerService::class.java,
                onConnected = { binder ->
                    onConnected((binder as PlayerServiceBinder).service)
                },
                onDisconnected = onDisconnected
            )
        }

        actual fun disconnect(context: AppContext, connection: Any) {
            unbindPlatformService(context, connection)
        }
    }
}
