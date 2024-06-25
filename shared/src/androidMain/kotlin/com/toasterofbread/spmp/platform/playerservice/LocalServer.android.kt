package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

actual object LocalServer {
    actual fun getLocalServerUnavailabilityReason(): String? = null

    actual fun startLocalServer(
        context: AppContext,
        port: Int,
    ): Job = throw IllegalAccessError()
}
