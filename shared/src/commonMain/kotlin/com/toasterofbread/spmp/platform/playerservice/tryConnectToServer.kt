package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.zeromq.ZMQ.Socket
import org.zeromq.ZMsg

internal suspend fun Socket.tryConnectToServer(
    server_url: String,
    handshake: SpMsClientHandshake,
    json: Json,
    shouldCancelConnection: () -> Boolean = { false },
    log: (String) -> Unit = { println(it) },
    setLoadState: ((PlayerServiceLoadState) -> Unit)? = null
): SpMsServerHandshake? = withContext(Dispatchers.IO) {
    check(connect(server_url))

    val handshake_message: ZMsg = ZMsg()
    handshake_message.add(json.encodeToString(handshake))
    check(handshake_message.send(this@tryConnectToServer))

    if (setLoadState != null) {
        setLoadState(
            PlayerServiceLoadState(
                true,
                getString("desktop_splash_connecting_to_server_at_\$x").replace("\$x", server_url.split("://", limit = 2).last())
            )
        )
    }

    log("Waiting for reply from server at $server_url...")

    var reply: ZMsg? = null
    while (reply == null) {
        reply = recvMsg(500)

        if (shouldCancelConnection()) {
            return@withContext null
        }
    }

    val server_handshake_data: String = reply.first.data.decodeToString().trimEnd { it == '\u0000' }

    log("Received reply handshake from server with the following content:\n$server_handshake_data")

    val server_handshake: SpMsServerHandshake
    try {
        server_handshake = json.decodeFromString(server_handshake_data)
    }
    catch (e: Throwable) {
        throw RuntimeException("Parsing reply handshake failed '$server_handshake_data'", e)
    }

    return@withContext server_handshake
}

private fun Socket.recvMsg(timeout_ms: Long?): ZMsg? {
    receiveTimeOut = timeout_ms?.toInt() ?: -1
    val msg: ZMsg? = ZMsg.recvMsg(this)
    receiveTimeOut = -1
    return msg
}
