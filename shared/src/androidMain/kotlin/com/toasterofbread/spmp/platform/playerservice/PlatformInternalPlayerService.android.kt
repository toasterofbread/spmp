package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.platform.AppContext
import ProgramArguments
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

actual class PlatformInternalPlayerService: ForegroundPlayerService(play_when_ready = true), PlayerService {
    actual companion object: InternalPlayerServiceCompanion(PlatformInternalPlayerService::class), PlayerServiceCompanion {
        override fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean = true
        override fun playsAudio(): Boolean = true
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
