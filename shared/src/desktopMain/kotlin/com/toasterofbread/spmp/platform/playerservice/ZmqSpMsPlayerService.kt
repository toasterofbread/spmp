package com.toasterofbread.spmp.platform.playerservice

import com.google.gson.Gson
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.PlatformServiceImpl
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import com.toasterofbread.utils.common.launchSingle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMsg
import java.io.IOException
import java.net.InetAddress

private const val HANDSHAKE_TIMEOUT_MS: Long = 1000
private const val POLL_STATE_INTERVAL: Long = 100

abstract class ZmqSpMsPlayerService: PlatformServiceImpl(), PlayerService {
    abstract val listeners: List<PlayerListener>

    private fun getServerPort(): Int = Settings.KEY_SPMS_PORT.get(context)
    private fun getClientName(): String {
        val host: String = InetAddress.getLocalHost().hostName
        val os: String = System.getProperty("os.name")

        return getString("app_name") + " [$os, $host]"
    }

    private val zmq: ZContext = ZContext()
    private var socket: ZMQ.Socket? = null
    private val queued_messages: MutableList<Pair<String, List<Any>>> = mutableListOf()
    private val poll_coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val load_coroutine_scope: CoroutineScope = CoroutineScope(Job())

    protected var playlist: MutableList<Song> = mutableListOf()
        private set

    protected var _state: MediaPlayerState = MediaPlayerState.IDLE
    protected var _is_playing: Boolean = false
    protected var _current_song_index: Int = -1
    protected var _duration_ms: Long = -1
    protected var _radio_state: RadioInstance.RadioState = RadioInstance.RadioState()
    protected var _repeat_mode: MediaPlayerRepeatMode = MediaPlayerRepeatMode.NONE
    protected var _volume: Float = 1f
    protected var current_song_time: Long = -1

    protected fun sendRequest(action: String, vararg params: Any) {
        synchronized(queued_messages) {
            queued_messages.add(Pair(action, params.asList()))
        }
    }

    protected fun updateIsPlaying(playing: Boolean) {
        if (playing == _is_playing) {
            return
        }

        val position_ms = current_position_ms
        _is_playing = playing
        updateCurrentSongPosition(position_ms)
    }

    protected fun updateCurrentSongPosition(position_ms: Long) {
        require(position_ms >= 0) { position_ms }
        if (_is_playing) {
            current_song_time = System.currentTimeMillis() - position_ms
        }
        else {
            current_song_time = position_ms
        }
    }

    override fun onCreate() {
        socket = zmq.createSocket(SocketType.DEALER).apply {
            check(connect("tcp://127.0.0.1:${getServerPort()}"))

            runBlocking {
                connectToServer()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        poll_coroutine_scope.cancel()
        load_coroutine_scope.cancel()
    }

    private data class ServerState(
        val queue: List<String>,
        val state: Int,
        val is_playing: Boolean,
        val current_song_index: Int,
        val current_position_ms: Int,
        val duration_ms: Int,
        val repeat_mode: Int,
        val volume: Float
    )

    private suspend fun ZMQ.Socket.connectToServer() = withContext(Dispatchers.IO) {
        val handshake_message = ZMsg()
        handshake_message.add(getClientName())
        handshake_message.send(this@connectToServer)

        val reply: ZMsg? = recvMsg(HANDSHAKE_TIMEOUT_MS)
        if (reply == null) {
            throw IOException("Did not receive handshake reply from server within timeout (${HANDSHAKE_TIMEOUT_MS}ms)")
        }

        val state_data: String = reply.first.data.decodeToString().trimEnd { it == '\u0000' }

        val state: ServerState
        try {
            state = Gson().fromJson(state_data)
        }
        catch (e: Throwable) {
            throw RuntimeException("Parsing handshake reply data failed '$state_data'", e)
        }

        assert(playlist.isEmpty())

        val jobs: MutableList<Job> = mutableListOf()
        val items: MutableList<Song?> = mutableListOf()

        for ((i, id) in state.queue.withIndex()) {
            items.add(null)
            jobs.add(
                launch {
                    val song: Song = SongRef(id)
                    song.loadData(context).onSuccess { data ->
                        data.saveToDatabase(context.database)
                    }
                    items.add(i, song)
                }
            )
        }
        jobs.joinAll()

        playlist.addAll(items.filterNotNull())

        if (playlist.isNotEmpty()) {
            service_player.session_started = true
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

            val song = playlist.getOrNull(_current_song_index)
            listeners.forEach { it.onSongTransition(song, false) }
        }
        if (state.volume != _volume) {
            _volume = state.volume
            listeners.forEach { it.onVolumeChanged(_volume) }
        }
        if (state.repeat_mode != _repeat_mode.ordinal) {
            _repeat_mode = MediaPlayerRepeatMode.values()[state.repeat_mode]
            listeners.forEach { it.onRepeatModeChanged(_repeat_mode) }
        }

        _duration_ms = state.duration_ms.toLong()
        updateCurrentSongPosition(state.current_position_ms.toLong())

        poll_coroutine_scope.launchSingle(Dispatchers.IO) {
            while (true) {
                delay(POLL_STATE_INTERVAL)
                pollServerState()
            }
        }
    }

    private fun ZMQ.Socket.pollServerState() {
        val events = ZMsg.recvMsg(socket!!)

        for (i in 0 until events.size) {
            val event_str: String = events.pop().data.decodeToString().removeSuffix("\u0000")
            if (event_str.isEmpty()) {
                continue
            }

            val event: Map<String, Any>
            try {
                event = Gson().fromJson(event_str) ?: continue
            }
            catch (e: Throwable) {
                throw RuntimeException("Parsing event failed '$event_str'", e)
            }
            println("Processing event: $event")

            val type = (event["type"] as String?) ?: continue
            val properties = (event["properties"] as? Map<String, Any>) ?: continue

            when (type) {
                "SongTransition" -> {
                    _current_song_index = (properties["index"] as Double).toInt()
                    _duration_ms = -1
                    updateCurrentSongPosition(0)
                    listeners.forEach { it.onSongTransition(getSong(_current_song_index), false) }
                }
                "PropertyChanged" -> {
                    val value: Any? = properties["value"]
                    when (properties["key"] as String) {
                        "state" -> {
                            if (value != _state.ordinal) {
                                _state = MediaPlayerState.values()[(value as Double).toInt()]
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
                                _repeat_mode = MediaPlayerRepeatMode.values()[(value as Double).toInt()]
                                listeners.forEach { it.onRepeatModeChanged(_repeat_mode) }
                            }
                        }
                        "volume" -> {
                            if (value != _volume) {
                                _volume = (value as Double).toFloat()
                                listeners.forEach { it.onVolumeChanged(_volume) }
                            }
                        }
                        "duration_ms" -> {
                            val duration = (value as Double).toLong()
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
                    val song = SongData(event["song_id"] as String)
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
        synchronized(queued_messages) {
            if (queued_messages.isEmpty()) {
                reply.add(byteArrayOf())
            }
            else {
                for (message in queued_messages) {
                    reply.add(message.first)
                    reply.add(Gson().toJson(message.second))
                }
            }
            reply.send(socket!!)

            queued_messages.clear()
        }
    }

    private fun ZMQ.Socket.recvMsg(timeout_ms: Long?): ZMsg? {
        receiveTimeOut = timeout_ms?.toInt() ?: -1
        val msg: ZMsg? = ZMsg.recvMsg(this)
        receiveTimeOut = -1
        return msg
    }
}
