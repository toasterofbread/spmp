package com.spectre7.spmp.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.Song
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ.Socket
import org.zeromq.ZMsg
import java.nio.charset.Charset
import kotlin.concurrent.thread

const val POLL_STATE_INTERVAL = 100L // ms

actual open class MediaPlayerService actual constructor() : PlatformService() {
    actual open class Listener actual constructor() {
        actual open fun onSongTransition(song: Song?) {}
        actual open fun onStateChanged(state: MediaPlayerState) {}
        actual open fun onPlayingChanged(is_playing: Boolean) {}
        actual open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {}
        actual open fun onVolumeChanged(volume: Float) {}
        actual open fun onDurationChanged(duration_ms: Long) {}
        actual open fun onSeeked(position_ms: Long) {}

        actual open fun onSongAdded(index: Int, song: Song) {}
        actual open fun onSongRemoved(index: Int) {}
        actual open fun onSongMoved(from: Int, to: Int) {}

        actual open fun onEvents() {}
    }

    private data class ServerPlayerState(
        val state: Int,
        val is_playing: Boolean,
        val current_song_index: Int,
        val current_position_ms: Int,
        val duration_ms: Int,
        val repeat_mode: Int,
        val volume: Float,
        val playlist: List<String>
    )

    private fun getServerPort(): Int = Settings.KEY_SPMS_PORT.get(this)

    private val zmq = ZContext()
    private var socket: Socket? = null
    private var poll_thread: Thread? = null
    private var playlist: MutableList<Song> = mutableListOf()
    private val queued_messages: MutableList<Pair<String, List<Any>>> = mutableListOf()
    private val listeners: MutableList<Listener> = mutableListOf()

    override fun onCreate() {
        super.onCreate()

        socket = zmq.createSocket(SocketType.DEALER).apply {
            check(connect("tcp://127.0.0.1:${getServerPort()}"))

            val msg = ZMsg()
            msg.add("SpMp (Android)")
            msg.send(this)

            val reply = ZMsg.recvMsg(this)

            val state: ServerPlayerState = Klaxon().parse(reply.first.getString(Charset.defaultCharset()))
                ?: throw RuntimeException("Invalid state handshake from server $reply")

            println("Initial state: $state")

            assert(playlist.isEmpty())
            for (id in state.playlist) {
                playlist.add(Song.fromId(id).loadData().getOrThrowHere() as Song)
            }

            if (state.state != _state.ordinal) {
                _state = MediaPlayerState.values()[state.state]
                listeners.forEach { it.onStateChanged(_state) }
            }
            if (state.is_playing != _is_playing) {
                _is_playing = state.is_playing
                listeners.forEach { it.onPlayingChanged(_is_playing) }
            }
            if (state.current_song_index != _current_song_index) {
                _current_song_index = state.current_song_index
                val song = playlist[_current_song_index]
                listeners.forEach { it.onSongTransition(song) }
            }
            if (state.volume != _volume) {
                _volume = state.volume
                listeners.forEach { it.onVolumeChanged(_volume) }
            }
            if (state.repeat_mode != _repeat_mode.ordinal) {
                _repeat_mode = MediaPlayerRepeatMode.values()[state.repeat_mode]
                listeners.forEach { it.onRepeatModeChanged(_repeat_mode) }
            }

            _volume = state.volume
            _duration_ms = state.duration_ms.toLong()
            updateCurrentSongPosition(state.current_position_ms.toLong())
        }

        poll_thread = thread {
            while (true) {
                try {
                    Thread.sleep(POLL_STATE_INTERVAL)
                }
                catch (e: InterruptedException) {
                    return@thread
                }

                pollServerState()
            }
        }
    }

    override fun onDestroy() {
        poll_thread?.apply {
            interrupt()
            join()
        }
        socket?.apply {
            close()
            socket = null
        }
        super.onDestroy()
    }

    @Suppress("UNCHECKED_CAST")
    private fun pollServerState() {

        val events = ZMsg.recvMsg(socket!!)

        for (i in 0 until events.size) {
            val event_str = events.popString()
            if (event_str.isEmpty()) {
                continue
            }

            println("EVENT STR $event_str")

            val event: Map<String, Any> = Klaxon().parse(event_str) ?: continue
            println("Processing event: $event")

            val type = (event["type"] as String?) ?: continue
            when (type) {
                "SongTransition" -> {
                    _current_song_index = event["index"] as Int
                    _duration_ms = -1
                    updateCurrentSongPosition(0)
                    listeners.forEach { it.onSongTransition(getSong()) }
                }
                "PropertyChanged" -> {
                    val value = event["value"]
                    when (event["key"] as String) {
                        "state" -> {
                            if (value != _state.ordinal) {
                                _state = MediaPlayerState.values()[value as Int]
                                listeners.forEach { it.onStateChanged(_state) }
                            }
                        }
                        "is_playing" -> {
                            if (value != _is_playing) {
                                updateIsPlaying(value as Boolean)
                                listeners.forEach { it.onPlayingChanged(_is_playing) }
                            }
                        }
                        "repeat_mode" -> {
                            if (value != _repeat_mode.ordinal) {
                                _repeat_mode = MediaPlayerRepeatMode.values()[value as Int]
                                listeners.forEach { it.onRepeatModeChanged(_repeat_mode) }
                            }
                        }
                        "volume" -> {
                            if (value != _volume) {
                                _volume = event["value"] as Float
                                listeners.forEach { it.onVolumeChanged(_volume) }
                            }
                        }
                        "duration_ms" -> {
                            val duration = (value as Int).toLong()
                            if (duration != _duration_ms) {
                                _duration_ms = duration
                                listeners.forEach { it.onDurationChanged(_duration_ms) }
                            }
                        }
                        else -> throw NotImplementedError(type)
                    }
                }
                "Seeked" -> {
                    val position_ms = (event["position_ms"] as Int).toLong()
                    updateCurrentSongPosition(position_ms)
                    listeners.forEach { it.onSeeked(position_ms) }
                }
                "SongAdded" -> {
                    val song = Song.fromId(event["song_id"] as String)
                    val index = event["index"] as Int
                    playlist.add(index, song)
                    listeners.forEach { it.onSongAdded(index, song) }
                }
                "SongRemoved" -> {
                    val index = event["index"] as Int
                    playlist.removeAt(index)
                    listeners.forEach { it.onSongRemoved(index) }
                }
                "SongMoved" -> {
                    val to = event["to_index"] as Int
                    val from = event["from_index"] as Int
                    playlist.add(to, playlist.removeAt(from))
                    listeners.forEach { it.onSongMoved(from, to) }
                }
                else -> throw NotImplementedError(event["type"] as String)
            }
        }

        val reply = ZMsg()
        if (queued_messages.isEmpty()) {
            reply.add(byteArrayOf())
        }
        else {
            for (message in queued_messages) {
                reply.add(message.first)
                reply.add(Klaxon().toJsonString(message.second))
            }
        }
        reply.send(socket!!)

        queued_messages.clear()
    }

    private fun sendRequest(action: String, vararg params: Any) {
        queued_messages.add(Pair(action, params.asList()))
    }

    private fun updateIsPlaying(playing: Boolean) {
        if (playing == _is_playing) {
            return
        }

        val position_ms = current_position_ms
        _is_playing = playing
        updateCurrentSongPosition(position_ms)
    }
    private fun updateCurrentSongPosition(position_ms: Long) {
        if (_is_playing) {
            current_song_time = System.currentTimeMillis() - position_ms
        }
        else {
            current_song_time = position_ms
        }

        println("current_song_time $current_song_time")
    }

    actual var session_started: Boolean by mutableStateOf(true)

    private var _state: MediaPlayerState = MediaPlayerState.IDLE
    private var _is_playing: Boolean = false
    private var _current_song_index: Int = -1
    private var _duration_ms: Long = -1
    private var _repeat_mode: MediaPlayerRepeatMode = MediaPlayerRepeatMode.OFF
    private var _volume: Float = 1f
    private var current_song_time: Long = -1

    actual val is_playing: Boolean
        get() = _is_playing
    actual val state: MediaPlayerState
        get() = _state
    actual val song_count: Int
        get() = playlist.size
    actual val current_song_index: Int
        get() = _current_song_index
    actual val current_position_ms: Long
        get() {
            if (current_song_time < 0) {
                return 0
            }
            if (!_is_playing) {
                return current_song_time
            }
            return System.currentTimeMillis() - current_song_time
        }
    actual val duration_ms: Long
        get() = _duration_ms
    actual val undo_count: Int = 0 // TODO
    actual val redo_count: Int = 0 // TODO
    actual var repeat_mode: MediaPlayerRepeatMode
        get() = _repeat_mode
        set(value) { 
            if (value == _repeat_mode) {
                return
            }
            _repeat_mode = value
            listeners.forEach { it.onRepeatModeChanged(_repeat_mode) }
            sendRequest("setRepeatMode", value.ordinal)
        }
    actual var volume: Float
        get() = _volume
        set(value) { 
            if (value == _volume) {
                return
            }
            _volume = value
            listeners.forEach { it.onVolumeChanged(_volume) }
            sendRequest("setVolume", value)
        }
    actual val has_focus: Boolean = true

    actual open fun play() {
        if (playlist.isEmpty() || _is_playing) {
            return
        }

        _is_playing = true
        updateCurrentSongPosition(current_song_time)

        listeners.forEach { it.onPlayingChanged(_is_playing) }

        sendRequest("play")
    }

    actual open fun pause() {
        if (playlist.isEmpty() || !_is_playing) {
            return
        }

        _is_playing = false
        updateCurrentSongPosition(System.currentTimeMillis() - current_song_time)

        listeners.forEach { it.onPlayingChanged(_is_playing) }

        sendRequest("pause")
    }

    actual open fun playPause() {
        if (playlist.isEmpty()) {
            return
        }

        val pos_ms = current_position_ms
        _is_playing = !_is_playing
        updateCurrentSongPosition(pos_ms)

        listeners.forEach { it.onPlayingChanged(_is_playing) }

        sendRequest("playPause")
    }

    actual open fun seekTo(position_ms: Long) {
        if (playlist.isEmpty()) {
            return 
        }

        updateCurrentSongPosition(position_ms)

        listeners.forEach { it.onSeeked(position_ms) }
        sendRequest("seekTo", position_ms)
    }

    actual open fun seekToSong(index: Int) {
        require(index in playlist.indices)

        _current_song_index = index
        _duration_ms = 0
        updateCurrentSongPosition(0)

        listeners.forEach {
            it.onSongTransition(playlist[index])
        }

        sendRequest("seekToSong", index)
    }

    actual open fun seekToNext() {
        if (playlist.isEmpty()) {
            return
        }

        val target_index = when (_repeat_mode) {
            MediaPlayerRepeatMode.OFF -> if (_current_song_index + 1 == playlist.size) return else _current_song_index + 1
            MediaPlayerRepeatMode.ONE -> _current_song_index
            MediaPlayerRepeatMode.ALL -> if (_current_song_index + 1 == playlist.size) 0 else _current_song_index + 1
        }

        _current_song_index = target_index
        current_song_time = 0
        _duration_ms = -1

        listeners.forEach { it.onSongTransition(playlist[_current_song_index]) }

        sendRequest("seekToNext")
    }

    actual open fun seekToPrevious() {
        if (playlist.isEmpty()) {
            return
        }

        val target_index = when (_repeat_mode) {
            MediaPlayerRepeatMode.OFF -> if (_current_song_index == 0) return else _current_song_index - 1
            MediaPlayerRepeatMode.ONE -> _current_song_index
            MediaPlayerRepeatMode.ALL -> if (_current_song_index == 0) playlist.size - 1 else _current_song_index - 1
        }

        _current_song_index = target_index
        current_song_time = 0
        _duration_ms = -1
        
        listeners.forEach { it.onSongTransition(playlist[_current_song_index]) }

        sendRequest("seekToPrevious")
    }

    actual fun getSong(): Song? = playlist.getOrNull(_current_song_index)

    actual fun getSong(index: Int): Song? = playlist.getOrNull(index)

    actual fun addSong(song: Song) = addSong(song, song_count)

    actual fun addSong(song: Song, index: Int) {
        require(index in 0..song_count)

        playlist.add(index, song)
        listeners.forEach { it.onSongAdded(index, song) }
        sendRequest("addSong", song.id, index)
    }

    actual fun moveSong(from: Int, to: Int) {
        require(from in 0 until song_count)
        require(to in 0 until song_count)

        if (playlist.size < 2) {
            return
        }

        playlist.add(to, playlist.removeAt(from))

        if (from == _current_song_index) {
            _current_song_index = to
        }
        else if (to == _current_song_index) {
            if (from < _current_song_index) {
                _current_song_index--
            }
            else {
                _current_song_index++
            }
        }

        listeners.forEach { it.onSongMoved(from, to) }
        sendRequest("moveSong", from, to)
    }

    actual fun removeSong(index: Int) {
        playlist.removeAt(index)
        if (_current_song_index == playlist.size) {
            _current_song_index--
        }

        listeners.forEach { it.onSongRemoved(index) }
        sendRequest("removeSong", index)
    }

    actual fun addListener(listener: Listener) { listeners.add(listener) }
    actual fun removeListener(listener: Listener) { listeners.remove(listener) }
}
