package com.spectre7.spmp.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.utils.lazyAssert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Poller
import org.zeromq.ZMQ.Socket
import org.zeromq.ZMsg
import java.io.StringReader
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
    private var last_event_id: Int = -1

    private class ServerReply<T>(val result: T? = null, val error: String?) {
        fun checkAndGetResult(): T? {
            if (error != null) {
                SpMp.error_manager.onError("ServerReply", RuntimeException(error))
                return null
            }
            return result
        }
    }

    override fun onCreate() {
        super.onCreate()

        socket = zmq.createSocket(SocketType.REQ).apply {
            check(connect("tcp://127.0.0.1:${getServerPort()}"))
        }

        playlist.clear()
        sendPropertyRequest<List<String>>("playlist")?.apply {
            playlist.addAll(map { Song.fromId(it) })
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
        val data = sendRequest<Map<String, Any>>("getEvents", last_event_id) ?: return

        val new_playlist = data["platlist"] as List<String>?
        if (new_playlist != null) {
            for (item in playlist.zip(new_playlist).withIndex()) {
                if (item.value.first.id != item.value.second) {
                    playlist[item.index] = Song.fromId(item.value.second)
                }
            }

            if (new_playlist.size > playlist.size) {
                for (i in playlist.size - 1 until new_playlist.size) {
                    playlist.add(Song.fromId(new_playlist[i]))
                }
            }
            else if (playlist.size > new_playlist.size) {
                for (i in new_playlist.size - 1 until playlist.size) {
                    playlist.removeLast()
                }
            }

            lazyAssert {
                if (playlist.size != new_playlist.size) {
                    return@lazyAssert false
                }
                for (item in playlist.zip(new_playlist)) {
                    if (item.first.id != item.second) {
                        return@lazyAssert false
                    }
                }
                return@lazyAssert true
            }
        }

        val events = data["events"] as List<Map<String, Any?>>
        events.lastOrNull()?.also { last_event_id = it["id"] as Int }

        for (event in events) {
            when (event["type"] as String) {
                "SongTransition" -> {} // TODO
                "PropertyChanged" -> {} // TODO
                "SongAdded" -> {  }
                "SongRemoved" -> {  }
                "SongMoved" -> {  }
                else -> throw NotImplementedError(event["type"] as String)
            }
        }
    }

    private inline fun <reified T> sendRequest(msg: ZMsg): T? {
        synchronized(socket!!) {
            check(msg.send(socket!!))

            val poller = zmq.createPoller(1)
            poller.register(socket!!, Poller.POLLIN)
            poller.poll() // TODO timeout

            if (poller.pollin(0)) {
                val reply = ZMsg.recvMsg(socket!!)
                val charset = Charset.defaultCharset()

                val result: T?
                val error: String?

                val result_str = reply.pop().getString(charset)
                if (result_str == "null") {
                    result = null
                }
                else {
                    result = Klaxon().parse<Map<String, T>>("{\"a\": $result_str}")!!["a"]
                }

                if (reply.isNotEmpty()) error = reply.pop().getString(charset)
                else error = null

                return ServerReply(result, error).checkAndGetResult()
            }
            else {
                println("NOREPLY")
                return null
            }
        }
    }

    private inline fun <reified T> sendRequest(action: String, vararg params: Any): T? {
        ZMsg().also { msg ->
            msg.add(action)
            for (param in params) {
                msg.add(Klaxon().toJsonString(param))
            }
            return sendRequest(msg)
        }
    }

    private fun sendRequest(action: String, vararg params: Any) {
        sendRequest<Any?>(action, *params)
    }

    private inline fun <reified T> sendPropertyRequest(key: String, value: Any? = null): T? {
        return if (value == null) sendRequest<T>("get", key)
        else return sendRequest<T>("set", key, value)
    }

    private fun sendPropertyRequest(key: String, value: Any) {
        sendPropertyRequest<Any?>(key, value)
    }

    actual var session_started: Boolean by mutableStateOf(true)

    actual val is_playing: Boolean
        get() = sendPropertyRequest("is_playing") as Boolean? ?: false
    actual val song_count: Int
//        get() = sendPropertyRequest("song_count") as Int? ?: 0
        get() = playlist.size
    actual val current_song_index: Int
        get() = sendPropertyRequest("current_song_index") as Int? ?: 0
    actual val current_position_ms: Long
        get() = (sendPropertyRequest("current_position_ms") as Int?)?.toLong() ?: 0
    actual val duration_ms: Long
        get() = (sendPropertyRequest("duration_ms") as Int?)?.toLong() ?: 0
    actual val shuffle_enabled: Boolean
        get() = sendPropertyRequest("shuffle_enabled") as Boolean? ?: false

    // TODO | Remove both of these (move logic to UI)
    actual val has_next_song: Boolean
        get() {
            val count = song_count
            if (count == 0) {
                return false
            }
            if (count == 1 && repeat_mode == MediaPlayerRepeatMode.REPEAT_MODE_OFF) {
                return false
            }

            if (current_song_index + 1 == count) {
                return repeat_mode != MediaPlayerRepeatMode.REPEAT_MODE_OFF
            }

            return true
        }
    actual val has_prev_song: Boolean
        get() {
            val count = song_count
            if (count == 0) {
                return false
            }
            if (count == 1 && repeat_mode == MediaPlayerRepeatMode.REPEAT_MODE_OFF) {
                return false
            }

            if (current_song_index == 0) {
                return repeat_mode != MediaPlayerRepeatMode.REPEAT_MODE_OFF
            }

            return true
        }

    actual val state: MediaPlayerState
        get() = TODO()
    actual var repeat_mode: MediaPlayerRepeatMode
        get() = (sendPropertyRequest("repeat_mode") as Int?)?.let { MediaPlayerRepeatMode.values()[it] } ?: MediaPlayerRepeatMode.REPEAT_MODE_OFF
        set(value) { sendPropertyRequest("repeat_mode", value.ordinal) }
    actual var volume: Float
        get() = sendPropertyRequest("volume") as Float? ?: 0f
        set(value) { sendPropertyRequest("volume", value) }
    actual val has_focus: Boolean
        get() = sendPropertyRequest("has_focus") as Boolean? ?: false

    actual open fun play() {
        sendRequest("play")
    }

    actual open fun pause() {
        sendRequest("pause")
    }

    actual open fun playPause() {
        sendRequest("playPause")
    }

    actual open fun seekTo(position_ms: Long) {
        sendRequest("seekTo", position_ms)
    }

    actual open fun seekTo(index: Int, position_ms: Long) {
        sendRequest("seekToIndex", index, position_ms)
    }

    actual open fun seekToNext() {
        sendRequest("seekToNext")
    }

    actual open fun seekToPrevious() {
        sendRequest("seekToPrevious")
    }

    actual fun getSong(): Song? = sendRequest<String>("getSong")?.let { Song.fromId(it) }

    actual fun getSong(index: Int): Song? = playlist[index]

    actual fun addSong(song: Song) {
        playlist.add(song)
        sendRequest("addSong", song.id)
    }

    actual fun addSong(song: Song, index: Int) {
        playlist.add(index, song)
        sendRequest("addSong", song.id, index)
    }

    actual fun moveSong(from: Int, to: Int) {
        playlist.add(to, playlist.removeAt(from))
        sendRequest("moveSong", from, to)
    }

    actual fun removeSong(index: Int) {
        playlist.removeAt(index)
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