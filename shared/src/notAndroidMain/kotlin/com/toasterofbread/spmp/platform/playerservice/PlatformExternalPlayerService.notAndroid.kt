package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import com.toasterofbread.spmp.platform.AppContext

actual class PlatformExternalPlayerService: ExternalPlayerService(plays_audio = false), PlayerService {
    actual companion object: PlayerServiceCompanion {
        override fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean = true

        override fun isServiceRunning(context: AppContext): Boolean = true

        override fun disconnect(context: AppContext, connection: Any) {
            (connection as ExternalPlayerService).onDestroy()
        }

        override fun connect(
            context: AppContext,
            launch_arguments: ProgramArguments,
            instance: PlayerService?,
            onConnected: (PlayerService) -> Unit,
            onDisconnected: () -> Unit,
        ): Any {
            require(instance is ExternalPlayerService?)
            val service: ExternalPlayerService =
                if (instance != null) instance.also { it.setContext(context) }
                else PlatformExternalPlayerService().also {
                    it.setContext(context)
                    it.onCreate()
                }
            onConnected(service)
            return service
        }
    }
}
