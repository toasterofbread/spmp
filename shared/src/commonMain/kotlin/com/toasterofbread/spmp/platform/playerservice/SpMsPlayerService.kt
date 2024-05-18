package com.toasterofbread.spmp.platform.playerservice

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.model.mediaitem.song.Song
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
import kotlinx.coroutines.withTimeout
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
import kotlin.time.*

private val POLL_STATE_INTERVAL: Duration = with (Duration) { 100.milliseconds }
private val POLL_TIMEOUT: Duration = with (Duration) { 3.seconds }
private val POLL_ATTEMPTS: Int = 5

abstract class SpMsPlayerService(val plays_audio: Boolean): PlatformServiceImpl(), ClientServerPlayerService {
    abstract fun getIpAddress(): String
    abstract fun getPort(): Int

    override var connected_server: ClientServerPlayerService.ServerInfo? by mutableStateOf(null)

    private val clients_result_channel: Channel<SpMsActionReply> = Channel()

    var socket_load_state: PlayerServiceLoadState by mutableStateOf(PlayerServiceLoadState(true))
        private set

    internal abstract fun onRadioCancelRequested()

    private fun getClientName(): String {
        val os: String = Platform.getOSName()
        var host: String = Platform.getHostName()
        return getString("app_name") + " [$os, $host]"
    }

    private val prefs_listener: PlatformPreferencesListener =
        PlatformPreferencesListener { _, key ->
            when (key) {
                context.settings.youtube_auth.YTM_AUTH.key -> {
                    sendYtmAuthToPlayers()
                }
            }
        }

    private val zmq: ZContext = ZContext().apply { setLinger(0) }
    private var socket: ZMQ.Socket? = null
    private val json: Json = Json { ignoreUnknownKeys = true }
    private val queued_messages: MutableList<Pair<String, List<JsonElement?>>> = mutableListOf()

    private val poll_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val connect_coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val player_status_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    internal val listeners: MutableList<PlayerListener> = mutableListOf()
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
        println("sendRequest $action ${params.toList()} ${connected_server == null}")

