package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

actual object LocalServer {
    actual fun isAvailable(): Boolean = false

    @Composable
    actual fun getLocalServerUnavailabilityReason(): String? = null

    actual fun startLocalServer(
        context: AppContext,
        port: Int
    ): Job {
        throw IllegalStateException()
    }
}
