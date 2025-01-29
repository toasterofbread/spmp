package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.settings.category.VideoFormatsEndpointType
import com.toasterofbread.spmp.model.settings.category.isAvailable
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.util.getConvertedProperty
import dev.toastbits.composekit.util.toCustomResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_option_audio_quality_high
import spmp.shared.generated.resources.s_option_audio_quality_low
import spmp.shared.generated.resources.s_option_audio_quality_medium

internal fun getStreamingCategoryItems(context: AppContext): List<SettingsItem> {
    val available_video_formats: List<VideoFormatsEndpointType> =
        VideoFormatsEndpointType.entries.filter { it.isAvailable() }

    return listOf(
        DropdownSettingsItem(
            context.settings.Streaming.VIDEO_FORMATS_METHOD.getConvertedProperty(
                fromProperty = { it.ordinal },
                toProperty = { available_video_formats[it] }
            ),
            itemCount = available_video_formats.size,
            getItem = { available_video_formats[it].getReadable() }
        ),

        ToggleSettingsItem(
            context.settings.Streaming.ENABLE_VIDEO_FORMAT_FALLBACK
        ),

        ToggleSettingsItem(
            context.settings.Streaming.AUTO_DOWNLOAD_ENABLED
        ),

        AppSliderItem(
            context.settings.Streaming.AUTO_DOWNLOAD_THRESHOLD,
            range = 1f..10f,
            min_label = "1".toCustomResource(),
            max_label = "10".toCustomResource()
        ),

        ToggleSettingsItem(
            context.settings.Streaming.AUTO_DOWNLOAD_ON_METERED
        ),

        DropdownSettingsItem.ofEnumState(
            context.settings.Streaming.STREAM_AUDIO_QUALITY
        ) { quality ->
            when (quality) {
                SongAudioQuality.HIGH -> stringResource(Res.string.s_option_audio_quality_high)
                SongAudioQuality.MEDIUM -> stringResource(Res.string.s_option_audio_quality_medium)
                SongAudioQuality.LOW -> stringResource(Res.string.s_option_audio_quality_low)
            }
        },

        DropdownSettingsItem.ofEnumState(
            context.settings.Streaming.DOWNLOAD_AUDIO_QUALITY
        ) { quality ->
            when (quality) {
                SongAudioQuality.HIGH -> stringResource(Res.string.s_option_audio_quality_high)
                SongAudioQuality.MEDIUM -> stringResource(Res.string.s_option_audio_quality_medium)
                SongAudioQuality.LOW -> stringResource(Res.string.s_option_audio_quality_low)
            }
        },

        ToggleSettingsItem(
            context.settings.Streaming.ENABLE_AUDIO_NORMALISATION
        ),

        ToggleSettingsItem(
            context.settings.Streaming.ENABLE_SILENCE_SKIPPING
        ),

        MultipleChoiceSettingsItem(
            context.settings.Streaming.DOWNLOAD_METHOD
        ) { method ->
            method.getTitle() + " - " + method.getDescription()
        },

        ToggleSettingsItem(
            context.settings.Streaming.SKIP_DOWNLOAD_METHOD_CONFIRMATION
        )
    )
}
