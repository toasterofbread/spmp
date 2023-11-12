package com.toasterofbread.spmp.model.mediaitem

import LocalPlayerState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.utils.composable.OnChangedEffect
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.composekit.platform.Platform
import kotlinx.coroutines.delay

enum class MediaItemPreviewInteractionPressStage {
    INSTANT, BRIEF, LONG_1, LONG_2;

    fun execute(
        item: MediaItem, 
        long_press_menu_data: LongPressMenuData, 
        onClick: (item: MediaItem, multiselect_key: Int?) -> Unit,
        onLongClick: (item: MediaItem, long_press_menu_data: LongPressMenuData) -> Unit
    ) {
        when (this) {
            INSTANT -> {
                if (long_press_menu_data.multiselect_context?.is_active == true) {
                    long_press_menu_data.multiselect_context.toggleItem(item, long_press_menu_data.multiselect_key)
                }
                else {
                    onClick(item, long_press_menu_data.multiselect_key)
                }
            }
            BRIEF -> {}
            LONG_1 -> onLongClick(item, long_press_menu_data)
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
    long_press_menu_data: LongPressMenuData,
    onClick: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    onLongClick: ((item: MediaItem, long_press_menu_data: LongPressMenuData) -> Unit)? = null
): Modifier {
    val player = LocalPlayerState.current
    
    val onClick = onClick ?: player::onMediaItemClicked
    val onLongClick = onLongClick ?: player::onMediaItemLongClicked

    if (Platform.DESKTOP.isCurrent()) {
        return platformClickable(
            onClick = { MediaItemPreviewInteractionPressStage.INSTANT.execute(item, long_press_menu_data, onClick, onLongClick) },
            onAltClick = { MediaItemPreviewInteractionPressStage.LONG_1.execute(item, long_press_menu_data, onClick, onLongClick) },
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
                    player.context.vibrateShort()

                    if (stage == MediaItemPreviewInteractionPressStage.values().last { it.isAvailable(long_press_menu_data) }) {
                        current_press_stage.execute(item, long_press_menu_data, onClick, onLongClick)
                        long_press_menu_data.current_interaction_stage = null
                        break
                    }
                }
            }
        }
        else {
            if (current_press_stage != MediaItemPreviewInteractionPressStage.values().last { it.isAvailable(long_press_menu_data) }) {
                current_press_stage.execute(item, long_press_menu_data, onClick, onLongClick)
            }
            current_press_stage = MediaItemPreviewInteractionPressStage.INSTANT
            long_press_menu_data.current_interaction_stage = null
        }
    }

    return clickable(interaction_source, getIndication(), onClick = {
        current_press_stage.execute(item, long_press_menu_data, onClick, onLongClick)
    })
}
