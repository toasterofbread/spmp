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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.*
import androidx.media3.session.*
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PlatformPreferencesListener
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import dev.toastbits.ytmkt.model.implementedOrNull
import dev.toastbits.ytmkt.endpoint.SetSongLikedEndpoint
import com.toasterofbread.spmp.platform.visualiser.FFTAudioProcessor
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatusListener
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.PlayerServiceCommand
import com.toasterofbread.spmp.platform.playerservice.*
import com.toasterofbread.spmp.platform.visualiser.MusicVisualiser
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import kotlinx.coroutines.*
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState

@androidx.annotation.OptIn(UnstableApi::class)
open class ForegroundPlayerService(
    private val play_when_ready: Boolean,
    private val playlist_auto_progress: Boolean = true
): MediaSessionService(), PlayerService {
    override val load_state: PlayerServiceLoadState = PlayerServiceLoadState(false)
    override val context: AppContext get() = _context
    private lateinit var _context: AppContext

    internal val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    internal lateinit var state: ExoPlayer
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
                    loudness_enhancer?.update(current_song, context)
                }
                context.settings.streaming.ENABLE_SILENCE_SKIPPING.key -> {
                    audio_sink.skipSilenceEnabled = context.settings.streaming.ENABLE_SILENCE_SKIPPING.get()
                }
            }
        }

    private val listeners: MutableList<PlayerListener> = mutableListOf()

    override fun addListener(listener: PlayerListener) {
        listener.addToPlayer(state)
        listeners.add(listener)
    }
    override fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
        listener.removeFromPlayer(state)
    }

    protected open fun onRadioCancelled() {}

    protected open fun getNotificationPlayer(state: Player): Player = state

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

        _context = AppContext(this, coroutine_scope)
        _context.getPrefs().addListener(prefs_listener)

        initialiseSessionAndPlayer(
            play_when_ready,
            playlist_auto_progress,
            getNotificationPlayer = { getNotificationPlayer(it) }
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
        stopSelf()

        _context.getPrefs().removeListener(prefs_listener)
        coroutine_scope.cancel()
        service_player.release()
        state.release()
        media_session.release()
        loudness_enhancer?.release()
        SongLikedStatusListener.removeListener(song_liked_listener)

        val audio_manager = getSystemService(AUDIO_SERVICE) as AudioManager?
        audio_manager?.unregisterAudioDeviceCallback(audio_device_callback)

        clearListener()
        super.onDestroy()
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)

        if (
            (!state.isPlaying && convertState(state.playbackState) != SpMsPlayerState.BUFFERING)
            || (
                context.settings.behaviour.STOP_PLAYER_ON_APP_CLOSE.get()
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
    override val state: SpMsPlayerState get() = convertState(state.playbackState)
    override val is_playing: Boolean get() = state.isPlaying
    override val song_count: Int get() = state.mediaItemCount
    override val current_song_index: Int get() = state.currentMediaItemIndex
    override val current_position_ms: Long get() = state.currentPosition
    override val duration_ms: Long get() = state.duration
    override val radio_instance: RadioInstance get() = service_player.radio_instance
    override var repeat_mode: SpMsPlayerRepeatMode
        get() = SpMsPlayerRepeatMode.entries[state.repeatMode]
        set(value) {
            state.repeatMode = value.ordinal
        }
    override var volume: Float
        get() = state.volume
        set(value) {
            state.volume = value
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
        state.play()
    }

    override fun pause() {
        state.pause()
    }

    override fun playPause() {
        if (state.isPlaying) {
            state.pause()
        }
        else {
            state.play()
        }
    }

    private val song_seek_undo_stack: MutableList<Pair<Int, Long>> = mutableListOf()
    private fun getSeekPosition(): Pair<Int, Long> = Pair(current_song_index, current_position_ms)

    override fun seekTo(position_ms: Long) {
        val current: Pair<Int, Long> = getSeekPosition()
        state.seekTo(position_ms)
        listeners.forEach { it.onSeeked(position_ms) }

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun seekToSong(index: Int) {
        val current: Pair<Int, Long> = getSeekPosition()
        state.seekTo(index, 0)

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun seekToNext() {
        val current: Pair<Int, Long> = getSeekPosition()
        state.seekToNext()

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun seekToPrevious() {
        val current: Pair<Int, Long> = getSeekPosition()
        state.seekToPrevious()

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun undoSeek() {
        val (index: Int, position_ms: Long) = song_seek_undo_stack.removeLastOrNull() ?: return

        if (index != current_song_index) {
            state.seekTo(index, position_ms)
        }
        else {
            state.seekTo(position_ms)
        }
    }

    override fun getSong(): Song? {
        return state.currentMediaItem?.getSong()
    }

    override fun getSong(index: Int): Song? {
        if (index !in 0 until song_count) {
            return null
        }

        return state.getMediaItemAt(index).getSong()
    }

    override fun addSong(song: Song, index: Int) {
        state.addMediaItem(index, song.buildExoMediaItem(context))
        listeners.forEach { it.onSongAdded(index, song) }

        service_player.session_started = true
    }

    override fun moveSong(from: Int, to: Int) {
        state.moveMediaItem(from, to)
        listeners.forEach { it.onSongMoved(from, to) }
    }

    override fun removeSong(index: Int) {
        val song: Song = state.getMediaItemAt(index).getSong()
        state.removeMediaItem(index)
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
