package com.spectre7.spmp.model

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.ui.component.LongPressMenuData
import com.spectre7.spmp.ui.layout.mainpage.PlayerViewContext
import com.spectre7.spmp.platform.Platform
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.utils.composable.OnChangedEffect
import kotlinx.coroutines.delay

private enum class PressStage {
    INSTANT, BRIEF, LONG_1, LONG_2;

    fun execute(item: MediaItem, playerProvider: () -> PlayerViewContext, long_press_menu_data: LongPressMenuData) {
        when (this) {
            BRIEF -> {}
            INSTANT -> {
                if (long_press_menu_data.multiselect_context?.is_active == true) {
                    long_press_menu_data.multiselect_context.toggleItem(item, long_press_menu_data.multiselect_key)
                }
                else {
                    playerProvider().onMediaItemClicked(item)
                }
            }
            LONG_1 -> playerProvider().showLongPressMenu(long_press_menu_data)
            LONG_2 -> long_press_menu_data.multiselect_context?.apply {
                setActive(true)
                toggleItem(item, long_press_menu_data.multiselect_key)
            }
        }
    }

    fun isAvailable(long_press_menu_data: LongPressMenuData): Boolean =
        when (this) {
            LONG_2 -> long_press_menu_data.multiselect_context != null
            else -> true
        }
}

private fun getIndication(): Indication? = null

@Composable
fun Modifier.mediaItemPreviewInteraction(
    item: MediaItem,
    playerProvider: () -> PlayerViewContext,
    long_press_menu_data: LongPressMenuData
): Modifier {
    if (Platform.is_desktop) {
        return platformClickable(
            onClick = { PressStage.INSTANT.execute(item, playerProvider, long_press_menu_data) },
            onAltClick = { PressStage.LONG_1.execute(item, playerProvider, long_press_menu_data) },
            indication = getIndication()
        )
    }

    var current_press_stage: PressStage by remember { mutableStateOf(PressStage.INSTANT) }
    val long_press_timeout = LocalViewConfiguration.current.longPressTimeoutMillis

    val interaction_source = remember { MutableInteractionSource() }
    val pressed by interaction_source.collectIsPressedAsState()

    OnChangedEffect(pressed) {
        if (pressed) {
            var delays = 0
            for (stage in PressStage.values()) {
                if (stage.ordinal == 0 || !stage.isAvailable(long_press_menu_data)) {
                    continue
                }

                if (stage.ordinal == 1) {
                    current_press_stage = stage
                }
                else {
                    delay(long_press_timeout * (++delays))
                    current_press_stage = stage
                    SpMp.context.vibrateShort()

                    if (stage == PressStage.values().last { it.isAvailable(long_press_menu_data) }) {
                        current_press_stage.execute(item, playerProvider, long_press_menu_data)
                        break
                    }
                }
            }
        }
        else {
            if (current_press_stage != PressStage.values().last { it.isAvailable(long_press_menu_data) }) {
                current_press_stage.execute(item, playerProvider, long_press_menu_data)
            }
            current_press_stage = PressStage.INSTANT
        }
    }

    return clickable(interaction_source, getIndication(), onClick = {
        current_press_stage.execute(item, playerProvider, long_press_menu_data)
    })
}
