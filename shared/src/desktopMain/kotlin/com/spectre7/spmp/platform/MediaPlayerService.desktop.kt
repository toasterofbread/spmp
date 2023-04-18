package com.spectre7.spmp.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import kotlinx.coroutines.CoroutineScope
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ.Socket
import org.zeromq.ZMsg
import java.nio.charset.Charset
import kotlin.concurrent.thread

const val POLL_STATE_INTERVAL = 100L // ms

actual open class MediaPlayerService actual constructor() : PlatformService() {
    actual open class Listener actual constructor() {
        actual open fun onMediaItemTransition(song: Song?) {
        }

        actual open fun onStateChanged(state: MediaPlayerState) {
        }

        actual open fun onPlayingChanged(is_playing: Boolean) {
        }

        actual open fun onShuffleEnabledChanged(shuffle_enabled: Boolean) {
        }

        actual open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {
        }

        actual open fun onEvents() {
        }
    }

    private fun getServerPort(): Int = Settings.KEY_SPMS_PORT.get(this)

    private val zmq = ZContext()
    private var socket: Socket? = null
    private var poll_thread: Thread? = null
    private var playlist: MutableList<Song> = mutableListOf()
    private val queued_messages: MutableList<Pair<String, List<Any>>> = mutableListOf()

//    private class ServerReply<T>(val result: T? = null, val error: String?) {
//        fun checkAndGetResult(): T? {
//            if (error != null) {
//                SpMp.error_manager.onError("ServerReply", RuntimeException(error))
//                return null
//            }
//            return result
//        }
//    }

    private class ServerPlayerState(
        val is_playing: Boolean,
        val current_song_index: Int,
        val current_position_ms: Int,
        val duration_ms: Int,
        val shuffle_enabled: Boolean,
        val repeat_mode: Int,
        val volume: Float,
        val playlist: List<String>
    )

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

            _is_playing = state.is_playing
            _current_song_index = state.current_song_index
            updateCurrentSongPosition(state.current_position_ms.toLong())
            _duration_ms = state.duration_ms.toLong()
            _shuffle_enabled = state.shuffle_enabled
            _repeat_mode = MediaPlayerRepeatMode.values()[state.repeat_mode]
            _volume = state.volume

            assert(playlist.isEmpty())
            for (id in state.playlist) {
                playlist.add(Song.fromId(id))
            }
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

            val event: Map<String, Any> = Klaxon().parse(event_str) ?: continue
            when (event["type"] as String) {
                "SongTransition" -> {
                    _current_song_index = event["index"] as Int
                    updateCurrentSongPosition(0)
                }
                "PropertyChanged" -> {
                    when (event["key"] as String) {
                        "is_playing" -> {
                            updateIsPlaying(event["value"] as Boolean)
                        }
                        "repeat_mode" -> {
                            _repeat_mode = MediaPlayerRepeatMode.values()[event["value"] as Int]
                        }
                        "volume" -> {
                            _volume = event["value"] as Float
                        }
                        else -> throw NotImplementedError(event["key"] as String)
                    }
                } // TODO
                "Seeked" -> updateCurrentSongPosition((event["position_ms"] as Int).toLong())
                "SongAdded" -> {
                    val song = Song.fromId(event["song_id"] as String)
                    val index = event["index"] as Int
                    playlist.add(index, song)
                }
                "SongRemoved" -> playlist.removeAt(event["index"] as Int)
                "SongMoved" -> playlist.add(event["to_index"] as Int, playlist.removeAt(event["from_index"] as Int))
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
    }

    actual var session_started: Boolean by mutableStateOf(true)

    private var _state: MediaPlayerState = MediaPlayerState.STATE_IDLE
    private var _is_playing: Boolean = false
    private var _current_song_index: Int = 0
    private var _duration_ms: Long = -1
    private var _shuffle_enabled: Boolean = false
    private var _repeat_mode: MediaPlayerRepeatMode = MediaPlayerRepeatMode.REPEAT_MODE_OFF
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
    actual val shuffle_enabled: Boolean
        get() = _shuffle_enabled
    actual var repeat_mode: MediaPlayerRepeatMode
        get() = _repeat_mode
        set(value) { sendRequest("setRepeatMode", value.ordinal) }
    actual var volume: Float
        get() = _volume
        set(value) { sendRequest("setVolume", value) }
    actual val has_focus: Boolean = true

    actual open fun play() {
        if (playlist.isEmpty()) {
            return
        }

        _is_playing = true
        current_song_time = System.currentTimeMillis() - current_song_time

        sendRequest("play")
    }

    actual open fun pause() {
        if (playlist.isEmpty()) {
            return
        }

        _is_playing = false
        current_song_time = System.currentTimeMillis() - current_song_time

        sendRequest("pause")
    }

    actual open fun playPause() {
        if (playlist.isEmpty()) {
            return
        }

        _is_playing = !_is_playing
        current_song_time = System.currentTimeMillis() - current_song_time

        sendRequest("playPause")
    }

    actual open fun seekTo(position_ms: Long) {
        require(playlist.isNotEmpty())
        require(position_ms in 0 until _duration_ms)

        if (_is_playing) {
            current_song_time -= position_ms - (System.currentTimeMillis() - current_song_time)
        }
        else {
            current_song_time = position_ms
        }

        sendRequest("seekTo", position_ms)
    }

    actual open fun seekTo(index: Int, position_ms: Long) {
        require(index in playlist.indices)
        require(position_ms in 0 until _duration_ms)

        _current_song_index = index

        if (_is_playing) {
            current_song_time -= position_ms - (System.currentTimeMillis() - current_song_time)
        }
        else {
            current_song_time = position_ms
        }

        sendRequest("seekToIndex", index, position_ms)
    }

    actual open fun seekToNext() {
        if (playlist.isEmpty()) {
            return
        }

        val target_index = when (_repeat_mode) {
            MediaPlayerRepeatMode.REPEAT_MODE_OFF -> if (_current_song_index + 1 == playlist.size) return else _current_song_index + 1
            MediaPlayerRepeatMode.REPEAT_MODE_ONE -> _current_song_index
            MediaPlayerRepeatMode.REPEAT_MODE_ALL -> if (_current_song_index + 1 == playlist.size) 0 else _current_song_index + 1
        }

        _current_song_index = target_index
        _is_playing = false
        current_song_time = 0
        _duration_ms = 0

        sendRequest("seekToNext")
    }

    actual open fun seekToPrevious() {
        if (playlist.isEmpty()) {
            return
        }

        val target_index = when (_repeat_mode) {
            MediaPlayerRepeatMode.REPEAT_MODE_OFF -> if (_current_song_index == 0) return else _current_song_index - 1
            MediaPlayerRepeatMode.REPEAT_MODE_ONE -> _current_song_index
            MediaPlayerRepeatMode.REPEAT_MODE_ALL -> if (_current_song_index == 0) playlist.size - 1 else _current_song_index - 1
        }

        _current_song_index = target_index
        _is_playing = false
        current_song_time = 0
        _duration_ms = 0

        sendRequest("seekToPrevious")
    }

    actual fun getSong(): Song? = playlist.getOrNull(_current_song_index)

    actual fun getSong(index: Int): Song? = playlist.getOrNull(index)

    actual fun addSong(song: Song) {
        playlist.add(song)
        sendRequest("addSong", song.id)
    }

    actual fun addSong(song: Song, index: Int) {
        playlist.add(index, song)
        sendRequest("addSong", song.id, index)
    }

    actual fun moveSong(from: Int, to: Int) {
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

        sendRequest("moveSong", from, to)
    }

    actual fun removeSong(index: Int) {
        if (index !in playlist.indices) {
            return
        }

        playlist.removeAt(index)
        if (_current_song_index == playlist.size) {
            _current_song_index--
        }

        sendRequest("removeSong", index)
    }

    actual fun addListener(listener: Listener) {
    }

    actual fun removeListener(listener: Listener) {
    }

    actual companion object {
        actual fun CoroutineScope.playerLaunch(action: CoroutineScope.() -> Unit) {
            action()
        }
    }
}