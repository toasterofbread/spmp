package com.toasterofbread.spmp.platform.playerservice

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRouter
import android.media.audiofx.LoudnessEnhancer
import android.media.session.MediaSession
import android.os.Binder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.getSystemService
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.playerservice.notification.PlayerServiceNotificationManager
import com.toasterofbread.spmp.platform.visualiser.FFTAudioProcessor
import com.toasterofbread.spmp.platform.visualiser.MusicVisualiser
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import com.toasterofbread.spmp.widget.WidgetUpdateListener
import dev.toastbits.composekit.settings.PlatformSettingsListener
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.spms.player.shouldRepeatOnSeekToPrevious
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("RestrictedApi")
@androidx.annotation.OptIn(UnstableApi::class)
open class ForegroundPlayerService(
    private val play_when_ready: Boolean,
    private val playlist_auto_progress: Boolean = true
): Service(), PlayerService {
    override val load_state: PlayerServiceLoadState = PlayerServiceLoadState(false)
    override val context: AppContext get() = _context
    private lateinit var _context: AppContext
    private var stopped: Boolean = false
    private lateinit var notification_manager: PlayerServiceNotificationManager
    private lateinit var media_data_spec_processor: MediaDataSpecProcessor

    internal val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    internal val colorblendr_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    internal lateinit var player: ExoPlayer
    internal lateinit var media_session: MediaSession
    internal lateinit var audio_sink: AudioSink
    internal var loudness_enhancer: LoudnessEnhancer? = null

    internal var current_song: Song? = null
    internal var paused_by_device_disconnect: Boolean = false
    internal var device_connection_changed_playing_status: Boolean = false

    private var repeat_song_on_previous_threshold: Duration? = null

    private lateinit var widget_update_listener: WidgetUpdateListener
    private val audio_device_callback: PlayerAudioDeviceCallback = PlayerAudioDeviceCallback(this)

    private val prefs_listener: PlatformSettingsListener =
        PlatformSettingsListener { key ->
            when (key) {
                context.settings.Streaming.ENABLE_AUDIO_NORMALISATION.key -> {
                    coroutine_scope.launch {
                        loudness_enhancer?.update(current_song, context)
                    }
                }
                context.settings.Streaming.ENABLE_SILENCE_SKIPPING.key -> {
                    coroutine_scope.launch {
                        audio_sink.skipSilenceEnabled = context.settings.Streaming.ENABLE_SILENCE_SKIPPING.get()
                    }
                }
                context.settings.Behaviour.REPEAT_SONG_ON_PREVIOUS_THRESHOLD_S.key -> {
                    coroutine_scope.launch {
                        updateRepeatSongOnPreviousThreshold()
                    }
                }
                context.settings.Experimental.ANDROID_MONET_COLOUR_ENABLE.key -> {
                    startColorblendrHeartbeatLoop()
                }
            }
        }

    private suspend fun updateRepeatSongOnPreviousThreshold() {
        val threshold_s: Float = context.settings.Behaviour.REPEAT_SONG_ON_PREVIOUS_THRESHOLD_S.get()
        if (threshold_s < 0f) {
            repeat_song_on_previous_threshold = null
        }
        else {
            repeat_song_on_previous_threshold = (threshold_s * 1000).roundToLong().milliseconds
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

    protected open fun getNotificationPlayer(): PlayerService = this

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        println("ForegroundPlayerService.onStartCommand ${intent?.action}")

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): Binder = PlayerBinder(this)

    override fun onCreate() {
        super.onCreate()

        _context = runBlocking {
            AppContext.create(this@ForegroundPlayerService, coroutine_scope)
        }
        _context.getPrefs().addListener(prefs_listener)

        media_data_spec_processor = MediaDataSpecProcessor(context)

        initialiseSessionAndPlayer(
            play_when_ready,
            playlist_auto_progress,
            coroutine_scope,
            media_data_spec_processor,
            getNotificationPlayer = { getNotificationPlayer() },
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

        startColorblendrHeartbeatLoop()

        widget_update_listener = WidgetUpdateListener(this, context)
        player.addListener(widget_update_listener)

        notification_manager = PlayerServiceNotificationManager(context, media_session, getSystemService()!!, this, player)

        coroutine_scope.launch {
            updateRepeatSongOnPreviousThreshold()
        }
    }

    override fun release() {
        stopped = true
        stopForeground(STOP_FOREGROUND_REMOVE)

        _context.getPrefs().removeListener(prefs_listener)
        coroutine_scope.cancel()
        service_player.release()
        player.release()
        media_session.release()
        loudness_enhancer?.release()
        media_data_spec_processor.release()
        notification_manager.release()

        player.removeListener(widget_update_listener)
        widget_update_listener.release()

        val audio_manager: AudioManager? = getSystemService()
        audio_manager?.unregisterAudioDeviceCallback(audio_device_callback)

        println("ForegroundPlayerService stopped, notifying listeners and updating all widgets")

        for (listener in listeners) {
            listener.onPlayingChanged(false)
            listener.onStateChanged(SpMsPlayerState.ENDED)
            listener.onSongTransition(null, false)
        }
        listeners.clear()

        runBlocking {
            widget_update_listener.updateAll()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    override fun onShutdown() {}

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)

        coroutine_scope.launch {
            if (context.settings.Behaviour.STOP_PLAYER_ON_APP_CLOSE.get()) {
                stop()
            }
        }
    }

    fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun checkAlive() {
        check(!stopped) { "Attempting to use a stopped foreground player service $this. This is a bug." }
    }

    private fun startColorblendrHeartbeatLoop() = colorblendr_coroutine_scope.launchSingle {
        if (!context.settings.Experimental.ANDROID_MONET_COLOUR_ENABLE.get()) {
            return@launchSingle
        }

        val action: String = "com.drdisagree.colorblendr.SERVICE_HEARTBEAT"
        val intent: Intent = Intent(action).setPackage("com.drdisagree.colorblendr")
        intent.putExtra("owner", "com.toasterofbread.spmp")

        while (true) {
            delay(3000)
            startService(intent)
        }
    }

    private lateinit var _service_player: PlayerServicePlayer
    override val service_player: PlayerServicePlayer get() = _service_player
    override val state: SpMsPlayerState get() = convertState(player.playbackState)

    override fun canPlay(): Boolean = true

    override val is_playing: Boolean get() = player.isPlaying
    override val item_count: Int get() = player.mediaItemCount
    override val current_item_index: Int get() = player.currentMediaItemIndex
    override val current_position_ms: Long get() = player.currentPosition
    override val duration_ms: Long get() = player.duration
    override val radio_instance: RadioInstance get() = service_player.radio_instance
    override val repeat_mode: SpMsPlayerRepeatMode
        get() = SpMsPlayerRepeatMode.entries[player.repeatMode]
    override val volume: Float
        get() = player.volume

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
    private fun getSeekPosition(): Pair<Int, Long> = Pair(current_item_index, current_position_ms)

    override fun seekToTime(position_ms: Long) {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()
        player.seekTo(position_ms)
        listeners.forEach { it.onSeeked(position_ms) }

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun setRepeatMode(repeat_mode: SpMsPlayerRepeatMode) {
        player.repeatMode = repeat_mode.ordinal
    }

    override fun setVolume(value: Double) {
        player.volume = value.toFloat()
    }

    override fun seekToItem(index: Int, position_ms: Long) {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()
        player.seekTo(index, position_ms)

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
        }
    }

    override fun seekToNext(): Boolean {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()
        player.seekToNextMediaItem()

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
            return true
        }

        return false
    }

    override fun seekToPrevious(repeat_threshold: Duration?): Boolean {
        checkAlive()

        val current: Pair<Int, Long> = getSeekPosition()

        if (shouldRepeatOnSeekToPrevious(repeat_threshold)) {
            seekToTime(0)
        }
        else {
            player.seekToPreviousMediaItem()
        }

        if (current != getSeekPosition()) {
            song_seek_undo_stack.add(current)
            return true
        }

        return false
    }

    override fun undoSeek() {
        checkAlive()

        val (index: Int, position_ms: Long) = song_seek_undo_stack.removeLastOrNull() ?: return

        if (index != current_item_index) {
            player.seekTo(index, position_ms)
        }
        else {
            player.seekTo(position_ms)
        }
    }

    override fun getSong(): Song? {
        return player.currentMediaItem?.toSong()
    }

    override fun getSong(index: Int): Song? {
        if (index !in 0 until item_count) {
            return null
        }

        return player.getMediaItemAt(index).toSong()
    }

    override fun getItem(): String? =
        getSong()?.id

    override fun getItem(index: Int): String? =
        getSong(index)?.id

    override fun addSong(song: Song, index: Int): Int {
        checkAlive()

        val add_to_index: Int =
            if (index < 0) 0
            else index.coerceAtMost(item_count)

        player.addMediaItem(add_to_index, song.buildExoMediaItem(context))
        listeners.forEach { it.onSongAdded(add_to_index, song) }

        service_player.session_started = true

        return add_to_index
    }

    override fun addItem(item_id: String, index: Int): Int {
        return addSong(SongRef(item_id), index)
    }

    override fun moveItem(from: Int, to: Int) {
        checkAlive()

        player.moveMediaItem(from, to)
        listeners.forEach { it.onSongMoved(from, to) }
    }

    override fun removeItem(index: Int) {
        checkAlive()

        val song: Song = player.getMediaItemAt(index).toSong()
        player.removeMediaItem(index)
        listeners.forEach { it.onSongRemoved(index, song) }
    }

    override fun clearQueue() {
        checkAlive()
        for (index in 0 until item_count) {
            removeItem(index)
        }
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
