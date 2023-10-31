package com.toasterofbread.spmp.platform.playerservice

import SpMp
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
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
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.toasterofbread.spmp.exovisualiser.ExoVisualizer
import com.toasterofbread.spmp.exovisualiser.FFTAudioProcessor
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.PlayerServiceCommand
import com.toasterofbread.spmp.platform.processMediaDataSpec
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private const val A13_MEDIA_NOTIFICATION_ASPECT = 2.9f / 5.7f

fun getMediaNotificationImageMaxOffset(image: Bitmap): IntOffset {
    val dimensions: IntSize = getMediaNotificationImageSize(image)
    return IntOffset(
        (image.width - dimensions.width) / 2,
        (image.height - dimensions.height) / 2
    )
}

fun getMediaNotificationImageSize(image: Bitmap): IntSize {
    val aspect: Float = if (Build.VERSION.SDK_INT >= 33) A13_MEDIA_NOTIFICATION_ASPECT else 1f
    if (image.width > image.height) {
        return IntSize(
            image.height,
            (image.height * aspect).roundToInt()
        )
    }
    else {
        return IntSize(
            image.width,
            (image.width * aspect).roundToInt()
        )
    }
}

private fun formatMediaNotificationImage(
    image: Bitmap,
    song: Song,
    context: AppContext,
): Bitmap {
    val dimensions: IntSize = getMediaNotificationImageSize(image)
    val offset: IntOffset = song.NotificationImageOffset.get(context.database) ?: IntOffset.Zero

    return Bitmap.createBitmap(
        image,
        (((image.width - dimensions.width) / 2) + offset.x).coerceIn(0, image.width - dimensions.width),
        (((image.height - dimensions.height) / 2) + offset.y).coerceIn(0, image.height - dimensions.height),
        dimensions.width,
        dimensions.height
    )
}

private class PlayerBinder(val service: PlatformPlayerService): Binder()

