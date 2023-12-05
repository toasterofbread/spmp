package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.zeromq.ZMQ.Socket
import org.zeromq.ZMsg

internal suspend fun ZmqSpMsPlayerService.tryConnectToServer(
    socket: Socket,
    server_url: String,
    handshake: SpMsClientHandshake,
    json: Json,
    shouldCancelConnection: () -> Boolean,
    setLoadState: (PlayerServiceLoadState) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    check(socket.connect(server_url))

    val handshake_message: ZMsg = ZMsg()
    handshake_message.add(json.encodeToString(handshake))
    handshake_message.send(socket)

    setLoadState(
        PlayerServiceLoadState(
            true,
            getString("desktop_splash_connecting_to_server_at_\$x").replace("\$x", server_url)
        )
    )

    println("Waiting for reply from server at $server_url...")

    var reply: ZMsg? = null
    while (reply == null) {
        reply = socket.recvMsg(500)

        if (shouldCancelConnection()) {
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

    setLoadState(
        PlayerServiceLoadState(
            true,
            getString("desktop_splash_setting_initial_state")
        )
    )

    applyServerState(state, this@withContext)

    setLoadState(PlayerServiceLoadState(false))

    return@withContext true
}

private fun Socket.recvMsg(timeout_ms: Long?): ZMsg? {
    receiveTimeOut = timeout_ms?.toInt() ?: -1
    val msg: ZMsg? = ZMsg.recvMsg(this)
    receiveTimeOut = -1
    return msg
}
