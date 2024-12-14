package com.toasterofbread.spmp.ui.layout.nowplaying.container

import LocalPlayerState
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import dev.toastbits.composekit.context.vibrateShort
import com.toasterofbread.spmp.model.settings.category.*
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.delay

private const val OVERSCROLL_CLEAR_DISTANCE_THRESHOLD_DP: Float = 5f

internal fun Modifier.playerOverscroll(
    swipe_state: AnchoredDraggableState<Int>,
    swipe_interaction_source: MutableInteractionSource
): Modifier = composed {
    val swipe_interactions: MutableList<Interaction> = remember { mutableStateListOf() }
    LaunchedEffect(swipe_interaction_source) {
        swipe_interaction_source.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    swipe_interactions.add(interaction)
                }
                is DragInteraction.Stop -> {
                    swipe_interactions.remove(interaction.start)
                }
                is DragInteraction.Cancel -> {
                    swipe_interactions.remove(interaction.start)
                }
            }
        }
    }

    val player: PlayerState = LocalPlayerState.current
    val controller: PlayerService? = player.controller
    val density: Density = LocalDensity.current
    var player_alpha: Float by remember { mutableStateOf(1f) }

    val overscroll_clear_enabled: Boolean by player.settings.Player.MINI_OVERSCROLL_CLEAR_ENABLED.observe()
    val overscroll_clear_time: Float by player.settings.Player.MINI_OVERSCROLL_CLEAR_TIME.observe()
    val overscroll_clear_mode: OverscrollClearMode by player.settings.Player.MINI_OVERSCROLL_CLEAR_MODE.observe()

    LaunchedEffect(controller, swipe_interactions.isNotEmpty(), overscroll_clear_enabled) {
        if (!overscroll_clear_enabled || controller == null) {
            return@LaunchedEffect
        }

        val anchor: Float = swipe_state.anchors.positionOf(0)
        val delta: Long = 50
        val time_threshold: Float = overscroll_clear_time * 1000

        var time_below_threshold: Long = 0
        var triggered: Boolean = false

        player_alpha = 1f

        while (swipe_interactions.isNotEmpty()) {
            delay(delta)

            if (controller.item_count == 0 && overscroll_clear_mode == OverscrollClearMode.NONE_IF_QUEUE_EMPTY) {
                continue
            }

            if (time_threshold == 0f) {
                player_alpha = 1f
            }
            else {
                player_alpha = 1f - (time_below_threshold / time_threshold).coerceIn(0f, 1f)
            }

            val offset: Dp = (swipe_state.offset - anchor).npAnchorToDp(density, player.context, player.np_swipe_sensitivity)
            if (offset < -OVERSCROLL_CLEAR_DISTANCE_THRESHOLD_DP.dp) {
                if (!triggered && time_below_threshold >= time_threshold) {
                    if (
                        overscroll_clear_mode == OverscrollClearMode.ALWAYS_HIDE
                        || (overscroll_clear_mode == OverscrollClearMode.HIDE_IF_QUEUE_EMPTY && controller.item_count == 0)
                    ) {
                        controller.service_player.cancelSession()
                    }

                    if (controller.item_count > 0) {
                        controller.service_player.clearQueue()
                    }

                    controller.context.vibrateShort()

                    triggered = true
                }

                time_below_threshold += delta
            }
            else {
                time_below_threshold = 0
                triggered = false
            }
        }
    }

    return@composed graphicsLayer {
        alpha = player_alpha
    }
}
