package com.toasterofbread.spmp.model.mediaitem

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressItem
import dev.toastbits.composekit.components.platform.composable.platformClickableWithOffset
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.composable.OnChangedEffect
import dev.toastbits.composekit.util.platform.Platform
import kotlinx.coroutines.delay

enum class MediaItemPreviewInteractionPressStage {
    INSTANT, BRIEF, LONG_1, LONG_2;

    fun execute(
        item: MediaItem,
        long_press_menu_data: LongPressMenuData,
        click_offset: Offset,
        onClick: (item: MediaItem, multiselect_key: Int?) -> Unit,
        onLongClick: (item: MediaItem, long_press_menu_data: LongPressMenuData) -> Unit
    ) {
        long_press_menu_data.click_offset = click_offset
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

@Composable
fun Modifier.mediaItemPreviewInteraction(
    item: MediaItem,
    long_press_menu_data: LongPressMenuData,
    enabled: Boolean = true,
    onClick: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    onLongClick: ((item: MediaItem, long_press_menu_data: LongPressMenuData) -> Unit)? = null
): Modifier {
    val base: Modifier = when (Platform.current) {
        Platform.ANDROID -> androidMediaItemPreviewInteraction(item, long_press_menu_data, enabled, onClick, onLongClick)
        Platform.DESKTOP,
        Platform.WEB -> desktopMediaItemPreviewInteraction(item, long_press_menu_data, enabled, onClick, onLongClick)
    }
    return base.longPressItem(long_press_menu_data)
}

@Composable
private fun Modifier.desktopMediaItemPreviewInteraction(
    item: MediaItem,
    long_press_menu_data: LongPressMenuData,
    enabled: Boolean = true,
    onClick: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    onLongClick: ((item: MediaItem, long_press_menu_data: LongPressMenuData) -> Unit)? = null
): Modifier {
    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

    val onItemClick = onClick ?: { item, key -> click_overrides.onMediaItemClicked(item, player, key) }
    val onItemLongClick = onLongClick ?: { item, data -> click_overrides.onMediaItemAltClicked(item, player, data) }

    return platformClickableWithOffset(
        onClick = { MediaItemPreviewInteractionPressStage.INSTANT.execute(item, long_press_menu_data, it, onItemClick, onItemLongClick) },
        onAltClick = { MediaItemPreviewInteractionPressStage.LONG_1.execute(item, long_press_menu_data, it, onItemClick, onItemLongClick) },
        onAlt2Click = { MediaItemPreviewInteractionPressStage.LONG_2.execute(item, long_press_menu_data, it, onItemClick, onItemLongClick) },
        enabled = enabled,
        indication = null
    )
}

@Composable
private fun Modifier.androidMediaItemPreviewInteraction(
    item: MediaItem,
    long_press_menu_data: LongPressMenuData,
    enabled: Boolean = true,
    onClick: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    onLongClick: ((item: MediaItem, long_press_menu_data: LongPressMenuData) -> Unit)? = null
): Modifier {
    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

    val onItemClick = onClick ?: { item, key -> click_overrides.onMediaItemClicked(item, player, key) }
    val onItemLongClick = onLongClick ?: { item, data -> click_overrides.onMediaItemAltClicked(item, player, data) }

    var current_press_stage: MediaItemPreviewInteractionPressStage by remember { mutableStateOf(MediaItemPreviewInteractionPressStage.INSTANT) }
    val long_press_timeout: Long = LocalViewConfiguration.current.longPressTimeoutMillis

    val interaction_source: MutableInteractionSource = remember { MutableInteractionSource() }
    val pressed: Boolean by interaction_source.collectIsPressedAsState()

    OnChangedEffect(pressed) {
        if (pressed) {
            var delays = 0
            for (stage in MediaItemPreviewInteractionPressStage.entries) {
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

                    if (stage == MediaItemPreviewInteractionPressStage.entries.last { it.isAvailable(long_press_menu_data) }) {
                        current_press_stage.execute(item, long_press_menu_data, Offset.Zero, onItemClick, onItemLongClick)
                        long_press_menu_data.current_interaction_stage = null
                        break
                    }
                }
            }
        }
        else {
            if (current_press_stage != MediaItemPreviewInteractionPressStage.entries.last { it.isAvailable(long_press_menu_data) }) {
                current_press_stage.execute(item, long_press_menu_data, Offset.Zero, onItemClick, onItemLongClick)
            }
            current_press_stage = MediaItemPreviewInteractionPressStage.INSTANT
            long_press_menu_data.current_interaction_stage = null
        }
    }

    return clickable(
        interaction_source,
        null,
        onClick = {
            current_press_stage.execute(item, long_press_menu_data, Offset.Zero, onItemClick, onItemLongClick)
        },
        enabled = enabled
    )
}
