package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsGroupItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsMultipleChoiceItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsSliderItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsToggleItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.NowPlayingQueueRadioInfoPosition
import com.toasterofbread.spmp.model.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.model.OverscrollClearMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction

internal fun getPlayerCategory(): List<SettingsItem> {
    return listOf(
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_MINI_PLAYER_SHOW_PREV_BUTTON.name),
            getString("s_key_mini_player_show_prev_button"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_MINI_PLAYER_OVERSCROLL_CLEAR_ENABLED.name),
            getString("s_key_mini_player_overscroll_clear_enabled"), null
        ),

        AppSliderItem(
            SettingsValueState(Settings.KEY_MINI_PLAYER_OVERSCROLL_CLEAR_TIME.name),
            getString("s_key_mini_player_overscroll_clear_time"), null,
            range = 0f .. 1f
        ),

        SettingsMultipleChoiceItem(
            SettingsValueState(Settings.KEY_MINI_PLAYER_OVERSCROLL_CLEAR_MODE.name),
            getString("s_key_mini_player_overscroll_clear_mode"), null,
            choice_amount = OverscrollClearMode.values().size,
            radio_style = false
        ) { index ->
            OverscrollClearMode.values()[index].getReadable()
        },

        SettingsMultipleChoiceItem(
            SettingsValueState(Settings.KEY_PLAYER_OVERLAY_CUSTOM_ACTION.name),
            getString("s_key_player_overlay_menu_custom_action"),
            getString("s_sub_player_overlay_menu_custom_action"),
            PlayerOverlayMenuAction.values().size,
            false,
            { index ->
                PlayerOverlayMenuAction.values()[index].getReadable()
            }
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_PLAYER_OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS.name),
            getString("s_key_player_overlay_menu_swap_long_short_press_actions"),
            null,
            title_max_lines = 2
        ),

        SettingsGroupItem(null),

        SettingsMultipleChoiceItem(
            SettingsValueState(Settings.KEY_NP_QUEUE_WAVE_BORDER_MODE.name),
            getString("s_key_np_queue_wave_border_mode"),
            getString("s_sub_np_queue_wave_border_mode"),
            NowPlayingQueueWaveBorderMode.values().size,
            false,
            { index ->
                when (NowPlayingQueueWaveBorderMode.values()[index]) {
                    NowPlayingQueueWaveBorderMode.TIME -> getString("s_option_wave_border_mode_time")
                    NowPlayingQueueWaveBorderMode.TIME_SYNC -> getString("s_option_wave_border_mode_time_sync")
                    NowPlayingQueueWaveBorderMode.SCROLL -> getString("s_option_wave_border_mode_scroll")
                    NowPlayingQueueWaveBorderMode.NONE -> getString("s_option_wave_border_mode_none")
                    NowPlayingQueueWaveBorderMode.LINE -> getString("s_option_wave_border_mode_line")
                }
            }
        ),

        SettingsMultipleChoiceItem(
            SettingsValueState(Settings.KEY_NP_QUEUE_RADIO_INFO_POSITION.name),
            getString("s_key_np_queue_radio_info_position"), null,
            NowPlayingQueueRadioInfoPosition.values().size,
            false,
            { index ->
                NowPlayingQueueRadioInfoPosition.values()[index].getReadable()
            }
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_PAUSE_ON_BT_DISCONNECT.name),
            getString("s_key_pause_on_bt_disconnect"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_RESUME_ON_BT_CONNECT.name),
            getString("s_key_resume_on_bt_connect"), getString("s_sub_resume_on_bt_connect")
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_PAUSE_ON_WIRED_DISCONNECT.name),
            getString("s_key_pause_on_wired_disconnect"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_RESUME_ON_WIRED_CONNECT.name),
            getString("s_key_resume_on_wired_connect"), getString("s_sub_resume_on_wired_connect")
        )
    )
}
