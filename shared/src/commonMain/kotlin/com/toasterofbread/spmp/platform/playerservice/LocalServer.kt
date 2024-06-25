package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

expect object LocalServer {
    fun getLocalServerUnavailabilityReason(): String?

    fun startLocalServer(
        context: AppContext,
        port: Int
    ): Job
}
