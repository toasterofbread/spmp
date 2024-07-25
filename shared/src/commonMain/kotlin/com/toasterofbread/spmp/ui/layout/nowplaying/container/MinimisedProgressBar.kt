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
import dev.toastbits.composekit.utils.composable.RecomposeOnInterval
import LocalAppState
import LocalSessionState
import LocalUiState
import com.toasterofbread.spmp.model.state.SessionState
import com.toasterofbread.spmp.model.state.UiState
import com.toasterofbread.spmp.ui.layout.nowplaying.*

@Composable
internal fun MinimisedProgressBar(
    height: Dp,
    modifier: Modifier = Modifier
) {
    val ui_state: UiState = LocalUiState.current
    val session_state: SessionState = LocalSessionState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current

    RecomposeOnInterval(POSITION_UPDATE_INTERVAL_MS) { state ->
        state

        LinearProgressIndicator(
            progress = { session_state.status.getProgress() },
            modifier = modifier
                .requiredHeight(height)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - expansion.get()
                },
            color = ui_state.getNPOnBackground(),
            trackColor = ui_state.getNPOnBackground().copy(alpha = 0.5f),
        )
    }
}
