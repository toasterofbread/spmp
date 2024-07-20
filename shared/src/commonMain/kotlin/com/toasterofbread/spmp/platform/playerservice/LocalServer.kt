package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Job

expect object LocalServer {
    @Composable
    fun getLocalServerUnavailabilityReason(): String?

    fun startLocalServer(
        context: AppContext,
        port: Int
    ): Job
}
