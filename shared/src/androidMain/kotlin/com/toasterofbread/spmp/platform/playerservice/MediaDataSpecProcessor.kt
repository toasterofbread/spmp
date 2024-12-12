package com.toasterofbread.spmp.platform.playerservice

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.toasterofbread.spmp.model.settings.category.VideoFormatsEndpointType
import com.toasterofbread.spmp.model.settings.category.instantiate
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getMediaDataSpecPlaybackUri
import dev.toastbits.composekit.settings.PlatformSettingsListener
import kotlinx.coroutines.runBlocking
import java.io.IOException

@OptIn(UnstableApi::class)
internal class MediaDataSpecProcessor(private val context: AppContext) {
    private var current_endpoint: VideoFormatsEndpointType? = null

    private val prefs_listener: PlatformSettingsListener =
        PlatformSettingsListener { key ->
            when (key) {
                context.settings.Streaming.ENABLE_VIDEO_FORMAT_FALLBACK.key,
                context.settings.Streaming.VIDEO_FORMATS_METHOD.key -> current_endpoint = null
            }
        }

    init {
        context.getPrefs().addListener(prefs_listener)
    }

    fun release() {
        context.getPrefs().removeListener(prefs_listener)
    }

    suspend fun processMediaDataSpec(data_spec: DataSpec): DataSpec {
        val endpoint: VideoFormatsEndpointType =
            current_endpoint
            ?: context.settings.Streaming.VIDEO_FORMATS_METHOD.get().also { current_endpoint = it }

        val uri: Uri =
            getMediaDataSpecPlaybackUri(
                data_spec,
                context,
                endpoint.instantiate(context.ytapi)
            ).getOrThrow()

        return data_spec.withUri(uri)
    }

    fun onLoadFailure(info: LoadErrorHandlingPolicy.LoadErrorInfo) {
        IOException(
            "Song load failed ($current_endpoint, ${info.loadEventInfo.dataSpec.uri})",
            info.exception
        ).printStackTrace()

        current_endpoint?.also {
            runBlocking {
                if (context.settings.Streaming.ENABLE_VIDEO_FORMAT_FALLBACK.get()) {
                    current_endpoint = it.getNext()
                }
            }
        }
    }

    private fun VideoFormatsEndpointType.getNext(): VideoFormatsEndpointType =
        VideoFormatsEndpointType.entries[
            (ordinal + 1) % VideoFormatsEndpointType.entries.size
        ]
}
