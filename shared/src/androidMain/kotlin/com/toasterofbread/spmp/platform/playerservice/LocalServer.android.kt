package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

actual object LocalServer {
    actual fun isAvailable(): Boolean = false

    actual suspend fun getLocalServerUnavailabilityReason(): String? = null

    actual suspend fun startLocalServer(
        context: AppContext,
        port: Int,
    ): Result<Job> = throw IllegalAccessError()
}
