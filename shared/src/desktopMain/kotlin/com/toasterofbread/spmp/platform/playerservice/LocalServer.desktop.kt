package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import dev.toastbits.spms.server.SpMs

private const val POLL_INTERVAL: Long = 100
private const val CLIENT_REPLY_ATTEMPTS: Int = 10

actual object LocalServer {
    private fun createServer(): SpMs = SpMs(headless = false, enable_gui = false)

    actual fun getLocalServerUnavailabilityReason(): String? {
        val server: SpMs =
            try {
                createServer()
            }
            catch (e: NoClassDefFoundError) {
                val split_message: List<String> = e.cause?.message?.split(" ") ?: emptyList()
                val missing_files: List<String> = split_message.filter { it.endsWith(".so") || it.endsWith(".dll") }

                return getString("warning_server_unavailable") + missing_files.joinToString(getString("server_missing_files_splitter"))
            }
            catch (e: UnsatisfiedLinkError) {
                val message: String = e.message ?: "NO MESSAGE"
                val end_index: Int = message.indexOf("in java.library.path")
                val missing_files: List<String> = listOf(message.substring(3, end_index))
                return getString("warning_server_unavailable") + missing_files.joinToString(getString("server_missing_files_splitter"))
            }

        server.release()
        return null
    }

    actual fun startLocalServer(
        context: AppContext,
        port: Int
    ): Job {
        val server: SpMs = SpMs(headless = false, enable_gui = false)

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
