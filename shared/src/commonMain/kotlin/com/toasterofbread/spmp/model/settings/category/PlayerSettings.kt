package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getPlayerCategoryItems
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction

data object PlayerSettings: SettingsCategory("player") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_player"),
            getString("s_cat_desc_player"),
            { getPlayerCategoryItems() },
            { Icons.Outlined.PlayArrow }
        )

    enum class Key: SettingsKey {
        MINI_SHOW_PREV_BUTTON,
        MINI_OVERSCROLL_CLEAR_ENABLED,
        MINI_OVERSCROLL_CLEAR_TIME,
        MINI_OVERSCROLL_CLEAR_MODE,
        SHOW_REPEAT_SHUFFLE_BUTTONS,
        SHOW_SEEK_BAR_GRADIENT,
        OVERLAY_CUSTOM_ACTION,
        OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS,
        QUEUE_ITEM_SWIPE_SENSITIVITY,
        QUEUE_EXTRA_SIDE_PADDING,
        QUEUE_WAVE_BORDER_MODE,
        QUEUE_RADIO_INFO_POSITION,
        RESUME_ON_BT_CONNECT,
        PAUSE_ON_BT_DISCONNECT,
        RESUME_ON_WIRED_CONNECT,
        PAUSE_ON_WIRED_DISCONNECT;

        override val category: SettingsCategory get() = PlayerSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                MINI_SHOW_PREV_BUTTON -> false
                MINI_OVERSCROLL_CLEAR_ENABLED -> false
                MINI_OVERSCROLL_CLEAR_TIME -> 0.2f
                MINI_OVERSCROLL_CLEAR_MODE -> OverscrollClearMode.HIDE_IF_QUEUE_EMPTY.ordinal
                SHOW_REPEAT_SHUFFLE_BUTTONS -> false
                SHOW_SEEK_BAR_GRADIENT -> true
                OVERLAY_CUSTOM_ACTION -> PlayerOverlayMenuAction.DEFAULT_CUSTOM.ordinal
                OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS -> false
                QUEUE_ITEM_SWIPE_SENSITIVITY -> 1f
                QUEUE_EXTRA_SIDE_PADDING -> 0f
                QUEUE_WAVE_BORDER_MODE -> NowPlayingQueueWaveBorderMode.TIME.ordinal
                QUEUE_RADIO_INFO_POSITION -> NowPlayingQueueRadioInfoPosition.TOP_BAR.ordinal
                RESUME_ON_BT_CONNECT -> true
                PAUSE_ON_BT_DISCONNECT -> true
                RESUME_ON_WIRED_CONNECT -> true
                PAUSE_ON_WIRED_DISCONNECT -> true
            } as T
    }
}

enum class NowPlayingQueueRadioInfoPosition {
    TOP_BAR, ABOVE_ITEMS;

    fun getReadable(): String =
        when (this) {
            TOP_BAR -> getString("s_option_np_queue_radio_info_top_bar")
            ABOVE_ITEMS -> getString("s_option_np_queue_radio_info_above_items")
        }
}

enum class OverscrollClearMode {
    ALWAYS_HIDE,
    HIDE_IF_QUEUE_EMPTY,
    NONE_IF_QUEUE_EMPTY;

    fun getReadable(): String =
        when (this) {
            ALWAYS_HIDE -> getString("s_option_mini_player_overscroll_clear_mode_always_hide")
            HIDE_IF_QUEUE_EMPTY -> getString("s_option_mini_player_overscroll_clear_mode_hide_if_queue_empty")
            NONE_IF_QUEUE_EMPTY -> getString("s_option_mini_player_overscroll_clear_mode_none_if_queue_empty")
        }
}

enum class NowPlayingQueueWaveBorderMode {
    TIME, TIME_SYNC, SCROLL, NONE, LINE
}
