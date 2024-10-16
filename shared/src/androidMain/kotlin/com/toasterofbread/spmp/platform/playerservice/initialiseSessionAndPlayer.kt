package com.toasterofbread.spmp.platform.playerservice

import android.app.PendingIntent
import android.content.ComponentName
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.BitmapLoader
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
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.PlayerServiceCommand
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
internal fun ForegroundPlayerService.initialiseSessionAndPlayer(
    play_when_ready: Boolean,
    playlist_auto_progress: Boolean,
    getNotificationPlayer: (ExoPlayer) -> Player = { it },
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

    audio_sink.skipSilenceEnabled = context.settings.streaming.ENABLE_SILENCE_SKIPPING.get()

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
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            { arrayOf(MatroskaExtractor(), FragmentedMp4Extractor()) }
        )
        .setLoadErrorHandlingPolicy(
            object : LoadErrorHandlingPolicy {
                override fun getFallbackSelectionFor(
                    fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
                    loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
                    ): LoadErrorHandlingPolicy.FallbackSelection? {
                    return null
                }

                override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
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

    val player_listener: InternalPlayerServicePlayerListener =
        InternalPlayerServicePlayerListener(
            service,
            onSongReadyToPlay = onSongReadyToPlay
        )
    player.addListener(player_listener)

    player.playWhenReady = play_when_ready
    player.pauseAtEndOfMediaItems = !playlist_auto_progress
    player.prepare()

    val controller_future: ListenableFuture<MediaController> =
        MediaController.Builder(
            service,
            SessionToken(service, ComponentName(service, service::class.java))
        ).buildAsync()

    controller_future.addListener(
        { controller_future.get() },
        MoreExecutors.directExecutor()
    )

    media_session = MediaSession.Builder(service, getNotificationPlayer(player))
        .setBitmapLoader(object : BitmapLoader {
            val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

            override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
                throw NotImplementedError()
            }

            override fun supportsMimeType(mimeType: String): Boolean = true

            override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
                return executor.submit<Bitmap> {
                    runBlocking {
                        val song = SongRef(uri.toString())
                        var fail_error: Throwable? = null

                        for (quality in ThumbnailProvider.Quality.byQuality()) {
                            val load_result = MediaItemThumbnailLoader.loadItemThumbnail(song, quality, context)
                            load_result.fold(
                                { image ->
                                    val formatted_image: Bitmap =
                                        formatMediaNotificationImage(
                                            image.asAndroidBitmap(),
                                            song,
                                            context
                                        )

                                    context.onNotificationThumbnailLoaded(formatted_image)

                                    return@runBlocking formatted_image
                                },
                                { error ->
                                    if (fail_error == null) {
                                        fail_error = error
                                    }
                                }
                            )
                        }

                        throw fail_error!!
                    }
                }
            }
        })
        .setSessionActivity(
            PendingIntent.getActivity(
                service,
                1,
                packageManager.getLaunchIntentForPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setCallback(object : MediaSession.Callback {
            override fun onAddMediaItems(
                media_session: MediaSession,
                controller: MediaSession.ControllerInfo,
                media_items: List<MediaItem>,
            ): ListenableFuture<List<MediaItem>> {
                val updated_media_items = media_items.map { item ->
                    item.buildUpon()
                        .setUri(item.requestMetadata.mediaUri)
                        .setMediaId(item.requestMetadata.mediaUri.toString())
                        .build()
                }
                return Futures.immediateFuture(updated_media_items)
            }

            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val result = super.onConnect(session, controller)
                val session_commands = result.availableSessionCommands
                    .buildUpon()

                for (command in PlayerServiceCommand.getBaseSessionCommands()) {
                    session_commands.add(command)
                }

                return MediaSession.ConnectionResult.accept(session_commands.build(), result.availablePlayerCommands)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                val command: PlayerServiceCommand? = PlayerServiceCommand.fromSessionCommand(customCommand, args)
                if (command == null) {
                    return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                }

                val result: Bundle = onPlayerServiceCommand(command)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, result))
            }
        })
        .build()
}
