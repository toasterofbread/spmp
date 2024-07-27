package com.toasterofbread.spmp.platform.playerservice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dev.toastbits.spms.socketapi.shared.SpMsClientHandshake
import dev.toastbits.spms.socketapi.shared.SpMsServerHandshake
import dev.toastbits.spms.zmq.ZmqSocket
import kotlin.time.Duration
import PlatformIO
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.`loading_splash_connecting_to_server_at_$x`

internal suspend fun ZmqSocket.tryConnectToServer(
    server_url: String,
    handshake: SpMsClientHandshake,
    json: Json,
    log: (String) -> Unit = { println(it) },
    setLoadState: ((PlayerServiceLoadState) -> Unit)? = null
): SpMsServerHandshake = withContext(Dispatchers.PlatformIO) {
    val first_loading_message: String = getString(Res.string.`loading_splash_connecting_to_server_at_$x`).replace("\$x", server_url.split("://", limit = 2).last())
    setLoadState?.invoke(PlayerServiceLoadState(true, first_loading_message))

    log("Connecting to server at $server_url...")

    while (true) {
        try {
            connect(server_url)
        }
        catch (e: Throwable) {
            delay(1000)
            continue
        }

        break
    }

    val handshake_message: MutableList<String> = mutableListOf()
    handshake_message.add(json.encodeToString(handshake))

    log("Sending handshake message to server at $server_url...")
    sendStringMultipart(handshake_message)

    log("Waiting for reply from server at $server_url...")

    val reply: List<String> =
        recvStringMultipart(with (Duration) { 500.milliseconds })
        ?: throw NullPointerException("No reply from server")

    val server_handshake_data: String = reply.first()

    log("Received reply handshake from server with the following content:\n$reply")

    val server_handshake: SpMsServerHandshake
    try {
        server_handshake = json.decodeFromString(server_handshake_data)
    }
    catch (e: Throwable) {
        throw RuntimeException("Parsing reply handshake failed. You might be using an outdated server verison. $server_handshake_data", e)
    }

    return@withContext server_handshake
}
