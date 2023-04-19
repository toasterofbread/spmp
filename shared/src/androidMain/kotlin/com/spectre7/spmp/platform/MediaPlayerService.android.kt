package com.spectre7.spmp.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.spectre7.spmp.PlayerDownloadManager
import com.spectre7.spmp.PlayerDownloadService.DownloadStatus
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.utils.getString
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.google.android.exoplayer2.upstream.cache.Cache as ExoCache

private fun convertState(exo_state: Int): MediaPlayerState {
    return MediaPlayerState.values()[exo_state - 1]
}

actual open class MediaPlayerService: PlatformService() {

    actual open class Listener {
        actual open fun onSongTransition(song: Song?) {}
        actual open fun onStateChanged(state: MediaPlayerState) {}
        actual open fun onPlayingChanged(is_playing: Boolean) {}
        actual open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {}
        actual open fun onVolumeChanged(volume: Float) {}
        actual open fun onSeeked(position_ms: Long) {}
        
        actual open fun onSongAdded(index: Int, song: Song?) {}
        actual open fun onSongRemoved(index: Int) {}
        actual open fun onSongMoved(from: Int, to: Int) {}

        actual open fun onEvents() {}

        private val listener = object : Player.Listener {
            override fun onMediaItemTransition(item: ExoMediaItem?, reason: Int) {
                onSongTransition(item?.getSong())
            }
            override fun onPlaybackStateChanged(state: Int) {
                onStateChanged(convertState(state))
            }
            override fun onIsPlayingChanged(is_playing: Boolean) {
                onPlayingChanged(is_playing)
            }
            override fun onRepeatModeChanged(repeat_mode: Int) {
                onRepeatModeChanged(MediaPlayerRepeatMode.values()[repeat_mode])
            }
            override fun onVolumeChanged(volume: Float) {
                this@Listener.onVolumeChanged(volume)
            }
            override fun onEvents(player: Player, events: Player.Events) {
                onEvents()
            }
        }

        internal fun addToPlayer(player: Player) {
            player.addListener(listener)
        }
        internal fun removeFromPlayer(player: Player) {
            player.removeListener(listener)
        }
    }

    private var player: ExoPlayer? = null
    private var cache: ExoCache? = null
    private var media_session: MediaSessionCompat? = null
    private var media_session_connector: MediaSessionConnector? = null
    private val listeners: MutableList<Listener> = mutableListOf()

    private val NOTIFICATION_ID = 2
    private val NOTIFICATION_CHANNEL_ID = "playback_channel"
    private var notification_manager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()

        val audio_sink = DefaultAudioSink.Builder()
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    emptyArray(),
                    SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                    SonicAudioProcessor()
                )
            )
            .build()

        val renderers_factory = RenderersFactory { handler: Handler?, _, audioListener: AudioRendererEventListener?, _, _ ->
            arrayOf(
                MediaCodecAudioRenderer(
                    this,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    audio_sink
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
                playWhenReady = false
                prepare()
            }

        cache = SimpleCache(File(context.getCacheDir(), "exoplayer"), LeastRecentlyUsedCacheEvictor(1), StandaloneDatabaseProvider(this))

        media_session = MediaSessionCompat(this, "spmp")
        media_session_connector = MediaSessionConnector(media_session!!)
        media_session_connector!!.setPlayer(player)
        media_session!!.setMediaButtonReceiver(null)
        media_session!!.isActive = true

        media_session!!.setCallback(object: MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(event_intent: Intent?): Boolean {

                val event = event_intent?.extras?.get("android.intent.extra.KEY_EVENT") as KeyEvent?
                if (event == null || event.action != KeyEvent.ACTION_DOWN) {
                    return super.onMediaButtonEvent(event_intent)
                }

                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        playPause()
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        play()
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        pause()
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        seekToNext()
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        seekToPrevious()
                    }
                    else -> {
                        println("Unhandled media event: ${event.keyCode}")
                        return super.onMediaButtonEvent(event_intent)
                    }
                }

                return true
            }
        })
    }

    override fun onDestroy() {
        notification_manager?.setPlayer(null)
        notification_manager = null

        media_session?.release()
        media_session = null
        player?.release()
        player = null
        cache?.release()
        cache = null

        super.onDestroy()
    }

    actual var session_started: Boolean by mutableStateOf(false)

    actual val state: MediaPlayerState get() = convertState(player!!.playbackState)
    actual val is_playing: Boolean get() = player!!.isPlaying
    actual val song_count: Int get() = player!!.mediaItemCount
    actual val current_song_index: Int get() = player!!.currentMediaItemIndex
    actual val current_position_ms: Long get() = player!!.currentPosition
    actual val duration_ms: Long get() = player!!.duration

    actual var repeat_mode: MediaPlayerRepeatMode
        get() = MediaPlayerRepeatMode.values()[player!!.repeatMode]
        set(value) { player!!.repeatMode = value.ordinal }
    actual var volume: Float
        get() = player!!.volume
        set(value) { player!!.volume = value }

    actual val has_focus: Boolean get() = player!!.audioFocusState == Player.AUDIO_FOCUS_STATE_HAVE_FOCUS

    actual open fun play() {
        if (state == MediaPlayerState.ENDED) {
            seekTo(0)
        }
        player!!.play()
    }
    actual open fun pause() = player!!.pause()
    actual open fun playPause() {
        if (is_playing) pause()
        else play()
    }

    actual open fun seekTo(position_ms: Long) {
        player!!.seekTo(position_ms)
        listeners.forEach { it.onSeeked(position_ms) }
    }
    actual open fun seekTo(index: Int, position_ms: Long) {
        player!!.seekTo(index, position_ms)
        listeners.forEach { it.onSeeked(position_ms) }
    }
    actual open fun seekToNext() = player!!.seekToNextMediaItem()
    actual open fun seekToPrevious() = player!!.seekToPreviousMediaItem()

    actual fun getSong(): Song? = getSong(current_song_index)
    actual fun getSong(index: Int): Song? {
        if (index < 0 || index >= song_count) {
            return null
        }
        return player!!.getMediaItemAt(index).getSong()
    }

    actual fun addSong(song: Song) {
        addSong(song, song_count)
    }
    actual fun addSong(song: Song, index: Int) {
        val target_index = index.coerceIn(0, song_count)
        val item = ExoMediaItem.Builder().setTag(song).setUri(song.id).setCustomCacheKey(song.id).build()
        player!!.addMediaItem(index, item)
        addNotificationToPlayer()
        listeners.forEach { onSongAdded(song, target_index) }
    }
    actual fun moveSong(from: Int, to: Int) {
        require(from in 0 until song_count)
        require(to in 0 until song_count)
        player!!.moveMediaItem(from, to)
        listeners.forEach { onSongMoved(from, to) }
    }
    actual fun removeSong(index: Int) {
        player!!.removeMediaItem(index)
        listeners.forEach { onSongRemoved(index) }
    }

    actual fun addListener(listener: Listener) {
        listeners.add(listener)
        listener.addToPlayer(player!!)
    }
    actual fun removeListener(listener: Listener) {
        listener.removeFromPlayer(player!!)
        listeners.remove(listener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(media_session, intent)
        return START_NOT_STICKY
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory({
            DefaultDataSource.Factory(this).createDataSource()
        }) { data_spec: DataSpec ->

            val song = Song.fromId(data_spec.uri.toString())

            val download_manager = PlayerServiceHost.download_manager
            var local_file: File? = download_manager.getSongLocalFile(song)
            if (local_file != null) {
                return@Factory data_spec.withUri(Uri.fromFile(local_file))
            }

            if (
                song.registry_entry.play_count >= Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.get<Int>(this)
                && (Settings.KEY_AUTO_DOWNLOAD_ON_METERED.get(this) || !isConnectionMetered())
            ) {
                var done = false
                runBlocking {

                    download_manager.getSongDownloadStatus(song.id) { initial_status ->

                        when (initial_status) {
                            DownloadStatus.DOWNLOADING -> {
                                val listener = object : PlayerDownloadManager.DownloadStatusListener() {
                                    override fun onSongDownloadStatusChanged(song_id: String, status: DownloadStatus) {
                                        if (song_id != song.id) {
                                            return
                                        }

                                        when (status) {
                                            DownloadStatus.IDLE, DownloadStatus.DOWNLOADING -> return
                                            DownloadStatus.PAUSED -> throw IllegalStateException()
                                            DownloadStatus.CANCELLED -> {
                                                done = true
                                            }
                                            DownloadStatus.FINISHED, DownloadStatus.ALREADY_FINISHED -> {
                                                local_file = download_manager.getSongLocalFile(song)
                                                done = true
                                            }
                                        }

                                        download_manager.removeDownloadStatusListener(this)
                                    }
                                }
                                download_manager.addDownloadStatusListener(listener)
                            }
                            DownloadStatus.IDLE, DownloadStatus.CANCELLED, DownloadStatus.PAUSED -> {
                                download_manager.startDownload(song.id, true) { completed_file, _ ->
                                    local_file = completed_file
                                    done = true
                                }
                            }
                            DownloadStatus.ALREADY_FINISHED, DownloadStatus.FINISHED -> throw IllegalStateException()
                        }

                    }

                    var elapsed = 0
                    while (!done && elapsed < AUTO_DOWNLOAD_SOFT_TIMEOUT) {
                        delay(100)
                        elapsed += 100
                    }
                }

                if (local_file != null) {
                    return@Factory data_spec.withUri(Uri.fromFile(local_file))
                }
            }

            val format = song.getStreamFormat()
            if (format.isFailure) {
                throw IOException(format.exceptionOrNull()!!)
            }

            return@Factory if (local_file != null) {
                data_spec.withUri(Uri.fromFile(local_file))
            }
            else {
                data_spec.withUri(Uri.parse(format.getOrThrow().stream_url))
            }
        }
    }

    private fun addNotificationToPlayer() {
        if (notification_manager != null) {
            return
        }
        notification_manager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            getNotificationChannel(),
            object : PlayerNotificationManager.MediaDescriptionAdapter {

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    return PendingIntent.getActivity(
                        this@MediaPlayerService,
                        1,
                        Intent(this@MediaPlayerService, PlatformContext.main_activity),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }

                override fun getCurrentContentText(player: Player): String? {
                    return getSong()?.artist?.title
                }

                override fun getCurrentContentTitle(player: Player): String {
                    return getSong()?.title ?: "NULL"
                }

                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                    fun getCroppedThumbnail(image: Bitmap?): Bitmap? {
                        if (image == null) {
                            return null
                        }

                        if (Build.VERSION.SDK_INT >= 33) {
                            val metadata_builder = MediaMetadataCompat.Builder()
                            metadata_builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)
                            media_session!!.setMetadata(metadata_builder.build())
                            return image
                        }
                        else {
                            return Bitmap.createBitmap(image, (image.width - image.height) / 2, 0, image.height, image.height)
                        }
                    }

                    try {
                        val song = getSong() ?: return null
                        if (song.isThumbnailLoaded(MediaItem.ThumbnailQuality.HIGH)) {
                            return getCroppedThumbnail(song.loadThumbnail(MediaItem.ThumbnailQuality.HIGH)?.asAndroidBitmap())
                        }

                        thread {
                            val cropped = getCroppedThumbnail(song.loadThumbnail(MediaItem.ThumbnailQuality.HIGH)?.asAndroidBitmap())
                            if (cropped != null) {
                                callback.onBitmap(cropped)
                            }
                        }

                        return null
                    }
                    catch (e: IndexOutOfBoundsException) {
                        return null
                    }
                }

            }
        ).setNotificationListener(
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(notificationId: Int,
                                                  notification: Notification,
                                                  ongoing: Boolean) {
                    super.onNotificationPosted(notificationId, notification, ongoing)
//                            if (!ongoing) {
//                                stopForeground(false)
//                            } else {
                    startForeground(notificationId, notification)
//                            }

                }
                override fun onNotificationCancelled(notificationId: Int,
                                                     dismissedByUser: Boolean) {
                    super.onNotificationCancelled(notificationId, dismissedByUser)
                    stopSelf()
                }
            }
        )
            // .setCustomActionReceiver(
            //     object : PlayerNotificationManager.CustomActionReceiver {
            //         override fun createCustomActions(
            //             context: ProjectContext,
            //             instanceId: Int
            //         ): MutableMap<String, NotificationCompat.Action> {
            //             val pendingIntent = PendingIntent.getService(
            //                 context,
            //                 1,
            //                 Intent(context, PlayerService::class.java).putExtra("action", SERVICE_INTENT_ACTIONS.STOP),
            //                 PendingIntent.FLAG_IMMUTABLE
            //             )
            //             return mutableMapOf(
            //                 Pair("Play", NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "namae", pendingIntent))
            //             )
            //         }

            //         override fun getCustomActions(player: Player): MutableList<String> {
            //             return mutableListOf("Play")
            //         }

            //         override fun onCustomAction(
            //             player: Player,
            //             action: String,
            //             intent: Intent
            //         ) {
            //             println(action)
            //         }

            //     }
            // )
            .build()

        notification_manager?.setUseFastForwardAction(false)
        notification_manager?.setUseRewindAction(false)

        notification_manager?.setUseNextActionInCompactView(true)
        notification_manager?.setUsePreviousActionInCompactView(true)

        notification_manager?.setPlayer(player)
        notification_manager?.setMediaSessionToken(media_session!!.sessionToken)

        session_started = true
    }

    private fun getNotificationChannel(): String{
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString("player_service_name"),
            NotificationManager.IMPORTANCE_NONE
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        return NOTIFICATION_CHANNEL_ID
    }

    actual companion object {
        actual fun CoroutineScope.playerLaunch(action: CoroutineScope.() -> Unit) {
            launch(Dispatchers.Main, block = action)
        }
    }
}

fun ExoMediaItem.getSong(): Song? {
    return when (val tag = localConfiguration!!.tag) {
        is IndexedValue<*> -> tag.value as Song?
        is Song? -> tag
        else -> throw IllegalStateException()
    }
}
