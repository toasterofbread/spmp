package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.platform.AppContext

interface PlayerServiceCompanion {
    suspend fun getUnavailabilityReason(context: AppContext, launch_arguments: ProgramArguments): String? = null
    fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean

    fun isServiceRunning(context: AppContext): Boolean
    fun isServiceAttached(context: AppContext): Boolean = false
    fun playsAudio(): Boolean = false

    suspend fun connect(
        context: AppContext,
        launch_arguments: ProgramArguments,
        instance: PlayerService? = null,
        onConnected: (PlayerService) -> Unit,
        onDisconnected: () -> Unit
    ): Any

    fun disconnect(context: AppContext, connection: Any)
}
