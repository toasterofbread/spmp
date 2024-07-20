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

actual object LocalServer {
    private fun createServer(): SpMs =
        SpMs(
            headless = false,
            enable_gui = false,
            machine_id = getMachineId()!!
        )

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

        server.release()
        return null
    }

    actual fun startLocalServer(
        context: AppContext,
        port: Int
    ): Job {
        val server: SpMs =
            SpMs(
                headless = false,
                enable_gui = false,
                machine_id = getMachineId()!!
            )

        server.bind(port)

        return context.coroutine_scope.launch(Dispatchers.IO) {
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
