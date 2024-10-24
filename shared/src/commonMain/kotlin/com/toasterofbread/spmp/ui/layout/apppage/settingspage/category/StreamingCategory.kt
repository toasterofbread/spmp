package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.item.DropdownSettingsItem
import dev.toastbits.composekit.settings.ui.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.settings.category.VideoFormatsEndpointType
import com.toasterofbread.spmp.platform.download.DownloadMethod
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem

internal fun getStreamingCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        DropdownSettingsItem(
            context.settings.streaming.VIDEO_FORMATS_METHOD
        ) { type ->
            type.getReadable()
        },

        ToggleSettingsItem(
            context.settings.streaming.ENABLE_VIDEO_FORMAT_FALLBACK
        ),

        ToggleSettingsItem(
            context.settings.streaming.AUTO_DOWNLOAD_ENABLED
        ),

        AppSliderItem(
            context.settings.streaming.AUTO_DOWNLOAD_THRESHOLD,
            range = 1f..10f,
            min_label = "1",
            max_label = "10"
        ),

        ToggleSettingsItem(
            context.settings.streaming.AUTO_DOWNLOAD_ON_METERED
        ),

        DropdownSettingsItem(
            context.settings.streaming.STREAM_AUDIO_QUALITY
        ) { quality ->
            when (quality) {
                SongAudioQuality.HIGH -> getString("s_option_audio_quality_high")
                SongAudioQuality.MEDIUM -> getString("s_option_audio_quality_medium")
                SongAudioQuality.LOW -> getString("s_option_audio_quality_low")
            }
        },

        DropdownSettingsItem(
            context.settings.streaming.DOWNLOAD_AUDIO_QUALITY
        ) { quality ->
            when (quality) {
                SongAudioQuality.HIGH -> getString("s_option_audio_quality_high")
                SongAudioQuality.MEDIUM -> getString("s_option_audio_quality_medium")
                SongAudioQuality.LOW -> getString("s_option_audio_quality_low")
            }
        },

        ToggleSettingsItem(
            context.settings.streaming.ENABLE_AUDIO_NORMALISATION
        ),

        ToggleSettingsItem(
            context.settings.streaming.ENABLE_SILENCE_SKIPPING
        ),

        MultipleChoiceSettingsItem(
            context.settings.streaming.DOWNLOAD_METHOD
        ) { method ->
            method.getTitle() + " - " + method.getDescription()
        },

        ToggleSettingsItem(
            context.settings.streaming.SKIP_DOWNLOAD_METHOD_CONFIRMATION
        )
    )
}
