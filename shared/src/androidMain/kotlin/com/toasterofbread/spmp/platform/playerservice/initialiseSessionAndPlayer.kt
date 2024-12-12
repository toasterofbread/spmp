package com.toasterofbread.spmp.platform.playerservice

import android.media.session.MediaSession
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
internal fun ForegroundPlayerService.initialiseSessionAndPlayer(
    play_when_ready: Boolean,
    playlist_auto_progress: Boolean,
    coroutine_scope: CoroutineScope,
    data_spec_processor: MediaDataSpecProcessor,
    getNotificationPlayer: () -> PlayerService,
    onSongReadyToPlay: () -> Unit = {}
) = runBlocking {
    val service: ForegroundPlayerService = this@initialiseSessionAndPlayer

    audio_sink = DefaultAudioSink.Builder(context.ctx)
        .setAudioProcessorChain(
            DefaultAudioProcessorChain(
                arrayOf(ForegroundPlayerService.fft_audio_processor),
                SilenceSkippingAudioProcessor(),
                SonicAudioProcessor()
            )
        )
        .build()

    audio_sink.skipSilenceEnabled = context.settings.Streaming.ENABLE_SILENCE_SKIPPING.get()

    val renderers_factory: RenderersFactory =
        RenderersFactory { handler: Handler?, _, audioListener: AudioRendererEventListener?, _, _ ->
            arrayOf(
                MediaCodecAudioRenderer(
                    service,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    audio_sink
                )
            )
        }

    player = ExoPlayer.Builder(
        service,
        renderers_factory,
        DefaultMediaSourceFactory(createDataSourceFactory(data_spec_processor))
            .setLoadErrorHandlingPolicy(
                object : LoadErrorHandlingPolicy {
                    override fun getFallbackSelectionFor(
                        fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
                        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
                    ): LoadErrorHandlingPolicy.FallbackSelection? {
                        return null
                    }

                    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                        data_spec_processor.onLoadFailure(loadErrorInfo)

                        if (loadErrorInfo.exception.cause is VideoFormatsEndpoint.YoutubeMusicPremiumContentException) {
                            // Returning Long.MAX_VALUE leads to immediate retry, and returning C.TIME_UNSET cancels the notification entirely for some reason
                            return 10000000
                        }
                        return 1000 * 10
                    }

                    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                        return Int.MAX_VALUE
                    }
                }
            )
    )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setUsePlatformDiagnostics(false)
        .build()

    val player_listener: ForegroundPlayerServicePlayerListener =
        ForegroundPlayerServicePlayerListener(
            service,
            onSongReadyToPlay = onSongReadyToPlay
        )
    player.addListener(player_listener)

    player.playWhenReady = play_when_ready
    player.pauseAtEndOfMediaItems = !playlist_auto_progress
    player.prepare()

    media_session = MediaSession(service, "ForegroundPlayerService")
    media_session.setCallback(PlayerSessionCallback(getNotificationPlayer(), context, coroutine_scope))
    media_session.isActive = true
}
