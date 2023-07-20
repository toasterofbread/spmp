package com.toasterofbread.spmp.ui.layout.prefspage

import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsMultipleChoiceItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString

internal fun getPlayerCategory(): List<SettingsItem> {
    return listOf(
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
        )
    )
}
