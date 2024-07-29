package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

actual object LocalServer {
    actual fun getLocalServerUnavailabilityReason(): String? = null

    actual fun startLocalServer(
        context: AppContext,
        port: Int,
    ): Result<Job> = throw IllegalAccessError()
}
