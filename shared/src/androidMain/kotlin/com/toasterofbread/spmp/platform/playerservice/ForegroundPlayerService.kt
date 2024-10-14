package com.toasterofbread.spmp.platform.playerservice

import android.content.Intent
import android.media.AudioManager
import android.media.MediaRouter
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MEDIA_STOP
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.getSystemService
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatusListener
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.PlayerServiceCommand
import com.toasterofbread.spmp.platform.visualiser.FFTAudioProcessor
import com.toasterofbread.spmp.platform.visualiser.MusicVisualiser
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import com.toasterofbread.spmp.shared.R
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import dev.toastbits.ytmkt.endpoint.SetSongLikedEndpoint
import dev.toastbits.ytmkt.model.implementedOrNull
import kotlinx.coroutines.*

@androidx.annotation.OptIn(UnstableApi::class)
open class ForegroundPlayerService(
    private val play_when_ready: Boolean,
    private val playlist_auto_progress: Boolean = true
): MediaSessionService(), PlayerService {
    override val load_state: PlayerServiceLoadState = PlayerServiceLoadState(false)
    override val context: AppContext get() = _context
    private lateinit var _context: AppContext
    private var stopped: Boolean = false

    internal val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    internal lateinit var player: ExoPlayer
    internal lateinit var media_session: MediaSession
    internal lateinit var audio_sink: AudioSink
    internal var loudness_enhancer: LoudnessEnhancer? = null

    internal var current_song: Song? = null
    internal var paused_by_device_disconnect: Boolean = false
    internal var device_connection_changed_playing_status: Boolean = false

    private val song_liked_listener: SongLikedStatusListener = SongLikedStatusListener { song, liked_status ->
        if (song == current_song) {
            updatePlayerCustomActions(liked_status)
        }
    }

    private val audio_device_callback: PlayerAudioDeviceCallback = PlayerAudioDeviceCallback(this)

    private val prefs_listener: PlatformPreferencesListener =
        PlatformPreferencesListener { _, key ->
            when (key) {
                context.settings.streaming.ENABLE_AUDIO_NORMALISATION.key -> {
                    coroutine_scope.launch {
                        loudness_enhancer?.update(current_song, context)
                    }
                }
                context.settings.streaming.ENABLE_SILENCE_SKIPPING.key -> {
                    coroutine_scope.launch {
                        audio_sink.skipSilenceEnabled = context.settings.streaming.ENABLE_SILENCE_SKIPPING.get()
                    }
                }
            }
        }

    private val listeners: MutableList<PlayerListener> = mutableListOf()

    override fun addListener(listener: PlayerListener) {
        listener.addToPlayer(player)
        listeners.add(listener)
    }
    override fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
        listener.removeFromPlayer(player)
    }

    protected open fun onRadioCancelled() {}

    protected open fun getNotificationPlayer(player: Player): Player = player

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val keyEvent: KeyEvent? = intent?.extras?.getParcelable(Intent.EXTRA_KEY_EVENT)
        println("onStartCommand $keyEvent $flags $startId $intent")

        // Partial workaround for https://github.com/androidx/media/issues/805
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            && keyEvent?.keyCode == KEYCODE_MEDIA_STOP
//            && flags == 0
        ) {
            Process.killProcess(Process.myPid())
        }

        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return media_session
    }

    override fun onBind(intent: Intent?): IBinder? {
        try {
            val binder = super.onBind(intent)
            if (binder != null) {
                return binder
            }
        }
        catch (_: Throwable) {}

        return PlayerBinder(this)
    }

    override fun onCreate() {
        super.onCreate()

        _context = runBlocking { AppContext.create(this@ForegroundPlayerService, coroutine_scope) }
        _context.getPrefs().addListener(prefs_listener)

        initialiseSessionAndPlayer(
            play_when_ready,
            playlist_auto_progress,
            getNotificationPlayer = { getNotificationPlayer(it) },
            onSongReadyToPlay = {
                listeners.forEach { it.onDurationChanged(player.duration) }
            }
        )

        _service_player = object : PlayerServicePlayer(this) {
            override fun onUndoStateChanged() {
                for (listener in listeners) {
                    listener.onUndoStateChanged()
                }
            }

            override val radio: RadioHandler =
                object : RadioHandler(this, context) {
                    override fun onRadioCancelled() {
                        super.onRadioCancelled()
                        this@ForegroundPlayerService.onRadioCancelled()
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

        SongLikedStatusListener.addListener(song_liked_listener)
    }

    override fun onDestroy() {
        stopped = true
        stopForeground(STOP_FOREGROUND_REMOVE)

        _context.getPrefs().removeListener(prefs_listener)
        coroutine_scope.cancel()
        service_player.release()
        player.release()
        media_session.release()
        loudness_enhancer?.release()
        SongLikedStatusListener.removeListener(song_liked_listener)

        val audio_manager: AudioManager? = getSystemService()
        audio_manager?.unregisterAudioDeviceCallback(audio_device_callback)

        clearListener()
        super.onDestroy()
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, true)
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)

        if (!player.isPlaying && convertState(player.playbackState) != SpMsPlayerState.BUFFERING) {
            stopSelf()
        }
        else if (intent?.component?.packageName == packageName) {
            coroutine_scope.launch {
                if (context.settings.behaviour.STOP_PLAYER_ON_APP_CLOSE.get()) {
                    stopSelf()
                }
            }
        }
    }

    private fun checkAlive() {
        check(!stopped) { "Attempting to use a stopped foreground player service $this. This is a bug." }
    }

    internal fun onPlayerServiceCommand(command: PlayerServiceCommand): Bundle {
        when (command) {
            is PlayerServiceCommand.SetLiked -> {
                val song: Song = current_song ?: return Bundle.EMPTY
                coroutine_scope.launch {
                    val endpoint: SetSongLikedEndpoint =
                        context.ytapi.user_auth_state?.SetSongLiked?.implementedOrNull() ?: return@launch

                    song.updateLiked(
                        command.value,
                        endpoint,
                        context
                    )
                }
            }
        }

        return Bundle.EMPTY
    }

    private lateinit var _service_player: PlayerServicePlayer
    override val service_player: PlayerServicePlayer get() = _service_player
    override val state: SpMsPlayerState get() = convertState(player.playbackState)
    override val is_playing: Boolean get() = player.isPlaying
    override val song_count: Int get() = player.mediaItemCount
    override val current_song_index: Int get() = player.currentMediaItemIndex
    override val current_position_ms: Long get() = player.currentPosition
    override val duration_ms: Long get() = player.duration
    override val radio_instance: RadioInstance get() = service_player.radio_instance
    override var repeat_mode: SpMsPlayerRepeatMode
        get() = SpMsPlayerRepeatMode.entries[player.repeatMode]
        set(value) {
            player.repeatMode = value.ordinal
        }
    override var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value
        }
    override val has_focus: Boolean
        get() = TODO()

    override fun isPlayingOverLatentDevice(): Boolean {
        val media_router: MediaRouter = (getSystemService(MEDIA_ROUTER_SERVICE) as MediaRouter?) ?: return false
        val selected_route: MediaRouter.RouteInfo = media_router.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return selected_route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH
        }
        else {
            return false
        }
    }

    override fun play() {
        checkAlive()
        player.play()
    }

    override fun pause() {
        checkAlive()
        player.pause()
    }

    override fun playPause() {
        checkAlive()
        if (player.isPlaying) {
            player.pause()
        }
        else {
            player.play()
        }
    }

    private val song_seek_undo_stack: MutableList<Pair<Int, Long>> = mutableListOf()
    private fun getSeekPosition(): Pair<Int, Long> = Pair(current_song_index, current_position_ms)

    override fun seekTo(position_ms: Long) {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()
        player.seekTo(position_ms)
        listeners.forEach { it.onSeeked(position_ms) }

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun seekToSong(index: Int) {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()
        player.seekTo(index, 0)

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun seekToNext() {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()
        player.seekToNext()

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun seekToPrevious() {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()
        player.seekToPrevious()

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun undoSeek() {
        checkAlive()

        val (index: Int, position_ms: Long) = song_seek_undo_stack.removeLastOrNull() ?: return

        if (index != current_song_index) {
            player.seekTo(index, position_ms)
        }
        else {
            player.seekTo(position_ms)
        }
    }

    override fun getSong(): Song? {
        return player.currentMediaItem?.getSong()
    }

    override fun getSong(index: Int): Song? {
        if (index !in 0 until song_count) {
            return null
        }

        return player.getMediaItemAt(index).getSong()
    }

    override fun addSong(song: Song, index: Int) {
        checkAlive()

        player.addMediaItem(index, song.buildExoMediaItem(context))
        listeners.forEach { it.onSongAdded(index, song) }

        service_player.session_started = true
    }

    override fun moveSong(from: Int, to: Int) {
        checkAlive()

        player.moveMediaItem(from, to)
        listeners.forEach { it.onSongMoved(from, to) }
    }

    override fun removeSong(index: Int) {
        checkAlive()

        val song: Song = player.getMediaItemAt(index).getSong()
        player.removeMediaItem(index)
        listeners.forEach { it.onSongRemoved(index, song) }
    }

    @Composable
    override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        val visualiser: MusicVisualiser = remember { MusicVisualiser(fft_audio_processor) }
        visualiser.Visualiser(colour, modifier, opacity)
    }

    companion object {
        // If there's a better way to provide information to MediaControllers, I'd like to know
        val fft_audio_processor: FFTAudioProcessor = FFTAudioProcessor()
    }
}
