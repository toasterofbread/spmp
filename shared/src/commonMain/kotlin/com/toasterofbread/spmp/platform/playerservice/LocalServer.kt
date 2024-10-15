package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

expect object LocalServer {
    fun isAvailable(): Boolean

    suspend fun getLocalServerUnavailabilityReason(): String?

    suspend fun startLocalServer(
        context: AppContext,
        port: Int
    ): Result<Job>
}
