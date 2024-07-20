package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.platform.download.DownloadMethod
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getStreamingCategoryItems
import com.toasterofbread.spmp.youtubeapi.NewPipeVideoFormatsEndpoint
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.formats.PipedVideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.YoutubeiVideoFormatsEndpoint
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_video_formats_endpoint
import spmp.shared.generated.resources.s_key_auto_download_enabled
import spmp.shared.generated.resources.s_key_auto_download_threshold
import spmp.shared.generated.resources.s_sub_auto_download_threshold
import spmp.shared.generated.resources.s_key_auto_download_on_metered
import spmp.shared.generated.resources.s_key_stream_audio_quality
import spmp.shared.generated.resources.s_sub_stream_audio_quality
import spmp.shared.generated.resources.s_key_download_audio_quality
import spmp.shared.generated.resources.s_sub_download_audio_quality
import spmp.shared.generated.resources.s_key_enable_audio_normalisation
import spmp.shared.generated.resources.s_sub_enable_audio_normalisation
import spmp.shared.generated.resources.s_key_enable_silence_skipping
import spmp.shared.generated.resources.s_key_download_method
import spmp.shared.generated.resources.s_sub_download_method
import spmp.shared.generated.resources.s_key_skip_download_method_confirmation
import spmp.shared.generated.resources.s_sub_skip_download_method_confirmation
import spmp.shared.generated.resources.s_cat_streaming
import spmp.shared.generated.resources.s_cat_desc_streaming
import spmp.shared.generated.resources.video_format_endpoint_youtubei
import spmp.shared.generated.resources.video_format_endpoint_piped
import spmp.shared.generated.resources.video_format_endpoint_newpipe

class StreamingSettings(val context: AppContext): SettingsGroup("STREAMING", context.getPrefs()) {
    val VIDEO_FORMATS_METHOD: PreferencesProperty<VideoFormatsEndpointType> by enumProperty(
        getName = { stringResource(Res.string.s_key_video_formats_endpoint) },
        getDescription = { null },
        getDefaultValue = { VideoFormatsEndpointType.DEFAULT }
    )
    val AUTO_DOWNLOAD_ENABLED: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_auto_download_enabled) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val AUTO_DOWNLOAD_THRESHOLD: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_auto_download_threshold) },
        getDescription = { stringResource(Res.string.s_sub_auto_download_threshold) },
        getDefaultValue = { 1 } // Listens
    )
    val AUTO_DOWNLOAD_ON_METERED: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_auto_download_on_metered) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STREAM_AUDIO_QUALITY: PreferencesProperty<SongAudioQuality> by enumProperty(
        getName = { stringResource(Res.string.s_key_stream_audio_quality) },
        getDescription = { stringResource(Res.string.s_sub_stream_audio_quality) },
        getDefaultValue = { SongAudioQuality.HIGH }
    )
    val DOWNLOAD_AUDIO_QUALITY: PreferencesProperty<SongAudioQuality> by enumProperty(
        getName = { stringResource(Res.string.s_key_download_audio_quality) },
        getDescription = { stringResource(Res.string.s_sub_download_audio_quality) },
        getDefaultValue = { SongAudioQuality.HIGH }
    )
    val ENABLE_AUDIO_NORMALISATION: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_audio_normalisation) },
        getDescription = { stringResource(Res.string.s_sub_enable_audio_normalisation) },
        getDefaultValue = { false }
    )
    val ENABLE_SILENCE_SKIPPING: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_silence_skipping) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val DOWNLOAD_METHOD: PreferencesProperty<DownloadMethod> by enumProperty(
        getName = { stringResource(Res.string.s_key_download_method) },
        getDescription = { stringResource(Res.string.s_sub_download_method) },
        getDefaultValue = { DownloadMethod.DEFAULT }
    )
    val SKIP_DOWNLOAD_METHOD_CONFIRMATION: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_skip_download_method_confirmation) },
        getDescription = { stringResource(Res.string.s_sub_skip_download_method_confirmation) },
        getDefaultValue = { false }
    )

    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_streaming) },
            { stringResource(Res.string.s_cat_desc_streaming) },
            { getStreamingCategoryItems(context) },
            { Icons.Outlined.Speaker }
        )
}

enum class VideoFormatsEndpointType {
    YOUTUBEI,
    PIPED,
    NEWPIPE;

    fun instantiate(api: YtmApi): VideoFormatsEndpoint =
        when (this) {
            YOUTUBEI -> YoutubeiVideoFormatsEndpoint(api)
            PIPED -> PipedVideoFormatsEndpoint(api)
            NEWPIPE -> NewPipeVideoFormatsEndpoint(api)
        }

    @Composable
    fun getReadable(): String =
        when (this) {
            YOUTUBEI -> stringResource(Res.string.video_format_endpoint_youtubei)
            PIPED -> stringResource(Res.string.video_format_endpoint_piped)
            NEWPIPE -> stringResource(Res.string.video_format_endpoint_newpipe)
        }

    companion object {
        val DEFAULT: VideoFormatsEndpointType = YOUTUBEI
    }
}
