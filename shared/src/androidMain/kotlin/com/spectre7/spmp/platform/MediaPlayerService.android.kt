package com.spectre7.spmp.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
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
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.exovisualiser.FFTAudioProcessor
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.properties.Delegates
import androidx.media3.common.MediaItem as ExoMediaItem
//import com.google.android.exoplayer2.upstream.cache.Cache as ExoCache

private fun convertState(exo_state: Int): MediaPlayerState {
    return MediaPlayerState.values()[exo_state - 1]
}

@UnstableApi
class ActualMediaPlayerService: MediaSessionService() {
    private val NOTIFICATION_ID = 2
    private val NOTIFICATION_CHANNEL_ID = "playback_channel"

    private lateinit var player: ExoPlayer
    private lateinit var media_session: MediaSession
    private lateinit var custom_commands: List<CommandButton>
    private lateinit var notification_builder: NotificationCompat.Builder

    private val audio_processor = FFTAudioProcessor()

    override fun onCreate() {
        super.onCreate()

        custom_commands = listOf() // TODO

        initializeSessionAndPlayer()
        createNotificationChannel()

        notification_builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(PlatformContext.ic_spmp)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(media_session))

        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                updatePlayerNotification(false)
                return MediaNotification(NOTIFICATION_ID, notification_builder.build())
            }

            override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean {
                TODO("Not yet implemented")
            }
        })
    }

    private fun updatePlayerNotification(send: Boolean) {
        val song = media_session.player.currentMediaItem?.getSong()

        val large_icon: Bitmap? =
            if (song != null)
                getCurrentLargeIcon(song) {
                    runInMainThread {
                        updatePlayerNotification(true)
                    }
                }
            else null

        notification_builder.apply {
            if (Build.VERSION.SDK_INT < 33) {
                setLargeIcon(large_icon)
            }
            setContentTitle(song?.title ?: "")
            setContentText(song?.artist?.title ?: "")
        }

        if (send) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification_builder.build())
        }
    }

    private fun getCurrentLargeIcon(song: Song, callback: (Bitmap) -> Unit): Bitmap? {
        fun getCroppedThumbnail(image: Bitmap?): Bitmap? {
            if (image == null) {
                return null
            }
            return Bitmap.createBitmap(image, (image.width - image.height) / 2, 0, image.height, image.height)
        }

        try {
            if (song.isThumbnailLoaded(MediaItem.ThumbnailQuality.HIGH)) {
                return getCroppedThumbnail(song.loadThumbnail(MediaItem.ThumbnailQuality.HIGH)?.asAndroidBitmap())
            }

            // TODO
            thread {
                val cropped = getCroppedThumbnail(song.loadThumbnail(MediaItem.ThumbnailQuality.HIGH)?.asAndroidBitmap())
                if (cropped != null) {
                    callback(cropped)
                }
            }

            return null
        }
        catch (e: IndexOutOfBoundsException) {
            return null
        }
    }

    private fun initializeSessionAndPlayer() {
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
            .setUsePlatformDiagnostics(true)
            .build()
            .apply {
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
                        song.loadThumbnail(MediaItem.ThumbnailQuality.HIGH)!!.asAndroidBitmap()
                    }
                }
            })
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    media_session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    media_items: List<ExoMediaItem>
                ): ListenableFuture<List<ExoMediaItem>> {
                    val updated_media_items = media_items.map { item ->
                        item.buildUpon()
                            .setUri(item.requestMetadata.mediaUri)
                            .setMediaId(item.requestMetadata.mediaUri.toString())
                            .build()
                    }
                    return Futures.immediateFuture(updated_media_items)
                }

            })
            .build()
            .apply {
        }
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
            }
            else {
                println("Playing song ${song.title} from external format ${format.getOrThrow()}")
                data_spec.withUri(Uri.parse(format.getOrThrow().stream_url))
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    override fun onDestroy() {
        player.release()
        media_session.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return media_session
    }
}

@UnstableApi
actual open class MediaPlayerService {

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

    private lateinit var player: MediaController
//    private lateinit var cache: Cache
    private val listeners: MutableList<Listener> = mutableListOf()

    // Undo
    private var current_action: MutableList<UndoRedoAction>? = null
    private val action_list: MutableList<List<UndoRedoAction>> = mutableListOf()
    private var action_head: Int = 0

    actual val supports_visualiser: Boolean = true

    actual open fun onCreate() {}
    actual open fun onDestroy() {}

    @Composable
    actual fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        TODO()
//        val visualiser = remember { ExoVisualizer(audio_processor) }
//        visualiser.Visualiser(colour, modifier, opacity)
    }

    actual var session_started: Boolean by mutableStateOf(false)
    actual var context: PlatformContext by Delegates.notNull()

    actual val state: MediaPlayerState get() = convertState(player.playbackState)
    actual val is_playing: Boolean get() = player.isPlaying
    actual val song_count: Int get() = player.mediaItemCount
    actual val current_song_index: Int get() = player.currentMediaItemIndex
    actual val current_position_ms: Long get() = player.currentPosition
    actual val duration_ms: Long get() = player.duration
    actual val undo_count: Int get() = action_head
    actual val redo_count: Int get() = action_list.size - undo_count

    actual var repeat_mode: MediaPlayerRepeatMode
        get() = MediaPlayerRepeatMode.values()[player.repeatMode]
        set(value) { player.repeatMode = value.ordinal }
    actual var volume: Float
        get() = player.volume
        set(value) { player.volume = value }

