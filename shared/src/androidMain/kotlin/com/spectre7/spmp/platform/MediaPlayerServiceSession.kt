package com.spectre7.spmp.platform

import GlobalPlayerState
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.*
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.spectre7.spmp.exovisualiser.FFTAudioProcessor
import com.spectre7.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.model.mediaitem.SongLikeStatus
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors

private const val NOTIFICATION_ID = 2
private const val NOTIFICATION_CHANNEL_ID = "playback_channel"

private const val COMMAND_SET_LIKE_TRUE = "com.spectre7.spmp.setliketrue"
private const val COMMAND_SET_LIKE_NEUTRAL = "com.spectre7.spmp.setlikeneutral"

@UnstableApi
class MediaPlayerServiceSession: MediaSessionService() {
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)
    private lateinit var player: ExoPlayer
    private lateinit var media_session: MediaSession
    private lateinit var notification_builder: NotificationCompat.Builder
    private var current_song: Song? = null

    private inner class PlayerListener: Player.Listener {
        private fun onSongLikeStatusChanged(status: SongLikeStatus?) {
            if (status?.loading == false) {
                coroutine_scope.launch {
                    updatePlayerCustomActions(status.status)
                }
            }
        }

        override fun onMediaItemTransition(media_item: MediaItem?, reason: Int) {
            val song = media_item?.getSong()
            if (song == current_song) {
                return
            }

            current_song?.like_status?.listeners?.remove(::onSongLikeStatusChanged)

            song?.like_status?.listeners?.add(::onSongLikeStatusChanged)
            current_song = song

            onSongLikeStatusChanged(song?.like_status)
        }
    }

    companion object {
        // If there's a better way to provide this to MediaControllers, I'd like to know
        val audio_processor = FFTAudioProcessor()
    }

    override fun onCreate() {
        super.onCreate()

        initialiseSessionAndPlayer()
        createNotificationChannel()
        updatePlayerCustomActions(null)

        notification_builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(PlatformContext.ic_spmp)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(media_session))
            .setContentIntent(PendingIntent.getActivity(
                this,
                1,
                packageManager.getLaunchIntentForPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE
            ))

        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                synchronized(coroutine_scope) {
                    coroutine_scope.coroutineContext.cancelChildren()
                    coroutine_scope.launch {
                        updatePlayerNotification()
                    }
                }
                return MediaNotification(NOTIFICATION_ID, notification_builder.build())
            }

            override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean {
                TODO("Action $action")
            }
        })
    }

    override fun onDestroy() {
        coroutine_scope.cancel()
        player.release()
        media_session.release()
        clearListener()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    private fun updatePlayerCustomActions(like_status: SongLikeStatus.Status?) {
        val like_action: CommandButton? =
            if (like_status != null && like_status.is_available)
                CommandButton.Builder()
                    .setDisplayName("TODO")
                    .setSessionCommand(SessionCommand(
                        if (like_status == SongLikeStatus.Status.NEUTRAL) COMMAND_SET_LIKE_TRUE else COMMAND_SET_LIKE_NEUTRAL,
                        Bundle.EMPTY
                    ))
                    .setIconResId(if (like_status == SongLikeStatus.Status.LIKED) PlatformContext.ic_thumb_up else PlatformContext.ic_thumb_up_off)
                    .build()
            else null

        media_session.setCustomLayout(listOfNotNull(
            like_action
        ))
    }

    private suspend fun updatePlayerNotification() {
        val song = media_session.player.currentMediaItem?.getSong()

        val large_icon: Bitmap? =
            if (song != null)
                getCurrentLargeIcon(song)
            else null

        notification_builder.apply {
            if (Build.VERSION.SDK_INT < 33) {
                setLargeIcon(large_icon)
            }
            setContentTitle(song?.title ?: "")
            setContentText(song?.artist?.title ?: "")
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification_builder.build())
    }

    private suspend fun getCurrentLargeIcon(song: Song): Bitmap? {
        fun getCroppedThumbnail(image: Bitmap?): Bitmap? {
            if (image == null) {
                return null
            }
            return Bitmap.createBitmap(image, (image.width - image.height) / 2, 0, image.height, image.height)
        }

        try {
            return getCroppedThumbnail(song.loadThumbnail(MediaItemThumbnailProvider.Quality.HIGH)?.asAndroidBitmap())
        }
        catch (e: IndexOutOfBoundsException) {
            return null
        }
    }

    private fun initialiseSessionAndPlayer() {
        val renderers_factory = RenderersFactory { handler: Handler?, _, audioListener: AudioRendererEventListener?, _, _ ->
            arrayOf(
                MediaCodecAudioRenderer(
                    this,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES,
                    audio_processor
                )
            )
        }

        player = ExoPlayer.Builder(
            this,
            renderers_factory,
            DefaultMediaSourceFactory(
                createDataSourceFactory()
            ) { arrayOf(MatroskaExtractor(), FragmentedMp4Extractor()) }
        )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setUsePlatformDiagnostics(false)
            .build()
            .apply {
                addListener(PlayerListener())
                playWhenReady = true
                prepare()
            }

        media_session = MediaSession.Builder(this, player)
            .setBitmapLoader(object : BitmapLoader {
                val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

                override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
                    throw NotImplementedError()
                }

                override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
                    val song = Song.fromId(uri.toString())
                    return executor.submit<Bitmap> {
                        runBlocking {
                            song.loadThumbnail(MediaItemThumbnailProvider.Quality.HIGH)!!.asAndroidBitmap()
                        }
                    }
                }
            })
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    media_session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    media_items: List<MediaItem>
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
                        .add(SessionCommand(COMMAND_SET_LIKE_TRUE, Bundle()))
                        .add(SessionCommand(COMMAND_SET_LIKE_NEUTRAL, Bundle()))
                    return MediaSession.ConnectionResult.accept(session_commands.build(), result.availablePlayerCommands)
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        COMMAND_SET_LIKE_TRUE -> current_song?.like_status?.setLiked(true)
                        COMMAND_SET_LIKE_NEUTRAL -> current_song?.like_status?.setLiked(null)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory({
            DefaultDataSource.Factory(this).createDataSource()
        }) { data_spec: DataSpec ->

            val song = Song.fromId(data_spec.uri.toString())

            val download_manager = GlobalPlayerState.download_manager
            var local_file: File? = download_manager.getSongLocalFile(song)
            if (local_file != null) {
                println("Playing song ${song.title} from local file $local_file")
                return@Factory data_spec.withUri(Uri.fromFile(local_file))
            }

            if (
                song.registry_entry.getPlayCount(ChronoUnit.WEEKS) >= com.spectre7.spmp.model.Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.get<Int>(this)
                && (com.spectre7.spmp.model.Settings.KEY_AUTO_DOWNLOAD_ON_METERED.get(this) || !isConnectionMetered())
            ) {
                var done = false
                runBlocking {
                    download_manager.getDownload(song) { initial_status ->
                        when (initial_status?.status) {
                            PlayerDownloadManager.DownloadStatus.Status.DOWNLOADING -> {
                                val listener = object : PlayerDownloadManager.DownloadStatusListener() {
                                    override fun onDownloadChanged(status: PlayerDownloadManager.DownloadStatus) {
                                        if (status.song != song) {
                                            return
                                        }

                                        when (status.status) {
                                            PlayerDownloadManager.DownloadStatus.Status.IDLE, PlayerDownloadManager.DownloadStatus.Status.DOWNLOADING -> return
                                            PlayerDownloadManager.DownloadStatus.Status.PAUSED -> throw IllegalStateException()
                                            PlayerDownloadManager.DownloadStatus.Status.CANCELLED -> {
                                                done = true
                                            }
                                            PlayerDownloadManager.DownloadStatus.Status.FINISHED, PlayerDownloadManager.DownloadStatus.Status.ALREADY_FINISHED -> {
                                                local_file = download_manager.getSongLocalFile(song)
                                                done = true
                                            }
                                        }

                                        download_manager.removeDownloadStatusListener(this)
                                    }
                                }
                                download_manager.addDownloadStatusListener(listener)
                            }
                            PlayerDownloadManager.DownloadStatus.Status.IDLE, PlayerDownloadManager.DownloadStatus.Status.CANCELLED, PlayerDownloadManager.DownloadStatus.Status.PAUSED, null -> {
                                download_manager.startDownload(song.id, true) { status ->
                                    local_file = status.file
                                    done = true
                                }
                            }
                            PlayerDownloadManager.DownloadStatus.Status.ALREADY_FINISHED, PlayerDownloadManager.DownloadStatus.Status.FINISHED -> throw IllegalStateException()
                        }
                    }

                    var elapsed = 0
                    while (!done && elapsed < AUTO_DOWNLOAD_SOFT_TIMEOUT) {
                        delay(100)
                        elapsed += 100
                    }
                }

                if (local_file != null) {
                    println("Playing song ${song.title} from local file $local_file")
                    return@Factory data_spec.withUri(Uri.fromFile(local_file))
                }
            }

            val format = song.getStreamFormat()
            if (format.isFailure) {
                throw IOException(format.exceptionOrNull()!!)
            }

            return@Factory if (local_file != null) {
                println("Playing song ${song.title} from local file $local_file")
                data_spec.withUri(Uri.fromFile(local_file))
            } else {
                println("Playing song ${song.title} from external format ${format.getOrThrow()}")
                data_spec.withUri(Uri.parse(format.getOrThrow().stream_url))
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            return
        }

        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "TODO",
                NotificationManager.IMPORTANCE_LOW
            )
            .apply {
                enableVibration(false)
            }
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return media_session
    }
}