        if (connected_server == null) {
            return
        }

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
        connectToServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
        socket?.close()
        context.getPrefs().removeListener(prefs_listener)
    }

    private fun onSocketConnectionLost(attempts: Int, expired_timeout: Duration) {
        println("Connection to server timed out after $attempts poll attempts, reconnecting...")

        connected_server?.run {
            connected_server = null
            connectToServer()
        }
    }

    private fun connectToServer() {
        check(connected_server == null)
        connect_coroutine_scope.launchSingle {
            while (true) {
                val socket: ZMQ.Socket = zmq.createSocket(SocketType.DEALER)
                this@SpMsPlayerService.socket = socket

                try {
                    socket.connectSocketToServer(with (Duration) { 5.seconds })
                }
                catch (e: Throwable) {
                    e.printStackTrace()
                    socket.close()
                    continue
                }

                break
            }
        }
    }

    fun disconnectFromServer() {
        connect_coroutine_scope.coroutineContext.cancelChildren()
        poll_coroutine_scope.coroutineContext.cancelChildren()
        player_status_coroutine_scope.coroutineContext.cancelChildren()
        connected_server = null
    }

    private suspend fun ZMQ.Socket.connectSocketToServer(timeout: Duration) = withContext(Dispatchers.Default) {
        connected_server = null

        val ip: String = getIpAddress()
        val port: Int = getPort()
        val protocol: String = "tcp"
        val server_url = "$protocol://$ip:$port"

        val handshake: SpMsClientHandshake =
            SpMsClientHandshake(
                name = getClientName(),
                type = if (plays_audio) SpMsClientType.SPMP_PLAYER else SpMsClientType.SPMP_STANDALONE,
                machine_id = getSpMsMachineId(context),
                language = context.getUiLanguage()
            )

        val server_handshake: SpMsServerHandshake =
            withTimeout(timeout) {
                tryConnectToServer(
                    server_url = server_url,
                    handshake = handshake,
                    json = json,
                    setLoadState = { socket_load_state = it }
                )
            }

        connected_server =
            ClientServerPlayerService.ServerInfo(
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
            poller.register(this@connectSocketToServer, ZMQ.Poller.POLLIN)

            var queued_events: MutableList<SpMsPlayerEvent>? = mutableListOf()

            while (true) {
                if (server_state_applied && queued_events != null) {
                    applyPlayerEvents(queued_events)
                    queued_events = null
                }

                var attempts: Int = POLL_ATTEMPTS
                var poll_successful: Boolean = false

                while (attempts-- > 0) {
                    if (
                        pollServerState(poller, POLL_TIMEOUT) { events ->
                            queued_events?.also {
                                it.addAll(events)
                                return@also
                            }

                            applyPlayerEvents(events)
                        }
                    ) {
                        poll_successful = true
                        break
                    }
                }

                if (!poll_successful) {
                    onSocketConnectionLost(POLL_ATTEMPTS, POLL_TIMEOUT)
                    break
                }

                delay(POLL_STATE_INTERVAL)
            }
        }

        applyServerState(server_handshake.server_state, this) { status ->
            socket_load_state =
                PlayerServiceLoadState(
                    true,
                    getString("loading_splash_setting_initial_state") + status?.let { " ($it)" }.orEmpty()
                )
        }

        socket_load_state = PlayerServiceLoadState(false)

        synchronized(this@withContext) {
            server_state_applied = true
        }

        sendYtmAuthToPlayers()
    }

    private suspend fun ZMQ.Socket.pollServerState(
        poller: ZMQ.Poller,
        timeout: Duration? = null,
        onEvents: suspend (List<SpMsPlayerEvent>) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var events: ZMsg? = null
        try {
            val wait_start: TimeMark = TimeSource.Monotonic.markNow()
            while (timeout == null || wait_start.elapsedNow() < timeout) {
                if (poller.poll(timeout?.let { (it - wait_start.elapsedNow()).inWholeMilliseconds } ?: -1) > 0) {
                    events = ZMsg.recvMsg(this@pollServerState)
                    break
                }
            }
        }
        catch (_: Throwable) {
            println("Polling server failed prematurely")
            return@withContext false
        }

        if (events == null) {
            println("Polling server timed out after $timeout")
            return@withContext false
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
                return@withContext reply_result
            }

            var results: ZMsg? = null
            val wait_start: TimeMark = TimeSource.Monotonic.markNow()

            while (timeout == null || wait_start.elapsedNow() < timeout) {
                if (poller.poll(timeout?.let { (it - wait_start.elapsedNow()).inWholeMilliseconds } ?: -1) > 0) {
                    results = ZMsg.recvMsg(this@pollServerState)
                    break
                }
            }

            if (results == null) {
                println("Getting results timed out after $timeout")
                return@withContext false
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

            return@withContext true
        }
    }

    override suspend fun getPeers(): Result<List<SpMsClientInfo>> = runCatching {
        sendRequest(SPMS_EXPECT_REPLY_CHAR + "clients")

        val result: SpMsActionReply =
            withTimeout(1000) {
                clients_result_channel.receive()
            }

        if (!result.success) {
            throw RuntimeException(result.error, result.error_cause?.let { RuntimeException(it) })
        }

        if (result.result == null) {
            throw NullPointerException("Result is null")
        }

        return Json.decodeFromJsonElement(result.result)
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
                    for (download in songs) {
                        put(download.song.id, download.file?.absolute_path)
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
                    machine_id = getSpMsMachineId(context),
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
                val machine_id: String = getSpMsMachineId(context)
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
                    context.settings.youtube_auth.YTM_AUTH.get(),
                    context
                ).takeIf { it.first != null }
            sendAuthInfoToPlayers(ytm_auth)
        }
    }

    override fun addListener(listener: PlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
    }
}

private fun ZMsg.addSafe(part: String) {
    addAll(SpMsSocketApi.encode(listOf(part)).map { ZFrame(it) })
}
