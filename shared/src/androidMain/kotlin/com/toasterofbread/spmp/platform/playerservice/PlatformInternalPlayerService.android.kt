package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.platform.AppContext
import ProgramArguments

actual class PlatformInternalPlayerService: ForegroundPlayerService(play_when_ready = true), PlayerService {
    actual companion object: InternalPlayerServiceCompanion(PlatformInternalPlayerService::class), PlayerServiceCompanion {
        actual fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean = true
    }

    @Composable
    actual override fun Visualiser(
        colour: Color,
        modifier: Modifier,
        opacity: Float,
    ) {
        super.Visualiser(colour, modifier, opacity)
    }
}
