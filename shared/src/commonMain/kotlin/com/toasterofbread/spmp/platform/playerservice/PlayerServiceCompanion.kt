package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import com.toasterofbread.spmp.platform.AppContext

interface PlayerServiceCompanion {
    fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean = true
    fun isServiceRunning(context: AppContext): Boolean
    fun playsAudio(): Boolean = false

    fun connect(
        context: AppContext,
        launch_arguments: ProgramArguments,
        instance: PlayerService? = null,
        onConnected: (PlayerService) -> Unit,
        onDisconnected: () -> Unit
    ): Any

    fun disconnect(context: AppContext, connection: Any)
}
