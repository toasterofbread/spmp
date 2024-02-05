package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.utils.common.associateNotNull
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.model.settings.category.YoutubeAuthSettings
import com.toasterofbread.spmp.platform.PlatformServiceImpl
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
                YoutubeAuthSettings.Key.YTM_AUTH.getName() -> {
                    sendYtmAuthToPlayers()
                }
            }
        }
    }

    private val zmq: ZContext = ZContext()
    private lateinit var socket: ZMQ.Socket
    private val http_client: OkHttpClient = OkHttpClient()
    private val json: Json = Json { ignoreUnknownKeys = true }
    private val queued_messages: MutableList<Pair<String, List<Any>>> = mutableListOf()
    private var cancel_connection: Boolean = false
    private var restart_connection: Boolean = false

    private val poll_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val connect_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val player_status_coroutine_scope: CoroutineScope = CoroutineScope(Job())

    internal abstract val listeners: List<PlayerListener>
    internal var playlist: MutableList<Song> = mutableListOf()
        private set
    
    internal var _state: MediaPlayerState = MediaPlayerState.IDLE
    internal var _is_playing: Boolean = false
    internal var _current_song_index: Int = -1
    internal var _duration_ms: Long = -1
    internal var _radio_state: RadioInstance.RadioState = RadioInstance.RadioState() // TODO
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

                val handshake: SpMsClientHandshake =
                    SpMsClientHandshake(
                        name = getClientName(),
                        type = SpMsClientType.SPMP_STANDALONE,
                        machine_id = getSpMsMachineId(),
                        language = context.getUiLanguage()
                    )

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

                poll_coroutine_scope.launchSingle {
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

                sendYtmAuthToPlayers()
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

    private fun sendYtmAuthToPlayers() {
        player_status_coroutine_scope.launch {
            val ytm_auth: Pair<Artist?, Headers>? =
                YoutubeApi.UserAuthState.unpackSetData(YoutubeAuthSettings.Key.YTM_AUTH.get(context), context).takeIf { it.first != null }
            sendStatusToPlayers(ytm_auth, emptyMap())
        }
    }

    override fun onSongFileAdded(download_status: DownloadStatus) {
        // TODO
        return

        player_status_coroutine_scope.launch {
            val local_players: List<SpMsClientInfo> = getLocalPlayers().getOrNull() ?: return@launch
            for (player in local_players) {
                val local_files_request: Request = Request.Builder()
                    .url("http://127.0.0.1:${player.player_port}/local_song/add/${download_status.song.id}")
                    .post(download_status.file!!.absolute_path.toRequestBody("text/plain".toMediaType()))
                    .build()
                http_client.newCall(local_files_request).execute()
            }
        }
    }

    override fun onSongFileDeleted(song: Song) {
        // TODO
        return

        player_status_coroutine_scope.launch {
            val local_players: List<SpMsClientInfo> = getLocalPlayers().getOrNull() ?: return@launch
            for (player in local_players) {
                val local_files_request: Request = Request.Builder()
                    .url("http://127.0.0.1:${player.player_port}/local_song/remove/${song.id}")
                    .build()
                http_client.newCall(local_files_request).execute()
            }
        }
    }

    override fun onLocalSongsSynced(songs: Map<String, DownloadStatus>) {
        player_status_coroutine_scope.launch {
            val ytm_auth: Pair<Artist?, Headers>? =
                YoutubeApi.UserAuthState.unpackSetData(YoutubeAuthSettings.Key.YTM_AUTH.get(context), context).takeIf { it.first != null }
            sendStatusToPlayers(ytm_auth, songs.mapValues { it.value.file!!.absolute_path })
        }
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

    override suspend fun sendStatusToPlayers(ytm_auth: Pair<Artist?, Headers>?, local_files: Map<String, String>): Result<Unit> = withContext(Dispatchers.IO) {
        // TODO
        return@withContext Result.success(Unit)

        val local_players: List<SpMsClientInfo> = getLocalPlayers().fold(
            { it },
            { return@withContext Result.failure(it) }
        )

        val yt_account_data: RequestBody? =
            if (ytm_auth == null) null
            else Json.encodeToString(YoutubeApi.UserAuthState.packSetData(ytm_auth.first, ytm_auth.second)).toRequestBody("application/json".toMediaType())
        val local_files_data: RequestBody =
            Json.encodeToString(local_files).toRequestBody("application/json".toMediaType())

        coroutineScope {
            for (player in local_players) {
                launch {
                    try {
                        val account_request: Request = Request.Builder()
                            .apply {
                                if (yt_account_data == null) {
                                    url("http://127.0.0.1:${player.player_port}/yt_account/remove")
                                }
                                else {
                                    url("http://127.0.0.1:${player.player_port}/yt_account/set")
                                    post(yt_account_data)
                                }
                            }
                            .build()
                        http_client.newCall(account_request).execute()

                        val local_files_request: Request = Request.Builder()
                            .url("http://127.0.0.1:${player.player_port}/local_song/add_batch")
                            .post(local_files_data)
                            .build()
                        http_client.newCall(local_files_request).execute()
                    }
                    catch (e: Throwable) {
                        RuntimeException(e).printStackTrace()
                    }
                }
            }
        }

        return@withContext Result.success(Unit)
    }
}
