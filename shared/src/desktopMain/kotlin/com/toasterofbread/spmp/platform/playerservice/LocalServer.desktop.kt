package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.getMachineId
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.warning_server_unavailable
import spmp.shared.generated.resources.server_missing_files_splitter

private const val POLL_INTERVAL: Long = 100
private const val CLIENT_REPLY_ATTEMPTS: Int = 10

<<<<<<< HEAD
actual object LocalServer {
    private fun createServer(): SpMs =
        SpMs(
            headless = false,
            enable_gui = false,
            machine_id = getMachineId()!!
        )

    actual fun isAvailable(): Boolean =
        SpMs.isAvailable(headless = false)

    @Composable
    actual fun getLocalServerUnavailabilityReason(): String? {
        val server: SpMs =
            try {
                createServer()
            }
            catch (e: NoClassDefFoundError) {
                val split_message: List<String> = e.cause?.message?.split(" ") ?: emptyList()
                val missing_files: List<String> = split_message.filter { it.endsWith(".so") || it.endsWith(".dll") }

                return stringResource(Res.string.warning_server_unavailable) + missing_files.joinToString(stringResource(Res.string.server_missing_files_splitter))
            }
            catch (e: UnsatisfiedLinkError) {
                val message: String = e.message ?: "NO MESSAGE"
                val end_index: Int = message.indexOf("in java.library.path")
                val missing_files: List<String> = listOf(message.substring(3, end_index))
                return stringResource(Res.string.warning_server_unavailable) + missing_files.joinToString(stringResource(Res.string.server_missing_files_splitter))
            }

        server.release()
        return null
=======
fun Throwable.getMissingLibrariesInMessage(): List<String>? =
    cause?.message?.let { getLibrariesInLibraryLoadErrorMessage(it) }
    ?: message?.let { getLibrariesInLibraryLoadErrorMessage(it) }

private fun getLibrariesInLibraryLoadErrorMessage(message: String): List<String>? {
    val end_index: Int = message.indexOf("in java.library.path")
    if (end_index != -1) {
        val libraries: List<String> = listOf(message.substring(3, end_index))
        check(libraries.isNotEmpty()) { "Message: '$message" }
        return libraries
>>>>>>> main
    }

    val split: List<String> = message.split(' ')
    val libraries: List<String> = split.filter { it.endsWith(".so") || it.endsWith(".dll") }
    if (libraries.isNotEmpty()) {
        return libraries
    }

    return null
}

actual object LocalServer {
    private data class MissingLibrariesException(val libraries: List<String>): RuntimeException(
        getString("warning_server_unavailable") + libraries.joinToString(getString("server_missing_files_splitter"))
    ) {
        init {
            require(libraries.isNotEmpty())
        }
    }

    private fun createServer(): Result<SpMs> =
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
            Result.failure(MissingLibrariesException(missing_libraries))
        }

    actual fun getLocalServerUnavailabilityReason(): String? =
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
                    return@fold it.message ?: (getString("error_message_generic") + " (${it::class})")
                }
            }
        )

    actual fun startLocalServer(
        context: AppContext,
        port: Int
<<<<<<< HEAD
    ): Job {
        val server: SpMs =
            SpMs(
                headless = false,
                enable_gui = false,
                machine_id = getMachineId()!!
            )
=======
    ): Result<Job> = runCatching {
        val server: SpMs = createServer().getOrThrow()
>>>>>>> main

        server.bind(port)

        return@runCatching context.coroutine_scope.launch(Dispatchers.IO) {
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
