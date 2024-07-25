package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import LocalAppState
import com.toasterofbread.spmp.platform.AppContext
import LocalProgramArguments
import LocalPlayerState
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.loading_splash_button_launch_without_server

internal class HeadlessExternalPlayerService: ExternalPlayerService(plays_audio = false), PlayerService {
    @Composable
    override fun PersistentContent(requestServiceChange: (PlayerServiceCompanion) -> Unit) {
        val state: SpMp.State = LocalAppState.current
        val launch_arguments: ProgramArguments = LocalProgramArguments.current
        val ui_only: Boolean by state.settings.platform.EXTERNAL_SERVER_MODE_UI_ONLY.observe()
        LaunchedEffect(ui_only) {
            if (!ui_only && PlatformExternalPlayerService.isAvailable(state.context, launch_arguments)) {
                requestServiceChange(PlatformExternalPlayerService.Companion)
            }
        }
    }

    @Composable
    override fun LoadScreenExtraContent(item_modifier: Modifier, requestServiceChange: (PlayerServiceCompanion) -> Unit) {
        val launch_arguments: ProgramArguments = LocalProgramArguments.current
        val internal_service_available: Boolean = remember(launch_arguments) { PlatformInternalPlayerService.Companion.isAvailable(context, launch_arguments) }

        if (internal_service_available) {
            Button(
                {
                    requestServiceChange(PlatformInternalPlayerService.Companion)
                },
                item_modifier
            ) {
                Text(stringResource(Res.string.loading_splash_button_launch_without_server))
            }
        }
    }

    companion object: PlayerServiceCompanion {
        override fun isServiceRunning(context: AppContext): Boolean = true
        override fun playsAudio(): Boolean = true

        override fun connect(
            context: AppContext,
            launch_arguments: ProgramArguments,
            instance: PlayerService?,
            onConnected: (PlayerService) -> Unit,
            onDisconnected: () -> Unit
        ): Any {
            require(instance is ExternalPlayerService?)
            val service: ExternalPlayerService =
                if (instance != null) instance.also { it.setContext(context) }
                else HeadlessExternalPlayerService().also {
                    it.setContext(context)
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
