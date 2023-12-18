package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.platform.PlatformServiceImpl
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMsg
import java.net.InetAddress

private const val POLL_STATE_INTERVAL: Long = 100
private const val POLL_TIMEOUT_MS: Long = 10000

abstract class SpMsPlayerService: PlatformServiceImpl(), ClientServerPlayerService {
    override var connected_server: ClientServerPlayerService.ServerInfo? by mutableStateOf(null)
    
    private val clients_result_channel: Channel<SpMsActionReply> = Channel()
    override suspend fun getPeers(): Result<List<SpMsClientInfo>> {
        sendRequest(SERVER_EXPECT_REPLY_CHAR + "clients")
        
        val result: SpMsActionReply = clients_result_channel.receive()
        if (!result.success) {
            return Result.failure(RuntimeException(result.error, result.error_cause?.let { RuntimeException(it) }))
        }
        
        if (result.result == null) {
            return Result.failure(NullPointerException("Result is null"))
        }
        
        return Result.success(Json.decodeFromJsonElement(result.result))
    }
    
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
    private lateinit var socket: ZMQ.Socket
    private val json: Json = Json { ignoreUnknownKeys = true }
    private val queued_messages: MutableList<Pair<String, List<Any>>> = mutableListOf()
    private val poll_coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val connect_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var cancel_connection: Boolean = false
    private var restart_connection: Boolean = false

    internal abstract val listeners: List<PlayerListener>
    internal var playlist: MutableList<Song> = mutableListOf()
        private set
    
    internal var _state: MediaPlayerState = MediaPlayerState.IDLE
    internal var _is_playing: Boolean = false
    internal var _current_song_index: Int = -1
    internal var _duration_ms: Long = -1
    internal var _radio_state: RadioInstance.RadioState = RadioInstance.RadioState()
    internal var _repeat_mode: MediaPlayerRepeatMode = MediaPlayerRepeatMode.NONE
    internal var _volume: Float = 1f
    internal var current_song_time: Long = -1

    protected fun sendRequest(action: String, vararg params: Any) {
        synchronized(queued_messages) {
            queued_messages.add(Pair(action, params.asList()))
        }
    }

    internal fun updateIsPlaying(playing: Boolean) {
        if (playing == _is_playing) {
            return
        }

        val position_ms = current_position_ms
        _is_playing = playing
        updateCurrentSongPosition(position_ms)
    }

    internal fun updateCurrentSongPosition(position_ms: Long) {
        require(position_ms >= 0) { position_ms }
        if (_is_playing) {
            current_song_time = System.currentTimeMillis() - position_ms
        }
        else {
            current_song_time = position_ms
        }
    }

    override fun onCreate() {
        context.getPrefs().addListener(prefs_listener)

        socket = zmq.createSocket(SocketType.DEALER)
        socket.connectToServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        poll_coroutine_scope.cancel()
        connect_coroutine_scope.cancel()
        context.getPrefs().removeListener(prefs_listener)
    }

    private fun onSocketConnectionLost(expired_timeout_ms: Long) {
        println("Connection to server timed out after ${expired_timeout_ms}ms, reconnecting...")
        socket.connectToServer()
    }

    private fun ZMQ.Socket.connectToServer() {
        connect_coroutine_scope.launch {
            do {
                connected_server = null
                cancel_connection = false
                restart_connection = false
                
                val ip: String = getServerIp()
                val port: Int = getServerPort()
                val protocol: String = "tcp"
                val server_url = "$protocol://$ip:$port"

                val handshake: SpMsClientHandshake = SpMsClientHandshake(getClientName(), SpMsClientType.SPMP_STANDALONE, context.getUiLanguage())

                val server_handshake: SpMsServerHandshake? = tryConnectToServer(
                    socket = this@connectToServer,
                    server_url = server_url,
                    handshake = handshake,
                    json = json,
                    shouldCancelConnection = { cancel_connection },
                    setLoadState = { socket_load_state = it }
                )

                if (server_handshake == null) {
                    disconnect(server_url)
                    continue
                }

                connected_server = ClientServerPlayerService.ServerInfo(
                    ip,
                    port,
                    protocol,
                    server_handshake.name,
                    server_handshake.device_name,
                    server_handshake.spms_commit_hash
                )

                var server_state_applied: Boolean = false

                poll_coroutine_scope.launchSingle(Dispatchers.IO) {
                    val context: ZMQ.Context = ZMQ.context(1)
                    val poller: ZMQ.Poller = context.poller()
                    poller.register(socket, ZMQ.Poller.POLLIN)

                    var queued_events: MutableList<SpMsPlayerEvent>? = mutableListOf()

                    while (true) {
                        delay(POLL_STATE_INTERVAL)

                        synchronized(this@launch) {
                            if (server_state_applied && queued_events != null) {
                                for (event in queued_events!!) {
                                    applyPlayerEvent(event)
                                }
                                queued_events = null
                            }
                        }

                        val poll_successful: Boolean =
                            pollServerState(poller, POLL_TIMEOUT_MS) { event ->
                                queued_events?.also {
                                    it.add(event)
                                    return@also
                                }

                                applyPlayerEvent(event)
                            }

                        if (!poll_successful) {
                            onSocketConnectionLost(POLL_TIMEOUT_MS)
                            break
                        }
                    }
                }

                socket_load_state =
                    PlayerServiceLoadState(
                        true,
                        getString("desktop_splash_setting_initial_state")
                    )

                applyServerState(server_handshake.server_state, this)

                socket_load_state = PlayerServiceLoadState(false)

                synchronized(this) {
                    server_state_applied = true
                }
            }
            while (restart_connection)
        }
    }

    private fun ZMQ.Socket.pollServerState(poller: ZMQ.Poller, timeout: Long = -1, onEvent: (SpMsPlayerEvent) -> Unit): Boolean {
        val events: ZMsg
        if (poller.poll(timeout) > 0) {
            events = ZMsg.recvMsg(this)
        }
        else {
            println("Polling server timed out after ${timeout}ms")
            return false
        }

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
                onEvent(event)
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

            val actions_expecting_result: List<Pair<String, List<Any>>> =
                queued_messages.filter { it.first.firstOrNull() == SERVER_EXPECT_REPLY_CHAR }
            
            queued_messages.clear()
            
            val reply_result: Boolean = reply.send(this@pollServerState)
            if (!reply_result || actions_expecting_result.isEmpty()) {
                return reply_result
            }
            
            val results: ZMsg
            if (poller.poll(timeout) > 0) {
                results = ZMsg.recvMsg(this)
            }
            else {
                println("Getting results timed out after ${timeout}ms")
                return false
            }
            
            val result_str: String = results.joinToString { it.data.decodeToString().removeSuffix("\u0000") }
            if (result_str.isEmpty()) {
                throw NullPointerException("Result string is empty")
            }

            val parsed_results: List<SpMsActionReply>
            try {
                parsed_results = json.decodeFromString(result_str)!!
            }
            catch (e: Throwable) {
                throw RuntimeException("Parsing result failed '$result_str'", e)
            }
            
            for ((i, result) in parsed_results.withIndex()) {
                val action: Pair<String, List<Any>> = actions_expecting_result[i]
                when (action.first.drop(1)) {
                    "clients" -> clients_result_channel.trySend(result)
                    else -> throw NotImplementedError("Action: '$action' Result: '$result'")
                }
            }
            
            return true
        }
    }
}
