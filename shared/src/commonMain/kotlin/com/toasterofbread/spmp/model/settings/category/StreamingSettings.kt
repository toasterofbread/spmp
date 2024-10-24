package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speaker
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.platform.download.DownloadMethod
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getStreamingCategoryItems
import com.toasterofbread.spmp.youtubeapi.NewPipeVideoFormatsEndpoint
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.formats.PipedVideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.YoutubeiVideoFormatsEndpoint
import dev.toastbits.composekit.platform.PreferencesProperty

class StreamingSettings(val context: AppContext): SettingsGroup("STREAMING", context.getPrefs()) {
    val VIDEO_FORMATS_METHOD: PreferencesProperty<VideoFormatsEndpointType> by enumProperty(
        getName = { getString("s_key_video_formats_endpoint") },
        getDescription = { null },
        getDefaultValue = { VideoFormatsEndpointType.DEFAULT }
    )
    val ENABLE_VIDEO_FORMAT_FALLBACK: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_enable_video_format_fallback") },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val AUTO_DOWNLOAD_ENABLED: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_auto_download_enabled") },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val AUTO_DOWNLOAD_THRESHOLD: PreferencesProperty<Int> by property(
        getName = { getString("s_key_auto_download_threshold") },
        getDescription = { getString("s_sub_auto_download_threshold") },
        getDefaultValue = { 1 } // Listens
    )
    val AUTO_DOWNLOAD_ON_METERED: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_auto_download_on_metered") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STREAM_AUDIO_QUALITY: PreferencesProperty<SongAudioQuality> by enumProperty(
        getName = { getString("s_key_stream_audio_quality") },
        getDescription = { getString("s_sub_stream_audio_quality") },
        getDefaultValue = { SongAudioQuality.HIGH }
    )
    val DOWNLOAD_AUDIO_QUALITY: PreferencesProperty<SongAudioQuality> by enumProperty(
        getName = { getString("s_key_download_audio_quality") },
        getDescription = { getString("s_sub_download_audio_quality") },
        getDefaultValue = { SongAudioQuality.HIGH }
    )
    val ENABLE_AUDIO_NORMALISATION: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_enable_audio_normalisation") },
        getDescription = { getString("s_sub_enable_audio_normalisation") },
        getDefaultValue = { false }
    )
    val ENABLE_SILENCE_SKIPPING: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_enable_silence_skipping") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val DOWNLOAD_METHOD: PreferencesProperty<DownloadMethod> by enumProperty(
        getName = { getString("s_key_download_method") },
        getDescription = { getString("s_sub_download_method") },
        getDefaultValue = { DownloadMethod.DEFAULT }
    )
    val SKIP_DOWNLOAD_METHOD_CONFIRMATION: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_skip_download_method_confirmation") },
        getDescription = { getString("s_sub_skip_download_method_confirmation") },
        getDefaultValue = { false }
    )

    override val page: CategoryPage? =
        SimplePage(
            { getString("s_cat_streaming") },
            { getString("s_cat_desc_streaming") },
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

    fun getReadable(): String =
        when (this) {
            YOUTUBEI -> getString("video_format_endpoint_youtubei")
            PIPED -> getString("video_format_endpoint_piped")
            NEWPIPE -> getString("video_format_endpoint_newpipe")
        }

    companion object {
        val DEFAULT: VideoFormatsEndpointType = YOUTUBEI
    }
}
