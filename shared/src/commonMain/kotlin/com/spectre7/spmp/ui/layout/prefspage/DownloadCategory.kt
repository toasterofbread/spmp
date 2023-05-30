package com.spectre7.spmp.ui.layout.prefspage

import com.spectre7.settings.model.*
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.resources.getString

internal fun getDownloadCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_AUTO_DOWNLOAD_ENABLED.name),
            getString("s_key_auto_download_enabled"), null
        ),

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.name),
            getString("s_key_auto_download_threshold"), getString("s_sub_auto_download_threshold"),
            range = 1f..10f,
            min_label = "1",
            max_label = "10"
        ),

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_STREAM_AUDIO_QUALITY.name),
            getString("s_key_stream_audio_quality"), getString("s_sub_stream_audio_quality"), 3
        ) { i ->
            when (i) {
                Song.AudioQuality.HIGH.ordinal -> getString("s_option_audio_quality_high")
                Song.AudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else -> getString("s_option_audio_quality_low")
            }
        },

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_DOWNLOAD_AUDIO_QUALITY.name),
            getString("s_key_download_audio_quality"), getString("s_sub_download_audio_quality"), 3
        ) { i ->
            when (i) {
                Song.AudioQuality.HIGH.ordinal -> getString("s_option_audio_quality_high")
                Song.AudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else -> getString("s_option_audio_quality_low")
            }
        }
    )
}
