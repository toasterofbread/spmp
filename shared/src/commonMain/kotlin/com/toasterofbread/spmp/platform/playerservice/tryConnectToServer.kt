package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.zeromq.ZMQ.Socket
import org.zeromq.ZMsg
import spms.socketapi.shared.SpMsClientHandshake
import spms.socketapi.shared.SpMsSocketApi
import spms.socketapi.shared.SpMsServerHandshake
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

internal suspend fun Socket.tryConnectToServer(
    server_url: String,
    handshake: SpMsClientHandshake,
    json: Json,
    log: (String) -> Unit = { println(it) },
    setLoadState: ((PlayerServiceLoadState) -> Unit)? = null
): SpMsServerHandshake = withContext(Dispatchers.IO) {
    val first_loading_message: String = getString("loading_splash_connecting_to_server_at_\$x").replace("\$x", server_url.split("://", limit = 2).last())
    setLoadState?.invoke(PlayerServiceLoadState(true, first_loading_message))

    log("Connecting to server at $server_url...")

    while (true) {
        try {
            connect(server_url)
        }
        catch (_: Throwable) {
            delay(100)
            continue
        }

        break
    }

    val handshake_message: ZMsg = ZMsg()
    handshake_message.add(json.encodeToString(handshake))

    log("Sending handshake message to server at $server_url...")
    check(handshake_message.send(this@tryConnectToServer))

    log("Waiting for reply from server at $server_url...")

    var reply: ZMsg?
    do {
        reply = recvMsg(with (Duration) { 500.milliseconds })
        ensureActive()
    }
    while (reply == null)

    val joined_reply: List<String> = SpMsSocketApi.decode(reply.map { it.data.decodeToString() })
    val server_handshake_data: String = joined_reply.first()

    log("Received reply handshake from server with the following content:\n$joined_reply")

    val server_handshake: SpMsServerHandshake
    try {
        server_handshake = json.decodeFromString(server_handshake_data)
    }
    catch (e: Throwable) {
        throw RuntimeException("Parsing reply handshake failed. You might be using an outdated server verison. $server_handshake_data", e)
    }

    return@withContext server_handshake
}

private fun Socket.recvMsg(timeout: Duration?): ZMsg? {
    receiveTimeOut = timeout?.inWholeMilliseconds?.toInt() ?: -1
    val msg: ZMsg? = ZMsg.recvMsg(this)
    receiveTimeOut = -1
    return msg
}
