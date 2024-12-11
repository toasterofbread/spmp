package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.spms.server.SpMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.error_message_generic
import spmp.shared.generated.resources.server_missing_files_splitter
import spmp.shared.generated.resources.warning_server_unavailable

private const val POLL_INTERVAL: Long = 100
private const val CLIENT_REPLY_ATTEMPTS: Int = 10

fun Throwable.getMissingLibrariesInMessage(): List<String>? =
    cause?.message?.let { getLibrariesInLibraryLoadErrorMessage(it) }
    ?: message?.let { getLibrariesInLibraryLoadErrorMessage(it) }

private fun getLibrariesInLibraryLoadErrorMessage(message: String): List<String>? {
    val end_index: Int = message.indexOf("in java.library.path")
    if (end_index != -1) {
        val libraries: List<String> = listOf(message.substring(3, end_index))
        check(libraries.isNotEmpty()) { "Message: '$message" }
        return libraries
    }

    val split: List<String> = message.split(' ')
    val libraries: List<String> = split.filter { it.endsWith(".so") || it.endsWith(".dll") }
    if (libraries.isNotEmpty()) {
        return libraries
    }

    return null
}

actual object LocalServer {
    private class MissingLibrariesException private constructor(message: String): RuntimeException(message) {
        companion object {
            suspend fun create(libraries: List<String>): MissingLibrariesException {
                require(libraries.isNotEmpty())
                return MissingLibrariesException(
                    getString(Res.string.warning_server_unavailable) + libraries.joinToString(getString(Res.string.server_missing_files_splitter))
                )
            }
        }
    }

    private suspend fun createServer(): Result<SpMs> =
        try {
            Result.success(SpMs(headless = false, enable_gui = false))
        }
        catch (e: Throwable) {
            val missing_libraries: List<String> =
                e.getMissingLibrariesInMessage()
                ?: emptyList()

            if (missing_libraries.isEmpty()) {
                throw e
            }

            check(missing_libraries.isNotEmpty()) { "Cause: '${e.cause?.message}'\nMessage: ${e.message}" }
            Result.failure(MissingLibrariesException.create(missing_libraries))
        }

    actual fun isAvailable(): Boolean =
        SpMs.isAvailable(headless = false)

    actual suspend fun getLocalServerUnavailabilityReason(): String? =
        createServer().fold(
            onSuccess = {
                it.release()
                return@fold null
            },
            onFailure = {
                if (it is MissingLibrariesException) {
                    return@fold it.message
                }
                else {
                    return@fold it.message ?: (getString(Res.string.error_message_generic) + " (${it::class})")
                }
            }
        )

    actual suspend fun startLocalServer(
        context: AppContext,
        port: Int
    ): Result<Job> = runCatching {
        val server: SpMs = createServer().getOrThrow()

        server.bind(port)

        return@runCatching context.coroutineScope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    server.poll(CLIENT_REPLY_ATTEMPTS)
                    delay(POLL_INTERVAL)
                }
            }
            finally {
                server.release()
            }
        }
    }
}
