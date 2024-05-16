package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlatformBinder
import dev.toastbits.composekit.platform.PlatformFile

private class PlayerServiceBinder(val service: PlatformInternalPlayerService): PlatformBinder()

actual class PlatformInternalPlayerService: ExternalPlayerService(plays_audio = false) {
    private fun launchLocalServer(launch_arguments: ProgramArguments) {
        LocalServer.startLocalServer(
            context,
            launch_arguments,
            context.settings.platform.SERVER_PORT.get()
        ) { result, stderr ->
            if (result != 0) {
                throw RuntimeException("Local server exited ($result): $stderr")
            }
        }
    }

    actual companion object: PlayerServiceCompanion {
        actual fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean =
            LocalServer.getServerExecutableFile(launch_arguments) != null

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
                        it.launchLocalServer(launch_arguments)
                    }
                else
                    PlatformInternalPlayerService().also {
                        it.setContext(context)
                        it.launchLocalServer(launch_arguments)
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
