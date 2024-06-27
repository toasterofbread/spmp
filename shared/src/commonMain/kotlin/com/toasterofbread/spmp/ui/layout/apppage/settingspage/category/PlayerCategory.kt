package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueRadioInfoPosition
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.model.settings.category.OverscrollClearMode
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.platform.AppContext
import kotlin.enums.enumEntries
import kotlin.math.roundToInt

internal fun getPlayerCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        AppSliderItem(
            context.settings.player.EXPAND_SWIPE_SENSITIVITY,
            range = 0.1f .. 10f
        ),

        ToggleSettingsItem(
            context.settings.player.MINI_SHOW_PREV_BUTTON
        ),

        ToggleSettingsItem(
            context.settings.player.MINI_OVERSCROLL_CLEAR_ENABLED
        ),

        AppSliderItem(
            context.settings.player.MINI_OVERSCROLL_CLEAR_TIME,
            range = 0f .. 1f
        ),

        MultipleChoiceSettingsItem(
            context.settings.player.MINI_OVERSCROLL_CLEAR_MODE
        ) { mode ->
            mode.getReadable()
        },

        GroupSettingsItem(null),

        ToggleSettingsItem(
            context.settings.player.SHOW_REPEAT_SHUFFLE_BUTTONS,
            title_max_lines = 2
        ),

        ToggleSettingsItem(
            context.settings.player.SHOW_SEEK_BAR_GRADIENT,
            title_max_lines = 2
        ),

        ToggleSettingsItem(
            context.settings.player.LANDSCAPE_SWAP_CONTROLS_AND_IMAGE,
            title_max_lines = 2
        ),

        MultipleChoiceSettingsItem(
            context.settings.player.OVERLAY_CUSTOM_ACTION,
        ) { action ->
            action.getReadable()
        },

        ToggleSettingsItem(
            context.settings.player.OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS,
            title_max_lines = 2
        ),

        GroupSettingsItem(null),

        AppSliderItem(
            context.settings.player.QUEUE_ITEM_SWIPE_SENSITIVITY,
            range = 0.1f .. 2f
        ),

        AppSliderItem(
            context.settings.player.QUEUE_EXTRA_SIDE_PADDING,
            range = 0f .. 1f,
            min_label = "0%",
            max_label = "100%",
            getValueText = {
                (it as Float * 100).roundToInt().toString() + "%"
            }
        ),

        MultipleChoiceSettingsItem(
            context.settings.player.QUEUE_WAVE_BORDER_MODE,
        ) { mode ->
            when (mode) {
                NowPlayingQueueWaveBorderMode.TIME -> getString("s_option_wave_border_mode_time")
                NowPlayingQueueWaveBorderMode.TIME_SYNC -> getString("s_option_wave_border_mode_time_sync")
                NowPlayingQueueWaveBorderMode.SCROLL -> getString("s_option_wave_border_mode_scroll")
                NowPlayingQueueWaveBorderMode.NONE -> getString("s_option_wave_border_mode_none")
                NowPlayingQueueWaveBorderMode.LINE -> getString("s_option_wave_border_mode_line")
            }
        },

        MultipleChoiceSettingsItem(
            context.settings.player.QUEUE_RADIO_INFO_POSITION
        ) { position ->
            position.getReadable()
        },

        ToggleSettingsItem(
            context.settings.player.PAUSE_ON_BT_DISCONNECT
        ),
        ToggleSettingsItem(
            context.settings.player.RESUME_ON_BT_CONNECT
        ),
        ToggleSettingsItem(
            context.settings.player.PAUSE_ON_WIRED_DISCONNECT
        ),
        ToggleSettingsItem(
            context.settings.player.RESUME_ON_WIRED_CONNECT
        )
    )
}