@androidx.annotation.OptIn(UnstableApi::class)
actual class PlatformPlayerService: MediaSessionService(), PlayerService {
    actual val context: AppContext get() = _context
    private lateinit var _context: AppContext

    private val coroutine_scope = CoroutineScope(Dispatchers.Main)
    private lateinit var player: Player
    private lateinit var media_session: MediaSession

    private var current_song: Song? = null
    private var paused_by_device_disconnect: Boolean = false
    private var device_connection_changed_playing_status: Boolean = false

    private val song_liked_listener = SongLikedStatus.Listener { song, liked_status ->
        if (song == current_song) {
            updatePlayerCustomActions(liked_status)
        }
    }

    private val player_listener = object : Player.Listener {
        override fun onMediaItemTransition(media_item: MediaItem?, reason: Int) {
            val song = media_item?.getSong()
            if (song?.id == current_song?.id) {
                return
            }

            current_song = song
            updatePlayerCustomActions()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (device_connection_changed_playing_status) {
                device_connection_changed_playing_status = false
            }
            else {
                paused_by_device_disconnect = false
            }
        }
    }

    private val audio_device_callback = object : AudioDeviceCallback() {
        private fun isBluetoothAudio(device: AudioDeviceInfo): Boolean {
            if (!device.isSink) {
                return false
            }
            return device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }
        private fun isWiredAudio(device: AudioDeviceInfo): Boolean {
            if (!device.isSink) {
                return false
            }
            return device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.type == AudioDeviceInfo.TYPE_USB_HEADSET)
        }

        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            if (player.isPlaying || !paused_by_device_disconnect) {
                return
            }

            val resume_on_bt: Boolean = Settings.KEY_RESUME_ON_BT_CONNECT.get(context)
            val resume_on_wired: Boolean = Settings.KEY_RESUME_ON_WIRED_CONNECT.get(context)

            for (device in addedDevices) {
                if ((resume_on_bt && isBluetoothAudio(device)) || (resume_on_wired && isWiredAudio(device))) {
                    player.play()
                    break
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            if (!player.isPlaying && player.playbackState == Player.STATE_READY) {
                return
            }

            val pause_on_bt: Boolean = Settings.KEY_PAUSE_ON_BT_DISCONNECT.get(context)
            val pause_on_wired: Boolean = Settings.KEY_PAUSE_ON_WIRED_DISCONNECT.get(context)

            for (device in removedDevices) {
                if ((pause_on_bt && isBluetoothAudio(device)) || (pause_on_wired && isWiredAudio(device))) {
                    device_connection_changed_playing_status = true
                    paused_by_device_disconnect = true
                    player.pause()
                    break
                }
            }
        }
    }

    actual override fun addListener(listener: PlayerListener) {
        listener.addToPlayer(player)
    }
    actual override fun removeListener(listener: PlayerListener) {
        listener.removeFromPlayer(player)
    }

    // If there's a better way to provide information to MediaControllers, I'd like to know
    actual companion object {
        val audio_processor = FFTAudioProcessor()

        private val listeners: MutableList<PlayerListener> = mutableListOf()
        private var player_instance: PlatformPlayerService? by mutableStateOf(null)

        fun setInstance(value: PlatformPlayerService?) {
            player_instance?.also {
                for (listener in listeners) {
                    it.removeListener(listener)
                }
            }

            player_instance = value

            value?.also {
                for (listener in listeners) {
                    it.addListener(listener)
                }
            }
        }

        actual fun addListener(listener: PlayerListener) {
            listeners.add(listener)
            player_instance?.addListener(listener)
        }
        actual fun removeListener(listener: PlayerListener) {
            listeners.remove(listener)
            player_instance?.removeListener(listener)
        }

        private fun AppContext.getAndroidContext(): Context =
            ctx.applicationContext

        actual fun connect(
            context: AppContext,
            instance: PlatformPlayerService?,
            onConnected: (PlatformPlayerService) -> Unit,
            onDisconnected: () -> Unit,
        ): Any {
            val ctx: Context = context.getAndroidContext()

            val service_connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    onConnected((service as PlayerBinder).service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnected()
                }
            }

            ctx.startService(Intent(ctx, PlatformPlayerService::class.java))
            ctx.bindService(Intent(ctx, PlatformPlayerService::class.java), service_connection, Context.BIND_AUTO_CREATE)

            return service_connection
        }

        actual fun disconnect(context: AppContext, connection: Any) {
            context.getAndroidContext().unbindService(connection as ServiceConnection)
        }

        actual fun isServiceRunning(context: AppContext): Boolean {
            val manager: ActivityManager = context.ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (service.service.className == PlatformPlayerService::class.java.name) {
                    return true
                }
            }
            return false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return media_session
    }

    override fun onBind(intent: Intent?): IBinder? {
        try {
            super.onBind(intent)
        }
        catch (_: Throwable) {}

        return PlayerBinder(this)
    }

    actual override fun onCreate() {
        super.onCreate()

        initialiseSessionAndPlayer()
        _context = AppContext(this, coroutine_scope).init()

        _service_player = object : PlayerServicePlayer(this) {
            override fun onUndoStateChanged() {
                for (listener in listeners) {
                    listener.onUndoStateChanged()
                }
            }
        }

        val audio_manager = getSystemService(AUDIO_SERVICE) as AudioManager?
        audio_manager?.registerAudioDeviceCallback(audio_device_callback, null)

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_spmp)
            }
        )

        SongLikedStatus.addListener(song_liked_listener)

        setInstance(this)
    }

    actual override fun onDestroy() {
        coroutine_scope.cancel()
        service_player.release()
        player.release()
        media_session.release()
        SongLikedStatus.removeListener(song_liked_listener)

        val audio_manager = getSystemService(AUDIO_SERVICE) as AudioManager?
        audio_manager?.unregisterAudioDeviceCallback(audio_device_callback)

        clearListener()
        super.onDestroy()
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)

        if (
            (!player.isPlaying && convertState(player.playbackState) != MediaPlayerState.BUFFERING)
            || (
                Settings.KEY_STOP_PLAYER_ON_APP_CLOSE.get(context)
                && intent?.component?.packageName == packageName
            )
        ) {
            stopSelf()
            onDestroy()
        }
    }

    private fun updatePlayerCustomActions(song_liked: SongLikedStatus? = null) {
        coroutine_scope.launch(Dispatchers.Main) {
            val actions: MutableList<CommandButton> = mutableListOf()

            val liked: SongLikedStatus? = song_liked ?: current_song?.Liked?.get(context.database)
            if (liked != null) {
                actions.add(
                    CommandButton.Builder()
                        .setDisplayName(
                            when (liked) {
                                SongLikedStatus.NEUTRAL -> getStringTODO("Like")
                                SongLikedStatus.LIKED -> getStringTODO("Remove like")
                                SongLikedStatus.DISLIKED -> getStringTODO("Remove dislike")
                            }
                        )
                        .setSessionCommand(
                            PlayerServiceCommand.SetLiked(
                                when (liked) {
                                    SongLikedStatus.NEUTRAL -> SongLikedStatus.LIKED
                                    SongLikedStatus.LIKED, SongLikedStatus.DISLIKED -> SongLikedStatus.NEUTRAL
                                }
                            ).getSessionCommand()
                        )
                        .setIconResId(
                            when (liked) {
                                SongLikedStatus.NEUTRAL -> R.drawable.ic_thumb_up_off
                                SongLikedStatus.LIKED -> R.drawable.ic_thumb_up
                                SongLikedStatus.DISLIKED -> R.drawable.ic_thumb_down
                            }
                        )
                        .build()
                )
            }

            media_session.setCustomLayout(actions)
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
                            return 1000
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

        player.addListener(player_listener)
        player.playWhenReady = true
        player.prepare()

        val controller_future: ListenableFuture<MediaController> =
            MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, PlatformPlayerService::class.java))
            ).buildAsync()

        controller_future.addListener(
            { controller_future.get() },
            MoreExecutors.directExecutor()
        )

        media_session = MediaSession.Builder(this, player)
            .setBitmapLoader(object : BitmapLoader {
                val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

                override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
                    throw NotImplementedError()
                }

                override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
                    return executor.submit<Bitmap> {
                        runBlocking {
                            val song = SongRef(uri.toString())
                            var fail_error: Throwable? = null

                            for (quality in MediaItemThumbnailProvider.Quality.byQuality()) {
                                val load_result = MediaItemThumbnailLoader.loadItemThumbnail(song, quality, context)
                                load_result.fold(
                                    { image ->
                                        return@runBlocking formatMediaNotificationImage(
                                            image.asAndroidBitmap(),
                                            song,
                                            context
                                        )
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
                    this,
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
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                    }

                    val result: Bundle = onPlayerServiceCommand(command)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, result))
                }
            })
            .build()
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory({
            DefaultDataSource.Factory(this).createDataSource()
        }) { data_spec: DataSpec ->
            try {
                return@Factory runBlocking {
                    processMediaDataSpec(data_spec, context, context.isConnectionMetered())
                }
            }
            catch (e: Throwable) {
                throw IOException(e)
            }
        }
    }

    private fun onPlayerServiceCommand(command: PlayerServiceCommand): Bundle {
        when (command) {
            is PlayerServiceCommand.SetLiked -> {
                val song = current_song
                if (song != null) {
                    coroutine_scope.launch {
                        val endpoint: SetSongLikedEndpoint? = context.ytapi.user_auth_state?.SetSongLiked
                        if (endpoint?.isImplemented() == true) {
                            song.updateLiked(
                                command.value,
                                endpoint,
                                context
                            )
                        }
                    }
                }
            }
        }

        return Bundle.EMPTY
    }

    private lateinit var _service_player: PlayerServicePlayer
    actual override val service_player: PlayerServicePlayer get() = _service_player
    actual override val state: MediaPlayerState get() = convertState(player.playbackState)
    actual override val is_playing: Boolean get() = player.isPlaying
    actual override val song_count: Int get() = player.mediaItemCount
    actual override val current_song_index: Int get() = player.currentMediaItemIndex
    actual override val current_position_ms: Long get() = player.currentPosition
    actual override val duration_ms: Long get() = player.duration
    actual override val radio_state: RadioInstance.RadioState get() = service_player.radio_state
    actual override var repeat_mode: MediaPlayerRepeatMode
        get() = MediaPlayerRepeatMode.values()[player.repeatMode]
        set(value) {
            player.repeatMode = value.ordinal
        }
    actual override var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value
        }
    actual override val has_focus: Boolean
        get() = TODO()

    actual override fun isPlayingOverRemoteDevice(): Boolean {
        val media_router: MediaRouter = (getSystemService(MEDIA_ROUTER_SERVICE) as MediaRouter?) ?: return false
        val selected_route: MediaRouter.RouteInfo = media_router.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return selected_route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH
        }
        else {
            return false
        }
    }

    actual override fun play() {
        player.play()
    }

    actual override fun pause() {
        player.pause()
    }

    actual override fun playPause() {
        if (player.isPlaying) {
            player.pause()
        }
        else {
            player.play()
        }
    }

    actual override fun seekTo(position_ms: Long) {
        player.seekTo(position_ms)
        listeners.forEach { it.onSeeked(position_ms) }
    }

    actual override fun seekToSong(index: Int) {
        player.seekTo(index, 0)
    }

    actual override fun seekToNext() {
        player.seekToNext()
    }

    actual override fun seekToPrevious() {
        player.seekToPrevious()
    }

    actual override fun getSong(): Song? {
        return player.currentMediaItem?.getSong()
    }

    actual override fun getSong(index: Int): Song? {
        if (index !in 0 until song_count) {
            return null
        }

        return player.getMediaItemAt(index).getSong()
    }

    actual override fun addSong(song: Song, index: Int) {
        player.addMediaItem(index, song.buildExoMediaItem(context))
        listeners.forEach { it.onSongAdded(index, song) }

        service_player.session_started = true
    }

    actual override fun moveSong(from: Int, to: Int) {
        player.moveMediaItem(from, to)
        listeners.forEach { it.onSongMoved(from, to) }
    }

    actual override fun removeSong(index: Int) {
        player.removeMediaItem(index)
        listeners.forEach { it.onSongRemoved(index) }
    }

    @Composable
    actual override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        val visualiser = remember { ExoVisualizer(audio_processor) }
        visualiser.Visualiser(colour, modifier, opacity)
    }
}

@UnstableApi
private fun Song.buildExoMediaItem(context: AppContext): MediaItem =
    MediaItem.Builder()
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(id.toUri()).build())
        .setUri(id)
        .setCustomCacheKey(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .apply {
                    val db = context.database

                    setArtworkUri(id.toUri())
                    setTitle(getActiveTitle(db))
                    setArtist(Artist.get(db)?.getActiveTitle(db))

                    val album = Album.get(db)
                    setAlbumTitle(album?.getActiveTitle(db))
                    setAlbumArtist(album?.Artist?.get(db)?.getActiveTitle(db))
                }
                .build()
        )
        .build()


fun convertState(exo_state: Int): MediaPlayerState {
    return MediaPlayerState.values()[exo_state - 1]
}

fun MediaItem.getSong(): Song {
    return SongRef(mediaMetadata.artworkUri.toString())
}
