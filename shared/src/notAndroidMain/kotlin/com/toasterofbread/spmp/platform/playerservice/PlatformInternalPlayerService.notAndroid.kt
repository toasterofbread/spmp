package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.platform.AppContext
<<<<<<< HEAD:shared/src/notAndroidMain/kotlin/com/toasterofbread/spmp/platform/playerservice/PlatformInternalPlayerService.notAndroid.kt
import com.toasterofbread.spmp.platform.PlatformBinder
=======
>>>>>>> main:shared/src/desktopMain/kotlin/com/toasterofbread/spmp/platform/playerservice/PlatformInternalPlayerService.desktop.kt
import dev.toastbits.spms.server.SpMs
import kotlinx.coroutines.launch

actual class PlatformInternalPlayerService: ExternalPlayerService(plays_audio = false) {
<<<<<<< HEAD:shared/src/notAndroidMain/kotlin/com/toasterofbread/spmp/platform/playerservice/PlatformInternalPlayerService.notAndroid.kt
    private fun launchLocalServer() {
        context.coroutine_scope.launch {
=======
    private fun autoLaunchLocalServer() {
        if (!context.settings.platform.SERVER_LOCAL_START_AUTOMATICALLY.get()) {
            return
        }

        try {
>>>>>>> main:shared/src/desktopMain/kotlin/com/toasterofbread/spmp/platform/playerservice/PlatformInternalPlayerService.desktop.kt
            LocalServer.startLocalServer(
                context,
                context.settings.platform.SERVER_PORT.get()
            )
        }
<<<<<<< HEAD:shared/src/notAndroidMain/kotlin/com/toasterofbread/spmp/platform/playerservice/PlatformInternalPlayerService.notAndroid.kt
=======
        catch (e: Throwable) {
            e.printStackTrace()
        }
>>>>>>> main:shared/src/desktopMain/kotlin/com/toasterofbread/spmp/platform/playerservice/PlatformInternalPlayerService.desktop.kt
    }

    actual companion object: PlayerServiceCompanion {
        override fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean =
            LocalServer.isAvailable()

        @Composable
        override fun getUnavailabilityReason(context: AppContext, launch_arguments: ProgramArguments): String? =
            LocalServer.getLocalServerUnavailabilityReason()

        override fun isServiceRunning(context: AppContext): Boolean = true

        override suspend fun connect(
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
                        it.autoLaunchLocalServer()
                    }
                else
                    PlatformInternalPlayerService().also {
                        it.setContext(context)
                        it.autoLaunchLocalServer()
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
