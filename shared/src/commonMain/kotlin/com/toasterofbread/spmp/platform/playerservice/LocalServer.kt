package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import com.toasterofbread.spmp.platform.AppContext

data class LocalServerProcess(
    val launch_command: String,
    val process: Process
)

expect object LocalServer {
    fun canStartLocalServer(): Boolean

    fun startLocalServer(
        context: AppContext,
        launch_arguments: ProgramArguments?,
        port: Int,
        onExit: (Int, String) -> Unit
    ): LocalServerProcess?
}
