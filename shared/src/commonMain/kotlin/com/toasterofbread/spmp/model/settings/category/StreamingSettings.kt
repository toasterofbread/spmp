package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadMethod
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getStreamingCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.model.YtmApi
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_streaming
import spmp.shared.generated.resources.s_cat_streaming
import spmp.shared.generated.resources.s_key_auto_download_enabled
import spmp.shared.generated.resources.s_key_auto_download_on_metered
import spmp.shared.generated.resources.s_key_auto_download_threshold
import spmp.shared.generated.resources.s_key_download_audio_quality
import spmp.shared.generated.resources.s_key_download_method
import spmp.shared.generated.resources.s_key_enable_audio_normalisation
import spmp.shared.generated.resources.s_key_enable_silence_skipping
import spmp.shared.generated.resources.s_key_skip_download_method_confirmation
import spmp.shared.generated.resources.s_key_stream_audio_quality
import spmp.shared.generated.resources.s_key_video_formats_endpoint
import spmp.shared.generated.resources.s_key_enable_video_format_fallback
import spmp.shared.generated.resources.s_sub_auto_download_threshold
import spmp.shared.generated.resources.s_sub_download_audio_quality
import spmp.shared.generated.resources.s_sub_download_method
import spmp.shared.generated.resources.s_sub_enable_audio_normalisation
import spmp.shared.generated.resources.s_sub_skip_download_method_confirmation
import spmp.shared.generated.resources.s_sub_stream_audio_quality
import spmp.shared.generated.resources.video_format_endpoint_newpipe
import spmp.shared.generated.resources.video_format_endpoint_piped
import spmp.shared.generated.resources.video_format_endpoint_youtubei

class StreamingSettings(val context: AppContext): SettingsGroupImpl("STREAMING", context.getPrefs()) {
    val VIDEO_FORMATS_METHOD: PlatformSettingsProperty<VideoFormatsEndpointType> by enumProperty(
        getName = { stringResource(Res.string.s_key_video_formats_endpoint) },
        getDescription = { null },
        getDefaultValue = { VideoFormatsEndpointType.DEFAULT }
    )
    val ENABLE_VIDEO_FORMAT_FALLBACK: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_video_format_fallback) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val AUTO_DOWNLOAD_ENABLED: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_auto_download_enabled) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val AUTO_DOWNLOAD_THRESHOLD: PlatformSettingsProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_auto_download_threshold) },
        getDescription = { stringResource(Res.string.s_sub_auto_download_threshold) },
        getDefaultValue = { 1 } // Listens
    )
    val AUTO_DOWNLOAD_ON_METERED: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_auto_download_on_metered) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STREAM_AUDIO_QUALITY: PlatformSettingsProperty<SongAudioQuality> by enumProperty(
        getName = { stringResource(Res.string.s_key_stream_audio_quality) },
        getDescription = { stringResource(Res.string.s_sub_stream_audio_quality) },
        getDefaultValue = { SongAudioQuality.HIGH }
    )
    val DOWNLOAD_AUDIO_QUALITY: PlatformSettingsProperty<SongAudioQuality> by enumProperty(
        getName = { stringResource(Res.string.s_key_download_audio_quality) },
        getDescription = { stringResource(Res.string.s_sub_download_audio_quality) },
        getDefaultValue = { SongAudioQuality.HIGH }
    )
    val ENABLE_AUDIO_NORMALISATION: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_audio_normalisation) },
        getDescription = { stringResource(Res.string.s_sub_enable_audio_normalisation) },
        getDefaultValue = { false }
    )
    val ENABLE_SILENCE_SKIPPING: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_enable_silence_skipping) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val DOWNLOAD_METHOD: PlatformSettingsProperty<DownloadMethod> by enumProperty(
        getName = { stringResource(Res.string.s_key_download_method) },
        getDescription = { stringResource(Res.string.s_sub_download_method) },
        getDefaultValue = { DownloadMethod.DEFAULT }
    )
    val SKIP_DOWNLOAD_METHOD_CONFIRMATION: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_skip_download_method_confirmation) },
        getDescription = { stringResource(Res.string.s_sub_skip_download_method_confirmation) },
        getDefaultValue = { false }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_streaming)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_streaming)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Speaker

    override fun getConfigurationItems(): List<SettingsItem> = getStreamingCategoryItems(context)

}

enum class VideoFormatsEndpointType {
    YOUTUBEI,
    PIPED,
    NEWPIPE;

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

expect fun VideoFormatsEndpointType.isAvailable(): Boolean

expect fun VideoFormatsEndpointType.instantiate(api: YtmApi): VideoFormatsEndpoint
