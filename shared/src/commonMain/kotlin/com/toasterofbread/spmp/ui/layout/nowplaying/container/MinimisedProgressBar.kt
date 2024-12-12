package com.toasterofbread.spmp.ui.layout.nowplaying.container

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.fillMaxWidth
import dev.toastbits.composekit.components.utils.composable.RecomposeOnInterval
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.*

@Composable
internal fun MinimisedProgressBar(
    height: Dp,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current

    RecomposeOnInterval(POSITION_UPDATE_INTERVAL_MS) { state ->
        state

        LinearProgressIndicator(
            progress = player.status.getProgress(),
            color = player.getNPOnBackground(),
            trackColor = player.getNPOnBackground().copy(alpha = 0.5f),
            modifier = modifier
                .requiredHeight(height)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - expansion.get()
                }
        )
    }
}
