package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlatformBinder
import dev.toastbits.spms.server.SpMs
import kotlinx.coroutines.launch

private class PlayerServiceBinder(val service: PlatformInternalPlayerService): PlatformBinder()

actual class PlatformInternalPlayerService: ExternalPlayerService(plays_audio = false) {
    private fun launchLocalServer() {
        context.coroutine_scope.launch {
            LocalServer.startLocalServer(
                context,
                context.settings.platform.SERVER_PORT.get()
            )
        }
    }

    actual companion object: PlayerServiceCompanion {
        override fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean =
            LocalServer.isAvailable()

        @Composable
        override fun getUnavailabilityReason(context: AppContext, launch_arguments: ProgramArguments): String? =
            LocalServer.getLocalServerUnavailabilityReason()

        override fun isServiceRunning(context: AppContext): Boolean = true

        override fun connect(
            context: AppContext,
            launch_arguments: ProgramArguments,
            instance: PlayerService?,
            onConnected: (PlayerService) -> Unit,
            onDisconnected: () -> Unit,
        ): Any {
            require(instance is PlatformInternalPlayerService?)
            val service: PlatformInternalPlayerService =
                if (instance != null)
                    instance.also {
                        it.setContext(context)
                        it.launchLocalServer()
                    }
                else
                    PlatformInternalPlayerService().also {
                        it.setContext(context)
                        it.launchLocalServer()
                        it.onCreate()
                    }
            onConnected(service)
            return service
        }

        override fun disconnect(context: AppContext, connection: Any) {
            (connection as ExternalPlayerService).onDestroy()
        }
    }
}
