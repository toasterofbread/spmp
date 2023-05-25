package com.spectre7.spmp.model

import LocalPlayerState
import SpMp
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.spectre7.spmp.platform.Platform
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.ui.component.LongPressMenuData
import com.spectre7.spmp.ui.layout.mainpage.PlayerState
import com.spectre7.utils.composable.OnChangedEffect
import kotlinx.coroutines.delay

enum class MediaItemPreviewInteractionPressStage {
    INSTANT, BRIEF, LONG_1, LONG_2;

    fun execute(item: MediaItem, long_press_menu_data: LongPressMenuData, player: PlayerState) {
        when (this) {
            BRIEF -> {}
            INSTANT -> {
                if (long_press_menu_data.multiselect_context?.is_active == true) {
                    long_press_menu_data.multiselect_context.toggleItem(item, long_press_menu_data.multiselect_key)
                }
                else {
                    player.onMediaItemClicked(item, long_press_menu_data.multiselect_key)
                }
            }
            LONG_1 -> player.showLongPressMenu(long_press_menu_data)
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
    long_press_menu_data: LongPressMenuData
): Modifier {
    val player = LocalPlayerState.current

    if (Platform.is_desktop) {
        return platformClickable(
            onClick = { MediaItemPreviewInteractionPressStage.INSTANT.execute(item, long_press_menu_data, player) },
            onAltClick = { MediaItemPreviewInteractionPressStage.LONG_1.execute(item, long_press_menu_data, player) },
            indication = getIndication()
        )
    }

    var current_press_stage: MediaItemPreviewInteractionPressStage by remember { mutableStateOf(MediaItemPreviewInteractionPressStage.INSTANT) }
    val long_press_timeout = LocalViewConfiguration.current.longPressTimeoutMillis

    val interaction_source = remember { MutableInteractionSource() }
    val pressed by interaction_source.collectIsPressedAsState()

    OnChangedEffect(pressed) {
        if (pressed) {
            var delays = 0
            for (stage in MediaItemPreviewInteractionPressStage.values()) {
                if (stage.ordinal == 0 || !stage.isAvailable(long_press_menu_data)) {
                    continue
                }

                if (stage.ordinal == 1) {
                    current_press_stage = stage
                    long_press_menu_data.current_interaction_stage = stage
                }
                else {
                    delay(long_press_timeout * (++delays))
                    current_press_stage = stage
                    long_press_menu_data.current_interaction_stage = stage
                    SpMp.context.vibrateShort()

                    if (stage == MediaItemPreviewInteractionPressStage.values().last { it.isAvailable(long_press_menu_data) }) {
                        current_press_stage.execute(item, long_press_menu_data, player)
                        long_press_menu_data.current_interaction_stage = null
                        break
                    }
                }
            }
        }
        else {
            if (current_press_stage != MediaItemPreviewInteractionPressStage.values().last { it.isAvailable(long_press_menu_data) }) {
                current_press_stage.execute(item, long_press_menu_data, player)
            }
            current_press_stage = MediaItemPreviewInteractionPressStage.INSTANT
            long_press_menu_data.current_interaction_stage = null
        }
    }

    return clickable(interaction_source, getIndication(), onClick = {
        current_press_stage.execute(item, long_press_menu_data, player)
    })
}
