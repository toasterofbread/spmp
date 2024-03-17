package com.toasterofbread.spmp.platform.playerservice

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.model.settings.category.YoutubeAuthSettings
import com.toasterofbread.spmp.model.settings.unpackSetData
import com.toasterofbread.spmp.platform.PlatformServiceImpl
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.model.radio.RadioState
import io.ktor.http.Headers
import io.ktor.util.flattenEntries
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.encodeToJsonElement
import org.zeromq.*
import java.net.InetAddress
import spms.socketapi.shared.*

private const val POLL_STATE_INTERVAL: Long = 100
private const val POLL_TIMEOUT_MS: Long = 10000

abstract class SpMsPlayerService: PlatformServiceImpl(), ClientServerPlayerService {
    override var connected_server: ClientServerPlayerService.ServerInfo? by mutableStateOf(null)

    private val clients_result_channel: Channel<SpMsActionReply> = Channel()

    var socket_load_state: PlayerServiceLoadState by mutableStateOf(PlayerServiceLoadState(true))
        private set
    var socket_connection_error: Throwable? by mutableStateOf(null)
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
                YoutubeAuthSettings.Key.YTM_AUTH.getName() -> {
                    sendYtmAuthToPlayers()
                }
            }
        }
    }

    private val zmq: ZContext = ZContext()
    private lateinit var socket: ZMQ.Socket
    private val json: Json = Json { ignoreUnknownKeys = true }
    private val queued_messages: MutableList<Pair<String, List<JsonElement?>>> = mutableListOf()
    private var cancel_connection: Boolean = false
    private var restart_connection: Boolean = false

    private val poll_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val connect_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val player_status_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    internal abstract val listeners: List<PlayerListener>
    internal var playlist: MutableList<Song> = mutableListOf()
        private set

    internal var _state: SpMsPlayerState = SpMsPlayerState.IDLE
    internal var _is_playing: Boolean = false
    internal var _current_song_index: Int = -1
    internal var _duration_ms: Long = -1
    internal var _radio_state: RadioState = RadioState() // TODO
    internal var _repeat_mode: SpMsPlayerRepeatMode = SpMsPlayerRepeatMode.NONE
    internal var _volume: Float = 1f
    internal var current_song_time: Long = -1

    protected fun sendRequest(action: String, vararg params: JsonElement?) {
        synchronized(queued_messages) {
            queued_messages.add(Pair(action, params.map { Json.encodeToJsonElement(it) }))
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
                socket_connection_error = null
                cancel_connection = false
                restart_connection = false

                val ip: String = getServerIp()
                val port: Int = getServerPort()
                val protocol: String = "tcp"
                val server_url = "$protocol://$ip:$port"

                val handshake: SpMsClientHandshake =
                    SpMsClientHandshake(
                        name = getClientName(),
                        type = SpMsClientType.SPMP_STANDALONE,
                        machine_id = getSpMsMachineId(),
                        language = context.getUiLanguage()
                    )

                val server_handshake: SpMsServerHandshake?
                try {
                    server_handshake = tryConnectToServer(
                        server_url = server_url,
                        handshake = handshake,
                        json = json,
                        shouldCancelConnection = { cancel_connection },
                        setLoadState = { socket_load_state = it }
                    )
                }
                catch (e: Throwable) {
                    socket_connection_error = e
                    continue
                }

                if (server_handshake == null) {
                    disconnect(server_url)
                    continue
                }

                connected_server = ClientServerPlayerService.ServerInfo(
                    ip = ip,
                    port = port,
                    protocol = protocol,
                    name = server_handshake.name,
                    device_name = server_handshake.device_name,
                    machine_id = server_handshake.machine_id,
                    spms_api_version = server_handshake.spms_api_version
                )

                var server_state_applied: Boolean = false

                poll_coroutine_scope.launchSingle {
                    val context: ZMQ.Context = ZMQ.context(1)
                    val poller: ZMQ.Poller = context.poller()
                    poller.register(socket, ZMQ.Poller.POLLIN)

                    var queued_events: MutableList<SpMsPlayerEvent>? = mutableListOf()

                    while (true) {
                        delay(POLL_STATE_INTERVAL)

                        synchronized(this@launch) {
                            if (server_state_applied && queued_events != null) {
                                applyPlayerEvents(queued_events!!)
                                queued_events = null
                            }
                        }

                        val poll_successful: Boolean =
                            pollServerState(poller, POLL_TIMEOUT_MS) { events ->
                                queued_events?.also {
                                    it.addAll(events)
                                    return@also
                                }

                                applyPlayerEvents(events)
                            }

                        if (!poll_successful) {
                            onSocketConnectionLost(POLL_TIMEOUT_MS)
                            break
                        }
                    }
                }

                applyServerState(server_handshake.server_state, this) { status ->
                    socket_load_state =
                        PlayerServiceLoadState(
                            true,
                            getString("desktop_splash_setting_initial_state") + status?.let { " ($it)" }.orEmpty()
                        )
                }

                socket_load_state = PlayerServiceLoadState(false)

                synchronized(this) {
                    server_state_applied = true
                }

                sendYtmAuthToPlayers()
            }
            while (restart_connection)
        }
    }

    private fun ZMQ.Socket.pollServerState(
        poller: ZMQ.Poller,
        timeout: Long = -1,
        onEvents: (List<SpMsPlayerEvent>) -> Unit
    ): Boolean {
        val events: ZMsg
        if (poller.poll(timeout) > 0) {
            events = ZMsg.recvMsg(this)
        }
        else {
            println("Polling server timed out after ${timeout}ms")
            return false
        }

        val decoded_events: List<SpMsPlayerEvent> =
            SpMsSocketApi.decode(events.map { it.data.decodeToString() })
            .mapNotNull { event: String ->
                try {
                    json.decodeFromString(event)
                }
                catch (e: Throwable) {
                    throw RuntimeException("Parsing event failed '$event'", e)
                }
            }

        onEvents(decoded_events)

        val reply: ZMsg = ZMsg()
        synchronized(queued_messages) {
            if (queued_messages.isEmpty()) {
                reply.add(byteArrayOf())
            }
            else {
                for (message in queued_messages) {
                    reply.addSafe(message.first)
                    reply.addSafe(Json.encodeToString(message.second))
                }
            }

            val actions_expecting_result: List<Pair<String, List<Any?>>> =
                queued_messages.filter { it.first.firstOrNull() == SPMS_EXPECT_REPLY_CHAR }

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

            val result_str: String = SpMsSocketApi.decode(results.map { it.data.decodeToString() }).first()
            if (result_str.isEmpty()) {
                throw RuntimeException("Result string is empty")
            }

            val parsed_results: List<SpMsActionReply>?
            try {
                parsed_results = json.decodeFromString(result_str)
            }
            catch (e: Throwable) {
                throw RuntimeException("Parsing result failed '$result_str'", e)
            }

            for ((i, result) in parsed_results.orEmpty().withIndex()) {
                val action: Pair<String, List<Any?>> = actions_expecting_result[i]
                when (action.first.drop(1)) {
                    "clients" -> clients_result_channel.trySend(result)
                    else -> throw NotImplementedError("Action: '$action' Result: '$result'")
                }
            }

            return true
        }
    }

    override suspend fun getPeers(): Result<List<SpMsClientInfo>> {
        sendRequest(SPMS_EXPECT_REPLY_CHAR + "clients")

        val result: SpMsActionReply = clients_result_channel.receive()
        if (!result.success) {
            return Result.failure(RuntimeException(result.error, result.error_cause?.let { RuntimeException(it) }))
        }

        if (result.result == null) {
            return Result.failure(NullPointerException("Result is null"))
        }

        return Result.success(Json.decodeFromJsonElement(result.result))
    }

    override fun onSongFilesAdded(songs: List<DownloadStatus>) {
        player_status_coroutine_scope.launch {
            runCommandOnEachLocalPlayer(
                "addLocalFiles",
                buildJsonObject {
                    for (song in songs) {
                        put(song.id, song.file?.absolute_path)
                    }
                }
            )
        }
    }

    override fun onSongFilesDeleted(songs: List<Song>) {
        player_status_coroutine_scope.launch {
            runCommandOnEachLocalPlayer(
                "removeLocalFiles",
                buildJsonArray {
                    for (song in songs) {
                        add(song.id)
                    }
                }
            )
        }
    }

    override fun onLocalSongsSynced(songs: List<DownloadStatus>) {
        player_status_coroutine_scope.launch {
            runCommandOnEachLocalPlayer(
                "setLocalFiles",
                buildJsonObject {
                    for (song in songs) {
                        put(song.id, song.file?.absolute_path)
                    }
                }
            )
        }
    }

    override suspend fun sendAuthInfoToPlayers(ytm_auth: Pair<String?, Headers>?): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            runCommandOnEachLocalPlayer(
                "setAuthInfo",
                ytm_auth?.second?.let {
                    buildJsonObject {
                        for ((key, value) in it.flattenEntries()) {
                            put(key, value)
                        }
                    }
                }
            )
        }
    }

    private suspend fun runCommandOnEachLocalPlayer(identifier: String, vararg params: JsonElement?) {
        val socket: ZMQ.Socket = zmq.createSocket(SocketType.REQ)

        val message: ZMsg = ZMsg()
        message.addSafe(SPMS_EXPECT_REPLY_CHAR + identifier)
        message.addSafe(Json.encodeToString(params))

        val local_players: List<SpMsClientInfo> = getLocalPlayers().getOrNull() ?: return

        for (player in local_players) {
            if (player.machine_id == connected_server?.machine_id && player.player_port == connected_server?.port) {
                sendRequest(identifier, *params)
                continue
            }

            val server_url: String = "tcp://127.0.0.1:${player.player_port}"

            val handshake: SpMsClientHandshake =
                SpMsClientHandshake(
                    name = getClientName(),
                    type = SpMsClientType.SPMP_STANDALONE,
                    machine_id = getSpMsMachineId(),
                    language = context.getUiLanguage()
                )

            val server_handshake: SpMsServerHandshake? =
                socket.tryConnectToServer(
                    server_url = server_url,
                    handshake = handshake,
                    json = json
                )

            if (server_handshake == null) {
                socket.disconnect(server_url)
                continue
            }

            try {
                message.send(socket)
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
            finally {
                socket.disconnect(server_url)
            }
        }

        socket.close()
    }

    private suspend fun getLocalPlayers(): Result<List<SpMsClientInfo>> =
        getPeers().fold(
            {
                val machine_id: String = getSpMsMachineId()
                Result.success(
                    it.filter { peer ->
                        !peer.is_caller && peer.player_port != null && peer.machine_id == machine_id
                    }
                )
            },
            { Result.failure(it) }
        )

    private fun sendYtmAuthToPlayers() {
        player_status_coroutine_scope.launch {
            val ytm_auth: Pair<String?, Headers>? =
                ApiAuthenticationState.unpackSetData(
                    YoutubeAuthSettings.Key.YTM_AUTH.get(context),
                    context
                ).takeIf { it.first != null }
            sendAuthInfoToPlayers(ytm_auth)
        }
    }
}

private fun ZMsg.addSafe(part: String) {
    addAll(SpMsSocketApi.encode(listOf(part)).map { ZFrame(it) })
}
