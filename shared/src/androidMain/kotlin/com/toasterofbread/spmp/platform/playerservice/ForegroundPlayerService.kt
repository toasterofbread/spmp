package com.toasterofbread.spmp.platform.playerservice

import android.content.Intent
import android.media.AudioManager
import android.media.MediaRouter
import android.media.audiofx.LoudnessEnhancer
import android.os.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.*
import androidx.media3.session.*
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.spmp.exovisualiser.ExoVisualizer
import com.toasterofbread.spmp.exovisualiser.FFTAudioProcessor
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.PlayerServiceCommand
import com.toasterofbread.spmp.platform.playerservice.*
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.implementedOrNull
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.*
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState

@androidx.annotation.OptIn(UnstableApi::class)
open class ForegroundPlayerService: MediaSessionService(), PlayerService {
    override val load_state: PlayerServiceLoadState = PlayerServiceLoadState(false)
    override val context: AppContext get() = _context
    private lateinit var _context: AppContext

    internal val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    internal lateinit var player: ExoPlayer
    internal lateinit var media_session: MediaSession
    internal lateinit var audio_sink: AudioSink
    internal var loudness_enhancer: LoudnessEnhancer? = null

    internal var current_song: Song? = null
    internal var paused_by_device_disconnect: Boolean = false
    internal var device_connection_changed_playing_status: Boolean = false

    private val song_liked_listener: SongLikedStatus.Listener = SongLikedStatus.Listener { song, liked_status ->
        if (song == current_song) {
            updatePlayerCustomActions(liked_status)
        }
    }

    private val audio_device_callback: PlayerAudioDeviceCallback = PlayerAudioDeviceCallback(this)

    private val prefs_listener: PlatformPreferencesListener = object : PlatformPreferencesListener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
                StreamingSettings.Key.ENABLE_AUDIO_NORMALISATION.getName() -> {
                    loudness_enhancer?.update(current_song, context)
                }
                StreamingSettings.Key.ENABLE_SILENCE_SKIPPING.getName() -> {
                    audio_sink.skipSilenceEnabled = StreamingSettings.Key.ENABLE_SILENCE_SKIPPING.get(context)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
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

        _context = AppContext(this, coroutine_scope).init()
        _context.getPrefs().addListener(prefs_listener)

        initialiseSessionAndPlayer()

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
    }

    override fun onDestroy() {
        _context.getPrefs().removeListener(prefs_listener)
        coroutine_scope.cancel()
        service_player.release()
        player.release()
        media_session.release()
        loudness_enhancer?.release()
        SongLikedStatus.removeListener(song_liked_listener)

        val audio_manager = getSystemService(AUDIO_SERVICE) as AudioManager?
        audio_manager?.unregisterAudioDeviceCallback(audio_device_callback)

        clearListener()
        super.onDestroy()
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)

        if (
            (!player.isPlaying && convertState(player.playbackState) != SpMsPlayerState.BUFFERING)
            || (
               BehaviourSettings.Key.STOP_PLAYER_ON_APP_CLOSE.get(context)
               && intent?.component?.packageName == packageName
                    )
            ) {
            stopSelf()
                onDestroy()
        }
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
    override val radio_state: RadioInstance.RadioState get() = service_player.radio_state
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
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun playPause() {
        if (player.isPlaying) {
            player.pause()
        }
        else {
            player.play()
        }
    }

    override fun seekTo(position_ms: Long) {
        player.seekTo(position_ms)
        listeners.forEach { it.onSeeked(position_ms) }
    }

    override fun seekToSong(index: Int) {
        player.seekTo(index, 0)
    }

    override fun seekToNext() {
        player.seekToNext()
    }

    override fun seekToPrevious() {
        player.seekToPrevious()
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
        player.addMediaItem(index, song.buildExoMediaItem(context))
        listeners.forEach { it.onSongAdded(index, song) }

        service_player.session_started = true
    }

    override fun moveSong(from: Int, to: Int) {
        player.moveMediaItem(from, to)
        listeners.forEach { it.onSongMoved(from, to) }
    }

    override fun removeSong(index: Int) {
        player.removeMediaItem(index)
        listeners.forEach { it.onSongRemoved(index) }
    }

    @Composable
    override fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        val visualiser: ExoVisualizer = remember { ExoVisualizer(fft_audio_processor) }
        visualiser.Visualiser(colour, modifier, opacity)
    }
    
    companion object {
        // If there's a better way to provide information to MediaControllers, I'd like to know
        val fft_audio_processor: FFTAudioProcessor = FFTAudioProcessor()
    }
}
