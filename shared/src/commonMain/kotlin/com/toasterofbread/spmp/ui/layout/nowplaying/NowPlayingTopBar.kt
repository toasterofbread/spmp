package com.toasterofbread.spmp.ui.layout.nowplaying

import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.PortraitLayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.DisplayBar
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.math.roundToInt
import LocalPlayerState

class NowPlayingTopBar {
    private val slot: LayoutSlot = PortraitLayoutSlot.PLAYER_TOP
    private var config: PortraitLayoutSlot.PlayerTopConfig = PortraitLayoutSlot.PlayerTopConfig()

    private var _height: Dp by mutableStateOf(0.dp)

    var displaying: Boolean by mutableStateOf(true)
        private set

    val height: Dp
        get() =
            if (slot.mustShow() || displaying) _height
            else 0.dp

    fun shouldShowInQueue(): Boolean =
        config?.show_in_queue == true || slot.mustShow()

    @Composable
    fun DisplayTopBar(
        expansion: PlayerExpansionState,
        distance_to_page: Dp,
        modifier: Modifier = Modifier,
        container_modifier: Modifier = Modifier
    ) {
        val player: PlayerState = LocalPlayerState.current
        val density: Density = LocalDensity.current

        val scale: Float by remember { derivedStateOf {
            if (!shouldShowInQueue() || expansion.getBounded() < 1f) expansion.getAbsolute().coerceAtMost(1f)
            else 1f
        } }

        displaying = slot.DisplayBar(
            distance_to_page = distance_to_page,
            modifier = modifier,
            container_modifier =
                container_modifier
                    .onSizeChanged {
                        with (density) {
                            _height = it.height.toDp()
                        }
                    }
                    .offset {
                        IntOffset(
                            0,
                            (-_height.toPx() * (1f - scale)).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        alpha = scale
                    },
            onConfigDataChanged = { config_data ->
                config = config_data?.let { Json.decodeFromJsonElement(it) } ?: PortraitLayoutSlot.PlayerTopConfig()
            },
            getParentBackgroundColour = { player.getNPBackground() }
        )
    }
}
