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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.viewinterop.AndroidView
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
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.exovisualiser.ExoVisualizer
import com.spectre7.spmp.exovisualiser.FFTAudioProcessor
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.resources.getString
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

    actual interface UndoRedoAction {
        actual fun undo()
        actual fun redo()
    }

    actual open class Listener {
        actual open fun onSongTransition(song: Song?) {}
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

    // Undo
    private var current_action: MutableList<UndoRedoAction>? = null
    private val action_list: MutableList<List<UndoRedoAction>> = mutableListOf()
    private var action_head: Int = 0

    private val audio_processor = FFTAudioProcessor()
    actual val supports_waveform: Boolean = true

    @Composable
    actual fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        val visualiser = remember { ExoVisualizer(audio_processor) }
        visualiser.Visualiser(colour, modifier, opacity)
    }

    override fun onCreate() {
        super.onCreate()

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
    actual val undo_count: Int get() = action_head
    actual val redo_count: Int get() = action_list.size - undo_count

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
    actual open fun seekToSong(index: Int) {
        player!!.seekTo(index, 0)
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
        require(index in 0 .. song_count)

        val item = ExoMediaItem.Builder().setTag(song).setUri(song.id).setCustomCacheKey(song.id).build()
        performAction(AddAction(item, index))

        addNotificationToPlayer()
    }
    actual fun moveSong(from: Int, to: Int) {
        require(from in 0 until song_count)
        require(to in 0 until song_count)

        performAction(MoveAction(from, to))
    }
    actual fun removeSong(index: Int) {
        require(index in 0 until song_count)
        performAction(RemoveAction(index))
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
                println("Playing song ${song.title} from local file $local_file")
                return@Factory data_spec.withUri(Uri.fromFile(local_file))
            }

            if (
                song.registry_entry.play_count >= Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.get<Int>(this)
                && (Settings.KEY_AUTO_DOWNLOAD_ON_METERED.get(this) || !isConnectionMetered())
            ) {
                var done = false
                runBlocking {

                    download_manager.getDownload(song) { initial_status ->

                        when (initial_status?.status) {
                            DownloadStatus.Status.DOWNLOADING -> {
                                val listener = object : PlayerDownloadManager.DownloadStatusListener() {
                                    override fun onDownloadChanged(status: DownloadStatus) {
                                        if (status.song != song) {
                                            return
                                        }

                                        when (status.status) {
                                            DownloadStatus.Status.IDLE, DownloadStatus.Status.DOWNLOADING -> return
                                            DownloadStatus.Status.PAUSED -> throw IllegalStateException()
                                            DownloadStatus.Status.CANCELLED -> {
                                                done = true
                                            }
                                            DownloadStatus.Status.FINISHED, DownloadStatus.Status.ALREADY_FINISHED -> {
                                                local_file = download_manager.getSongLocalFile(song)
                                                done = true
                                            }
                                        }

                                        download_manager.removeDownloadStatusListener(this)
                                    }
                                }
                                download_manager.addDownloadStatusListener(listener)
                            }
                            DownloadStatus.Status.IDLE, DownloadStatus.Status.CANCELLED, DownloadStatus.Status.PAUSED, null -> {
                                download_manager.startDownload(song.id, true) { status ->
                                    local_file = status.file
                                    done = true
                                }
                            }
                            DownloadStatus.Status.ALREADY_FINISHED, DownloadStatus.Status.FINISHED -> throw IllegalStateException()
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
        )
        .setNotificationListener(
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    super.onNotificationPosted(notificationId, notification, ongoing)
                    startForeground(notificationId, notification)
                }
                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    super.onNotificationCancelled(notificationId, dismissedByUser)
                    stopSelf()
                }
            }
        )
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

    actual fun undoableAction(action: MediaPlayerService.() -> Unit) {
        undoableActionWithCustom {
            action()
            null
        }
    }

    actual fun undoableActionWithCustom(action: MediaPlayerService.() -> UndoRedoAction?) {
        synchronized(action_list) {
            assert(current_action == null)
            current_action = mutableListOf()

            val customAction = action(this)
            if (customAction != null) {
                performAction(customAction)
            }

            for (i in 0 until redo_count) {
                action_list.removeLast()
            }
            action_list.add(current_action!!)
            action_head++

            current_action = null
            listeners.forEach { it.onUndoStateChanged() }
        }
    }

    private fun performAction(action: UndoRedoAction) {
        action.redo()
        current_action?.add(action)
    }

    actual fun redo() {
        synchronized(action_list) {
            if (redo_count == 0) {
                return
            }
        }
    }

    actual fun redoAll() {
        synchronized(action_list) {
            for (i in 0 until redo_count) {
                for (action in action_list[action_head++]) {
                    action.redo()
                }
            }
            listeners.forEach { it.onUndoStateChanged() }
        }
    }

    actual fun undo() {
        synchronized(action_list) {
            if (undo_count == 0) {
                return
            }
            for (action in action_list[--action_head].asReversed()) {
                action.undo()
            }
            listeners.forEach { it.onUndoStateChanged() }
        }
    }

    actual fun undoAll() {
        synchronized(action_list) {
            for (i in 0 until undo_count) {
                for (action in action_list[--action_head].asReversed()) {
                    action.undo()
                }
            }
            listeners.forEach { it.onUndoStateChanged() }
        }
    }

    private abstract inner class Action: UndoRedoAction {
        protected val is_undoable: Boolean get() = current_action != null
    }
    private inner class AddAction(val item: ExoMediaItem, val index: Int): Action() {
        override fun redo() {
            player!!.addMediaItem(index, item)
            listeners.forEach { it.onSongAdded(index, item.getSong()) }
        }
        override fun undo() {
            player!!.removeMediaItem(index)
            listeners.forEach { it.onSongRemoved(index) }
        }
    }
    private inner class MoveAction(val from: Int, val to: Int): Action() {
        override fun redo() {
            player!!.moveMediaItem(from, to)
            listeners.forEach { it.onSongMoved(from, to) }
        }
        override fun undo() {
            player!!.moveMediaItem(to, from)
            listeners.forEach { it.onSongMoved(to, from) }
        }
    }
    private inner class RemoveAction(val index: Int): Action() {
        private lateinit var item: ExoMediaItem
        override fun redo() {
            item = player!!.getMediaItemAt(index)
            player!!.removeMediaItem(index)
            listeners.forEach { it.onSongRemoved(index) }
        }
        override fun undo() {
            player!!.addMediaItem(index, item)
            listeners.forEach { it.onSongAdded(index, item.getSong()) }
        }
    }
    private inner class ClearAction(): Action() {
        private var items: List<ExoMediaItem>? = null
        override fun redo() {
            if (items == null && is_undoable) {
                items = List(player!!.mediaItemCount) {
                    player!!.getMediaItemAt(it)
                }
            }
            player!!.clearMediaItems()
        }
        override fun undo() {
            assert(items != null && player!!.mediaItemCount == 0)
            for (item in items!!) {
                player!!.addMediaItem(item)
            }
        }
    }

    actual companion object {
        actual fun CoroutineScope.playerLaunch(action: CoroutineScope.() -> Unit) {
            launch(Dispatchers.Main, block = action)
        }
    }
}

fun ExoMediaItem.getSong(): Song {
    return when (val tag = localConfiguration!!.tag) {
        is IndexedValue<*> -> tag.value as Song
        is Song -> tag
        else -> throw IllegalStateException()
    }
}
