package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

expect object LocalServer {
    fun isAvailable(): Boolean

    @Composable
    fun getLocalServerUnavailabilityReason(): String?

    fun startLocalServer(
        context: AppContext,
        port: Int
    ): Result<Job>
}
