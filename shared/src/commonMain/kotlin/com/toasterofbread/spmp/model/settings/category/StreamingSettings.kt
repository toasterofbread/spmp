package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speaker
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.download.DownloadMethod
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getStreamingCategoryItems
import com.toasterofbread.spmp.youtubeapi.formats.VideoFormatsEndpointType

data object StreamingSettings: SettingsCategory("streaming") {
    override val keys: List<SettingsKey> = Key.values().toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_streaming"),
            getString("s_cat_desc_streaming"),
            { getStreamingCategoryItems() }
        ) { Icons.Outlined.Speaker }

    enum class Key: SettingsKey {
        VIDEO_FORMATS_METHOD,
        AUTO_DOWNLOAD_ENABLED,
        AUTO_DOWNLOAD_THRESHOLD,
        AUTO_DOWNLOAD_ON_METERED,
        STREAM_AUDIO_QUALITY,
        DOWNLOAD_AUDIO_QUALITY,
        ENABLE_AUDIO_NORMALISATION,
        ENABLE_SILENCE_SKIPPING,

        DOWNLOAD_METHOD,
        SKIP_DOWNLOAD_METHOD_CONFIRMATION;

        override val category: SettingsCategory get() = StreamingSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                VIDEO_FORMATS_METHOD -> VideoFormatsEndpointType.DEFAULT.ordinal
                AUTO_DOWNLOAD_ENABLED -> true
                AUTO_DOWNLOAD_THRESHOLD -> 1 // Listens
                AUTO_DOWNLOAD_ON_METERED -> false
                STREAM_AUDIO_QUALITY -> SongAudioQuality.MEDIUM.ordinal
                DOWNLOAD_AUDIO_QUALITY -> SongAudioQuality.MEDIUM.ordinal
                ENABLE_AUDIO_NORMALISATION -> false
                ENABLE_SILENCE_SKIPPING -> false
                DOWNLOAD_METHOD -> DownloadMethod.DEFAULT.ordinal
                SKIP_DOWNLOAD_METHOD_CONFIRMATION -> false
            } as T
    }
}
