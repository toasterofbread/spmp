@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.platform.playerservice

import SpMp
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import app.cash.sqldelight.Query
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.toasterofbread.spmp.exovisualiser.FFTAudioProcessor
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.platform.MediaPlayerRepeatMode
import com.toasterofbread.spmp.platform.MediaPlayerState
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlayerServiceCommand
import com.toasterofbread.spmp.platform.buildExoMediaItem
import com.toasterofbread.spmp.platform.convertState
import com.toasterofbread.spmp.platform.getSong
import com.toasterofbread.spmp.platform.isConnectionMetered
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
    context: PlatformContext,
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

fun PlayerServiceState.toBundle(): Bundle =
    bundleOf(
        "stop_after_current_song" to stop_after_current_song
    )

fun PlayerServiceState.Companion.fromBundle(bundle: Bundle): PlayerServiceState =
    PlayerServiceState(
        stop_after_current_song = bundle.getBoolean("stop_after_current_song")
    )

@UnstableApi
actual abstract class PlatformPlayerService: MediaSessionService() {
    actual abstract fun savePersistentQueue()

    private var _state: MutableState<PlayerServiceState> = mutableStateOf(PlayerServiceState())
    actual var service_state: PlayerServiceState
        get() = _state.value
        set(value) {
            if (value == _state.value) {
                return
            }
            _state.value = value

            media_session.setSessionExtras(value.toBundle())
        }

    actual val context: PlatformContext get() = _context
    private lateinit var _context: PlatformContext

    private val coroutine_scope = CoroutineScope(Dispatchers.Main)
    private lateinit var player: Player
    private lateinit var media_session: MediaSession

    private var current_song: Song? = null
    private var paused_by_device_disconnect: Boolean = false
    private var device_connection_changed_playing_status: Boolean = false

    private val player_listener = object : Player.Listener {
        val song_liked_listener = Query.Listener {
            updatePlayerCustomActions()
        }

        override fun onMediaItemTransition(media_item: MediaItem?, reason: Int) {
            val song = media_item?.getSong()
            if (song?.id == current_song?.id) {
                return
            }

            // Listeners don't seem to work between database instances
            with(SpMp.context.database.songQueries) {
                current_song?.also {
                    likedById(it.id).removeListener(song_liked_listener)
                }
                song?.also {
                    likedById(it.id).addListener(song_liked_listener)
                }
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
            if (!device.isSink) return false
            return device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }
        private fun isWiredAudio(device: AudioDeviceInfo): Boolean {
            if (!device.isSink) return false
            return device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.type == AudioDeviceInfo.TYPE_USB_HEADSET)
        }

        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            if (player.isPlaying || !paused_by_device_disconnect) {
                return
            }

            val resume_on_bt: Boolean = Settings.KEY_RESUME_ON_BT_CONNECT.get()
            val resume_on_wired: Boolean = Settings.KEY_RESUME_ON_WIRED_CONNECT.get()

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

            val pause_on_bt: Boolean = Settings.KEY_PAUSE_ON_BT_DISCONNECT.get()
            val pause_on_wired: Boolean = Settings.KEY_PAUSE_ON_WIRED_DISCONNECT.get()

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

    actual open class Listener {
        actual open fun onSongTransition(song: Song?, manual: Boolean) {}
        actual open fun onStateChanged(state: MediaPlayerState) {}
        actual open fun onPlayingChanged(is_playing: Boolean) {}
        actual open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {}
        actual open fun onVolumeChanged(volume: Float) {}
        actual open fun onDurationChanged(duration_ms: Long) {}
        actual open fun onSeeked(position_ms: Long) {}
        actual open fun onUndoStateChanged() {}

        actual open fun onSongAdded(index: Int, song: Song) {}
        actual open fun onSongRemoved(index: Int) {}
        actual open fun onSongMoved(from: Int, to: Int) {}

        actual open fun onEvents() {}

        private val player_listener = object : Player.Listener {
            var current_song: Song? = null
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                val song = item?.getSong()
                if (song?.id == current_song?.id) {
                    return
                }
                current_song = song
                onSongTransition(song, reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
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
            player.addListener(player_listener)
        }
        internal fun removeFromPlayer(player: Player) {
            player.removeListener(player_listener)
        }
    }

    companion object {
        // If there's a better way to provide this to MediaControllers, I'd like to know
        val audio_processor = FFTAudioProcessor()
    }

    actual override fun onCreate() {
        super.onCreate()

        _context = PlatformContext(this, coroutine_scope).init()

        initialiseSessionAndPlayer()

        val audio_manager = getSystemService(AUDIO_SERVICE) as AudioManager?
        audio_manager?.registerAudioDeviceCallback(audio_device_callback, null)

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_spmp)
            }
        )
    }

    actual override fun onDestroy() {
        coroutine_scope.cancel()
        player.release()
        media_session.release()

        val audio_manager = getSystemService(AUDIO_SERVICE) as AudioManager?
        audio_manager?.unregisterAudioDeviceCallback(audio_device_callback)

        clearListener()
        super.onDestroy()
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)

        val intent_package_name: String = intent?.component?.packageName ?: return
        if (intent_package_name == packageName && Settings.KEY_STOP_PLAYER_ON_APP_CLOSE.get(context)) {
            stopSelf()
        }
    }

    private fun updatePlayerCustomActions() {
        coroutine_scope.launch(Dispatchers.Main) {
            val actions: MutableList<CommandButton> = mutableListOf()

            val liked: SongLikedStatus? = current_song?.Liked?.get(context.database)
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
                            PlayerServiceCommand.SetLiked(liked).getSessionCommand()
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
            .setUsePlatformDiagnostics(false)
            .build()

        player.addListener(player_listener)
        player.playWhenReady = true
        player.prepare()

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
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    val command: PlayerServiceCommand? = PlayerServiceCommand.fromSessionCommand(customCommand)
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
                return@Factory processMediaDataSpec(data_spec, context, isConnectionMetered())
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
                        val endpoint: SetSongLikedEndpoint? = SpMp.context.ytapi.user_auth_state?.SetSongLiked
                        if (endpoint?.isImplemented() == true) {
                            song.updateLiked(
                                command.value,
                                endpoint,
                                SpMp.context
                            )
                        }
                    }
                }
            }
            PlayerServiceCommand.SavePersistentQueue -> {
                savePersistentQueue()
            }
            is PlayerServiceCommand.SetStopAfterCurrentSong -> {
                service_state = service_state.copy(stop_after_current_song = command.value)
            }
        }

        return Bundle.EMPTY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return media_session
    }

    actual val service_player: PlayerServicePlayer = PlayerServicePlayer(this)
    actual val state: MediaPlayerState get() = convertState(player.playbackState)
    actual val is_playing: Boolean get() = player.isPlaying
    actual val song_count: Int get() = player.mediaItemCount
    actual val current_song_index: Int get() = player.currentMediaItemIndex
    actual val current_position_ms: Long get() = player.currentPosition
    actual val duration_ms: Long get() = player.duration
    actual val radio: RadioInstance.RadioState get() = service_state.radio_state
    actual var repeat_mode: MediaPlayerRepeatMode
        get() = MediaPlayerRepeatMode.values()[player.repeatMode]
        set(value) {
            player.repeatMode = value.ordinal
        }
    actual var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value
        }
    actual val has_focus: Boolean
        get() = TODO()

    actual open fun play() {
        player.play()
    }

    actual open fun pause() {
        player.pause()
    }

    actual open fun playPause() {
        if (player.isPlaying) {
            player.pause()
        }
        else {
            player.play()
        }
    }

    actual open fun seekTo(position_ms: Long) {
        player.seekTo(position_ms)
    }

    actual open fun seekToSong(index: Int) {
        player.seekTo(index, 0)
    }

    actual open fun seekToNext() {
        player.seekToNext()
    }

    actual open fun seekToPrevious() {
        player.seekToPrevious()
    }

    actual fun getSong(): Song? {
        return player.currentMediaItem?.getSong()
    }

    actual fun getSong(index: Int): Song? {
        if (index !in 0 until song_count) {
            return null
        }

        return player.getMediaItemAt(index).getSong()
    }

    actual fun addSong(song: Song) {
        addSong(song, song_count)
    }

    actual fun addSong(song: Song, index: Int) {
        player.addMediaItem(index, song.buildExoMediaItem(context))
    }

    actual fun moveSong(from: Int, to: Int) {
        player.moveMediaItem(from, to)
    }

    actual fun removeSong(index: Int) {
        player.removeMediaItem(index)
    }

    actual fun addListener(listener: Listener) {
        listener.addToPlayer(player)
    }

    actual fun removeListener(listener: Listener) {
        listener.removeFromPlayer(player)
    }
}
