package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.GroupSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.MultipleChoiceSettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueRadioInfoPosition
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.model.settings.category.OverscrollClearMode
import com.toasterofbread.spmp.model.settings.category.PlayerSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import kotlin.math.roundToInt

internal fun getPlayerCategoryItems(): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.MINI_SHOW_PREV_BUTTON.getName()),
            getString("s_key_mini_player_show_prev_button"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.MINI_OVERSCROLL_CLEAR_ENABLED.getName()),
            getString("s_key_mini_player_overscroll_clear_enabled"), null
        ),

        AppSliderItem(
            SettingsValueState(PlayerSettings.Key.MINI_OVERSCROLL_CLEAR_TIME.getName()),
            getString("s_key_mini_player_overscroll_clear_time"), null,
            range = 0f .. 1f
        ),

        MultipleChoiceSettingsItem(
            SettingsValueState(PlayerSettings.Key.MINI_OVERSCROLL_CLEAR_MODE.getName()),
            getString("s_key_mini_player_overscroll_clear_mode"), null,
            choice_amount = OverscrollClearMode.entries.size
        ) { index ->
            OverscrollClearMode.entries[index].getReadable()
        },

        GroupSettingsItem(null),

        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.SHOW_REPEAT_SHUFFLE_BUTTONS.getName()),
            getString("s_key_player_show_repeat_shuffle_buttons"), getString("s_sub_player_show_repeat_shuffle_buttons"),
            title_max_lines = 2
        ),

        MultipleChoiceSettingsItem(
            SettingsValueState(PlayerSettings.Key.OVERLAY_CUSTOM_ACTION.getName()),
            getString("s_key_player_overlay_menu_custom_action"),
            getString("s_sub_player_overlay_menu_custom_action"),
            PlayerOverlayMenuAction.entries.size
        ) { index ->
            PlayerOverlayMenuAction.entries[index].getReadable()
        },

        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS.getName()),
            getString("s_key_player_overlay_menu_swap_long_short_press_actions"),
            null,
            title_max_lines = 2
        ),

        GroupSettingsItem(null),

        AppSliderItem(
            SettingsValueState(PlayerSettings.Key.QUEUE_ITEM_SWIPE_SENSITIVITY.getName()),
            getString("s_key_np_queue_item_swipe_sensitivity"),
            getString("s_sub_np_queue_item_swipe_sensitivity"),
            range = 0.1f .. 2f
        ),

        AppSliderItem(
            SettingsValueState(PlayerSettings.Key.QUEUE_EXTRA_SIDE_PADDING.getName()),
            getString("s_key_np_queue_extra_side_padding"),
            getString("s_sub_np_queue_extra_side_padding"),
            range = 0f .. 1f,
            min_label = "0%",
            max_label = "100%",
            getValueText = {
                (it as Float * 100).roundToInt().toString() + "%"
            }
        ),

        MultipleChoiceSettingsItem(
            SettingsValueState(PlayerSettings.Key.QUEUE_WAVE_BORDER_MODE.getName()),
            getString("s_key_np_queue_wave_border_mode"),
            getString("s_sub_np_queue_wave_border_mode"),
            NowPlayingQueueWaveBorderMode.entries.size
        ) { index ->
            when (NowPlayingQueueWaveBorderMode.entries[index]) {
                NowPlayingQueueWaveBorderMode.TIME -> getString("s_option_wave_border_mode_time")
                NowPlayingQueueWaveBorderMode.TIME_SYNC -> getString("s_option_wave_border_mode_time_sync")
                NowPlayingQueueWaveBorderMode.SCROLL -> getString("s_option_wave_border_mode_scroll")
                NowPlayingQueueWaveBorderMode.NONE -> getString("s_option_wave_border_mode_none")
                NowPlayingQueueWaveBorderMode.LINE -> getString("s_option_wave_border_mode_line")
            }
        },

        MultipleChoiceSettingsItem(
            SettingsValueState(PlayerSettings.Key.QUEUE_RADIO_INFO_POSITION.getName()),
            getString("s_key_np_queue_radio_info_position"), null,
            NowPlayingQueueRadioInfoPosition.entries.size
        ) { index ->
            NowPlayingQueueRadioInfoPosition.entries[index].getReadable()
        },

        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.PAUSE_ON_BT_DISCONNECT.getName()),
            getString("s_key_pause_on_bt_disconnect"), null
        ),
        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.RESUME_ON_BT_CONNECT.getName()),
            getString("s_key_resume_on_bt_connect"), getString("s_sub_resume_on_bt_connect")
        ),
        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.PAUSE_ON_WIRED_DISCONNECT.getName()),
            getString("s_key_pause_on_wired_disconnect"), null
        ),
        ToggleSettingsItem(
            SettingsValueState(PlayerSettings.Key.RESUME_ON_WIRED_CONNECT.getName()),
            getString("s_key_resume_on_wired_connect"), getString("s_sub_resume_on_wired_connect")
        )
    )
}