//    actual val has_focus: Boolean get() = player.audioFocusState == Player.AUDIO_FOCUS_STATE_HAVE_FOCUS
    actual val has_focus: Boolean get() = true // TODO

    actual open fun play() {
        if (state == MediaPlayerState.ENDED) {
            seekTo(0)
        }
        player.play()
    }
    actual open fun pause() = player.pause()
    actual open fun playPause() {
        if (is_playing) pause()
        else play()
    }

    actual open fun seekTo(position_ms: Long) {
        player.seekTo(position_ms)
        listeners.forEach { it.onSeeked(position_ms) }
    }
    actual open fun seekToSong(index: Int) {
        player.seekTo(index, 0)
    }
    actual open fun seekToNext() = player.seekToNextMediaItem()
    actual open fun seekToPrevious() = player.seekToPreviousMediaItem()

    actual fun getSong(): Song? = getSong(current_song_index)
    actual fun getSong(index: Int): Song? {
        if (index < 0 || index >= song_count) {
            return null
        }
        return player.getMediaItemAt(index).getSong()
    }

    actual fun addSong(song: Song) {
        addSong(song, song_count)
    }
    actual fun addSong(song: Song, index: Int) {
        require(index in 0 .. song_count)

        val item = ExoMediaItem.Builder()
            .setRequestMetadata(ExoMediaItem.RequestMetadata.Builder().setMediaUri(song.id.toUri()).build())
            .setTag(song)
            .setUri(song.id)
            .setCustomCacheKey(song.id)
            .setMediaMetadata(
                MediaMetadata.Builder().setArtworkUri(
                    song.id.toUri()
                ).build()
            )
            .build()
        performAction(AddAction(item, index))

        session_started = true // TODO
//        addNotificationToPlayer()
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
        listener.addToPlayer(player)
    }
    actual fun removeListener(listener: Listener) {
        listener.removeFromPlayer(player)
        listeners.remove(listener)
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
            player.addMediaItem(index, item)
            listeners.forEach { it.onSongAdded(index, item.getSong()) }
        }
        override fun undo() {
            player.removeMediaItem(index)
            listeners.forEach { it.onSongRemoved(index) }
        }
    }
    private inner class MoveAction(val from: Int, val to: Int): Action() {
        override fun redo() {
            player.moveMediaItem(from, to)
            listeners.forEach { it.onSongMoved(from, to) }
        }
        override fun undo() {
            player.moveMediaItem(to, from)
            listeners.forEach { it.onSongMoved(to, from) }
        }
    }
    private inner class RemoveAction(val index: Int): Action() {
        private lateinit var item: ExoMediaItem
        override fun redo() {
            item = player.getMediaItemAt(index)
            player.removeMediaItem(index)
            listeners.forEach { it.onSongRemoved(index) }
        }
        override fun undo() {
            player.addMediaItem(index, item)
            listeners.forEach { it.onSongAdded(index, item.getSong()) }
        }
    }
    private inner class ClearAction(): Action() {
        private var items: List<ExoMediaItem>? = null
        override fun redo() {
            if (items == null && is_undoable) {
                items = List(player.mediaItemCount) {
                    player.getMediaItemAt(it)
                }
            }
            player.clearMediaItems()
        }
        override fun undo() {
            assert(items != null && player.mediaItemCount == 0)
            for (item in items!!) {
                player.addMediaItem(item)
            }
        }
    }

    private fun release() {
        player.release()
    }
    
    actual companion object {
        actual fun CoroutineScope.playerLaunch(action: CoroutineScope.() -> Unit) {
            launch(Dispatchers.Main, block = action)
        }

        actual fun <T: MediaPlayerService> connect(context: PlatformContext, cls: Class<T>, onConnected: (service: T) -> Unit, onDisconnected: () -> Unit) {
            val session_token = SessionToken(context.ctx, ComponentName(context.ctx, ActualMediaPlayerService::class.java))
            val controller_future = MediaController.Builder(context.ctx, session_token).buildAsync()
            controller_future.addListener(
                {
                    val service = cls.newInstance()
                    service.player = controller_future.get()
                    service.context = context
                    onConnected(service)
                },
                MoreExecutors.directExecutor()
            )

        }
        actual fun disconnect(context: PlatformContext, service: MediaPlayerService) {
            service.release()
        }
    }
}

fun ExoMediaItem.getSong(): Song {
    return when (val tag = localConfiguration?.tag) {
        is IndexedValue<*> -> tag.value as Song
        is Song -> tag
        else -> {
            check(mediaId.isNotBlank())
            Song.fromId(mediaId)
        }
    }
}
