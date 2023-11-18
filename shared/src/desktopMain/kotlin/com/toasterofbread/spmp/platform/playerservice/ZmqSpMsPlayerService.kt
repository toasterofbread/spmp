package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.PlatformServiceImpl
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMsg
import java.net.InetAddress

private const val POLL_STATE_INTERVAL: Long = 100

abstract class ZmqSpMsPlayerService: PlatformServiceImpl(), PlayerService {
    abstract val listeners: List<PlayerListener>

    var socket_load_state: PlayerServiceLoadState by mutableStateOf(PlayerServiceLoadState(true))
        private set

    private fun getServerPort(): Int = Settings.KEY_SERVER_PORT.get(context)
    private fun getServerIp(): String = Settings.KEY_SERVER_IP.get(context)

    private fun getClientName(): String {
        val host: String = InetAddress.getLocalHost().hostName
        val os: String = System.getProperty("os.name")

        return getString("app_name") + " [$os, $host]"
    }

    private val prefs_listener: PlatformPreferences.Listener = object : PlatformPreferences.Listener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
                Settings.KEY_SERVER_IP.name,
                Settings.KEY_SERVER_PORT.name -> {
                    restart_connection = true
                    cancel_connection = true
                }
            }
        }
    }

    private val zmq: ZContext = ZContext()
    private var socket: ZMQ.Socket? = null
    private val queued_messages: MutableList<Pair<String, List<Any>>> = mutableListOf()
    private val poll_coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val connect_coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private var cancel_connection: Boolean = false
    private var restart_connection: Boolean = false

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

    private fun getServerURL(): String =
        "tcp://${getServerIp()}:${getServerPort()}"

    override fun onCreate() {
        context.getPrefs().addListener(prefs_listener)

        socket = zmq.createSocket(SocketType.DEALER).apply {
            suspend fun tryConnection() {
                val server_url: String = getServerURL()
                check(connect(getServerURL()))

                if (!connectToServer(server_url)) {
                    disconnect(server_url)
                }
            }

            connect_coroutine_scope.launch {
                do {
                    cancel_connection = false
                    restart_connection = false
                    tryConnection()
                }
                while (restart_connection)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        poll_coroutine_scope.cancel()
        connect_coroutine_scope.cancel()
        context.getPrefs().removeListener(prefs_listener)
    }

    private data class ServerState(
        val queue: List<String>,
        val state: Int,
        val is_playing: Boolean,
        val current_item_index: Int,
        val current_position_ms: Int,
        val duration_ms: Int,
        val repeat_mode: Int,
        val volume: Float
    )

    private suspend fun ZMQ.Socket.connectToServer(url: String): Boolean = withContext(Dispatchers.IO) {
        val handshake_message = ZMsg()
        handshake_message.add(getClientName())
        handshake_message.send(this@connectToServer)

        socket_load_state = PlayerServiceLoadState(
            true,
            getString("loading_message_connecting_to_server_at_\$x").replace("\$x", url)
        )

        println("Waiting for reply from server at $url...")

        var reply: ZMsg? = null
        while (reply == null) {
            reply = recvMsg(500)

            if (cancel_connection) {
                return@withContext false
            }
        }

        val state_data: String = reply.first.data.decodeToString().trimEnd { it == '\u0000' }
        println("Received handshake reply from server with the following state data:\n$state_data")

        val state: ServerState
        try {
            state = Gson().fromJson(state_data)
        }
        catch (e: Throwable) {
            throw RuntimeException("Parsing handshake reply data failed '$state_data'", e)
        }

        socket_load_state = PlayerServiceLoadState(
            true,
            getString("loading_message_setting_initial_state")
        )

        assert(playlist.isEmpty())

        val items: Array<Song?> = arrayOfNulls(state.queue.size)

        state.queue.mapIndexed { i, id ->
            launch {
                val song: Song = SongRef(id)
                song.loadData(context).onSuccess { data ->
                    data.saveToDatabase(context.database)

                    data.artist?.also { artist ->
                        this@withContext.launch {
                            artist.loadData(context).onSuccess { artist_data ->
                                artist_data.saveToDatabase(context.database)
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

        if (state.state != _state.ordinal) {
            _state = MediaPlayerState.values()[state.state]
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
        if (state.repeat_mode != _repeat_mode.ordinal) {
            _repeat_mode = MediaPlayerRepeatMode.values()[state.repeat_mode]
            listeners.forEach {
                it.onRepeatModeChanged(_repeat_mode)
                it.onEvents()
            }
        }

        _duration_ms = state.duration_ms.toLong()
        updateCurrentSongPosition(state.current_position_ms.toLong())

        poll_coroutine_scope.launchSingle(Dispatchers.IO) {
            while (true) {
                delay(POLL_STATE_INTERVAL)
                pollServerState()
            }
        }

        socket_load_state = PlayerServiceLoadState(false)

        listeners.forEach {
            it.onDurationChanged(_duration_ms)
            it.onEvents()
        }

        return@withContext true
    }

    private fun ZMQ.Socket.pollServerState() {
        val events: ZMsg = ZMsg.recvMsg(socket ?: return)

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

            try {
                val type = (event["type"] as String?) ?: continue
                val properties = (event["properties"] as? Map<String, Any>) ?: continue
                when (type) {
                    "ItemTransition" -> {
                        _current_song_index = (properties["index"] as Double).toInt()
                        _duration_ms = -1
                        updateCurrentSongPosition(0)
                        listeners.forEach { 
                            it.onSongTransition(getSong(_current_song_index), false)
                            it.onEvents() 
                        }
                    }
                    "PropertyChanged" -> {
                        val value: Any? = properties["value"]
                        when (properties["key"] as String) {
                            "state" -> {
                                if (value != _state.ordinal) {
                                    _state = MediaPlayerState.values()[(value as Double).toInt()]
                                    listeners.forEach { 
                                        it.onStateChanged(_state)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "is_playing" -> {
                                if (value != _is_playing) {
                                    updateIsPlaying(value as Boolean)
                                    listeners.forEach { 
                                        it.onPlayingChanged(_is_playing)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "repeat_mode" -> {
                                if (value != _repeat_mode.ordinal) {
                                    _repeat_mode = MediaPlayerRepeatMode.values()[(value as Double).toInt()]
                                    listeners.forEach { 
                                        it.onRepeatModeChanged(_repeat_mode)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "volume" -> {
                                if (value != _volume) {
                                    _volume = (value as Double).toFloat()
                                    listeners.forEach { 
                                        it.onVolumeChanged(_volume)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "duration_ms" -> {
                                val duration = (value as Double).toLong()
                                if (duration != _duration_ms) {
                                    _duration_ms = duration
                                    listeners.forEach { 
                                        it.onDurationChanged(_duration_ms)
                                        it.onEvents() 
                                    }
                                }
                            }
                            else -> throw NotImplementedError(type)
                        }
                    }
                    "Seeked" -> {
                        val position_ms = (properties["position_ms"] as Double).toLong()
                        updateCurrentSongPosition(position_ms)
                        listeners.forEach { 
                            it.onSeeked(position_ms)
                            it.onEvents() 
                        }
                    }
                    "ItemAdded" -> {
                        val song = SongData(properties["item_id"] as String)
                        val index = (properties["index"] as Double).toInt()
                        playlist.add(index, song)
                        listeners.forEach { 
                            it.onSongAdded(index, song)
                            it.onEvents() 
                        }
                        service_player.session_started = true
                    }
                    "ItemRemoved" -> {
                        val index = (properties["index"] as Double).toInt()
                        playlist.removeAt(index)
                        listeners.forEach { 
                            it.onSongRemoved(index)
                            it.onEvents() 
                        }
                    }
                    "ItemMoved" -> {
                        val to = (properties["to"] as Double).toInt()
                        val from = (properties["from"] as Double).toInt()
                        playlist.add(to, playlist.removeAt(from))
                        listeners.forEach { 
                            it.onSongMoved(from, to)
                            it.onEvents() 
                        }
                    }
                    else -> throw NotImplementedError(type)
                }
            }
            catch (e: Throwable) {
                throw RuntimeException("Processing event $event failed", e)
            }
        }

        val reply: ZMsg = ZMsg()
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
