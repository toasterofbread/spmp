package com.toasterofbread.spmp.ui.layout.nowplaying

import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.PortraitLayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.DisplayBar
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.offset
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class NowPlayingTopBar {
    private val slot: LayoutSlot = PortraitLayoutSlot.PLAYER_TOP
    private var config: PortraitLayoutSlot.PlayerTopConfig? = null

    private var scale: Float by mutableStateOf(1f)
    private var _height: Dp by mutableStateOf(0.dp)
    private var displaying: Boolean by mutableStateOf(true)

    val height: Dp
        get() =
            if (slot.mustShow() || displaying) _height * scale
            else 0.dp

    fun shouldShowInQueue(): Boolean =
        config?.show_in_queue == true || slot.mustShow()

    @Composable
    fun DisplayTopBar(
        expansion: NowPlayingExpansionState,
        distance_to_page: Dp,
        modifier: Modifier = Modifier,
        container_modifier: Modifier = Modifier
    ) {
        val density: Density = LocalDensity.current

        scale =
            if (!shouldShowInQueue() || expansion.getBounded() < 1f) expansion.getAbsolute().coerceAtMost(1f)
            else 1f

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
                    .offset(y = -_height * (1f - scale)),
            onConfigDataChanged = { config_data ->
                config = config_data?.let { Json.decodeFromJsonElement(it) }
            }
        )
    }
}
