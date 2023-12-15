package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.DropdownSettingsItem
import com.toasterofbread.composekit.settings.ui.item.MultipleChoiceSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.platform.download.DownloadMethod
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.youtubeapi.formats.VideoFormatsEndpointType

internal fun getStreamingCategoryItems(): List<SettingsItem> {
    return listOf(
        DropdownSettingsItem(
            SettingsValueState(StreamingSettings.Key.VIDEO_FORMATS_METHOD.getName()),
            getString("s_key_video_formats_endpoint"), null, VideoFormatsEndpointType.entries.size
        ) { i ->
            VideoFormatsEndpointType.entries[i].getReadable()
        },

        ToggleSettingsItem(
            SettingsValueState(StreamingSettings.Key.AUTO_DOWNLOAD_ENABLED.getName()),
            getString("s_key_auto_download_enabled"), null
        ),

        AppSliderItem(
            SettingsValueState<Int>(StreamingSettings.Key.AUTO_DOWNLOAD_THRESHOLD.getName()),
            getString("s_key_auto_download_threshold"), getString("s_sub_auto_download_threshold"),
            range = 1f..10f,
            min_label = "1",
            max_label = "10"
        ),

        ToggleSettingsItem(
            SettingsValueState(StreamingSettings.Key.AUTO_DOWNLOAD_ON_METERED.getName()),
            getString("s_key_auto_download_on_metered"), null
        ),

        DropdownSettingsItem(
            SettingsValueState(StreamingSettings.Key.STREAM_AUDIO_QUALITY.getName()),
            getString("s_key_stream_audio_quality"), getString("s_sub_stream_audio_quality"), 3
        ) { i ->
            when (i) {
                SongAudioQuality.HIGH.ordinal -> getString("s_option_audio_quality_high")
                SongAudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else -> getString("s_option_audio_quality_low")
            }
        },

        DropdownSettingsItem(
            SettingsValueState(StreamingSettings.Key.DOWNLOAD_AUDIO_QUALITY.getName()),
            getString("s_key_download_audio_quality"), getString("s_sub_download_audio_quality"), 3
        ) { i ->
            when (i) {
                SongAudioQuality.HIGH.ordinal -> getString("s_option_audio_quality_high")
                SongAudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else -> getString("s_option_audio_quality_low")
            }
        },

        ToggleSettingsItem(
            SettingsValueState(StreamingSettings.Key.ENABLE_AUDIO_NORMALISATION.getName()),
            getString("s_key_enable_audio_normalisation"), getString("s_sub_enable_audio_normalisation")
        ),

        ToggleSettingsItem(
            SettingsValueState(StreamingSettings.Key.ENABLE_SILENCE_SKIPPING.getName()),
            getString("s_key_enable_silence_skipping"), null
        ),

        MultipleChoiceSettingsItem(
            SettingsValueState(StreamingSettings.Key.DOWNLOAD_METHOD.getName()),
            getString("s_key_download_method"), getString("s_sub_download_method"),
            DownloadMethod.available.size
        ) {
            val method: DownloadMethod = DownloadMethod.available[it]
            return@MultipleChoiceSettingsItem method.getTitle() + " - " + method.getDescription()
        },

        ToggleSettingsItem(
            SettingsValueState(StreamingSettings.Key.SKIP_DOWNLOAD_METHOD_CONFIRMATION.getName()),
            getString("s_key_skip_download_method_confirmation"), getString("s_sub_skip_download_method_confirmation")
        )
    )
}
