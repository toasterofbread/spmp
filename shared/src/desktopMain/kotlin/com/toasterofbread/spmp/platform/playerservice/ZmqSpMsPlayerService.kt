package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.platform.PlatformServiceImpl
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
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

    private fun getServerPort(): Int = DesktopSettings.Key.SERVER_PORT.get(context)
    private fun getServerIp(): String = DesktopSettings.Key.SERVER_IP_ADDRESS.get(context)

    private fun getClientName(): String {
        val host: String = InetAddress.getLocalHost().hostName
        val os: String = System.getProperty("os.name")

        return getString("app_name") + " [$os, $host]"
    }

    private val prefs_listener: PlatformPreferencesListener = object : PlatformPreferencesListener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
                DesktopSettings.Key.SERVER_IP_ADDRESS.getName(),
                DesktopSettings.Key.SERVER_PORT.getName() -> {
                    restart_connection = true
                    cancel_connection = true
                }
            }
        }
    }

    private val zmq: ZContext = ZContext()
    private var socket: ZMQ.Socket? = null
    private val json: Json = Json { ignoreUnknownKeys = true }
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

    private suspend fun ZMQ.Socket.connectToServer(url: String): Boolean = withContext(Dispatchers.IO) {
        val client_info: SpMsClientInfo = SpMsClientInfo(getClientName(), SpMsClientType.HEADLESS)
        val handshake_message: ZMsg = ZMsg()
        handshake_message.add(json.encodeToString(client_info))
        handshake_message.send(this@connectToServer)

        socket_load_state = PlayerServiceLoadState(
            true,
            getString("desktop_splash_connecting_to_server_at_\$x").replace("\$x", url)
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

        val state: SpMsServerState
        try {
            state = json.decodeFromString(state_data)
        }
        catch (e: Throwable) {
            throw RuntimeException("Parsing handshake reply data failed '$state_data'", e)
        }

        socket_load_state = PlayerServiceLoadState(
            true,
            getString("desktop_splash_setting_initial_state")
        )

        assert(playlist.isEmpty())

        val items: Array<Song?> = arrayOfNulls(state.queue.size)

        state.queue.mapIndexed { i, id ->
            launch {
                val song: Song = SongRef(id)
                tryTransaction {
                    song.loadData(context).onSuccess { data ->
                        data.saveToDatabase(context.database)

                        data.artist?.also { artist ->
                            this@withContext.launch {
                                tryTransaction {
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

            val event: SpMsPlayerEvent
            try {
                event = json.decodeFromString(event_str) ?: continue
            }
            catch (e: Throwable) {
                throw RuntimeException("Parsing event failed '$event_str'", e)
            }

            try {
                when (event.type) {
                    SpMsPlayerEvent.Type.ITEM_TRANSITION -> {
                        _current_song_index = event.properties["index"]!!.int
                        _duration_ms = -1
                        updateCurrentSongPosition(0)
                        listeners.forEach { 
                            it.onSongTransition(getSong(_current_song_index), false)
                            it.onEvents() 
                        }
                    }
                    SpMsPlayerEvent.Type.PROPERTY_CHANGED -> {
                        val key: String = event.properties["key"]!!.content
                        val value: JsonPrimitive = event.properties["value"]!!
                        when (key) {
                            "state" -> {
                                if (value.int != _state.ordinal) {
                                    _state = MediaPlayerState.values()[value.int]
                                    listeners.forEach { 
                                        it.onStateChanged(_state)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "is_playing" -> {
                                if (value.boolean != _is_playing) {
                                    updateIsPlaying(value.boolean)
                                    listeners.forEach { 
                                        it.onPlayingChanged(_is_playing)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "repeat_mode" -> {
                                if (value.int != _repeat_mode.ordinal) {
                                    _repeat_mode = MediaPlayerRepeatMode.values()[value.int]
                                    listeners.forEach { 
                                        it.onRepeatModeChanged(_repeat_mode)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "volume" -> {
                                if (value.float != _volume) {
                                    _volume = value.float
                                    listeners.forEach { 
                                        it.onVolumeChanged(_volume)
                                        it.onEvents() 
                                    }
                                }
                            }
                            "duration_ms" -> {
                                if (value.long != _duration_ms) {
                                    _duration_ms = value.long
                                    listeners.forEach { 
                                        it.onDurationChanged(_duration_ms)
                                        it.onEvents() 
                                    }
                                }
                            }
                            else -> throw NotImplementedError(key)
                        }
                    }
                    SpMsPlayerEvent.Type.SEEKED -> {
                        val position_ms: Long = event.properties["position_ms"]!!.long
                        updateCurrentSongPosition(position_ms)
                        listeners.forEach { 
                            it.onSeeked(position_ms)
                            it.onEvents() 
                        }
                    }
                    SpMsPlayerEvent.Type.ITEM_ADDED -> {
                        val song: SongData = SongData(event.properties["item_id"]!!.content)
                        val index: Int = event.properties["index"]!!.int
                        playlist.add(index, song)
                        listeners.forEach { 
                            it.onSongAdded(index, song)
                            it.onEvents() 
                        }
                        service_player.session_started = true
                    }
                    SpMsPlayerEvent.Type.ITEM_REMOVED -> {
                        val index: Int = event.properties["index"]!!.int
                        playlist.removeAt(index)
                        listeners.forEach { 
                            it.onSongRemoved(index)
                            it.onEvents() 
                        }
                    }
                    SpMsPlayerEvent.Type.ITEM_MOVED -> {
                        val to: Int = event.properties["to"]!!.int
                        val from: Int = event.properties["from"]!!.int
                        playlist.add(to, playlist.removeAt(from))
                        listeners.forEach { 
                            it.onSongMoved(from, to)
                            it.onEvents() 
                        }
                    }
                    else -> throw NotImplementedError(event.toString())
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
