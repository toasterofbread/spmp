package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import com.toasterofbread.spmp.platform.AppContext

actual object LocalServer {
    actual fun canStartLocalServer(): Boolean = false

    actual fun startLocalServer(
        context: AppContext,
        launch_arguments: ProgramArguments?,
        port: Int,
        onExit: (Int, String) -> Unit
    ): LocalServerProcess? = null
}
