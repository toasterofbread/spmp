package com.toasterofbread.spmp.platform

import android.content.ComponentName
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.toasterofbread.spmp.exovisualiser.ExoVisualizer
import com.toasterofbread.spmp.model.mediaitem.Song
import kotlin.properties.Delegates
import androidx.media3.common.MediaItem as ExoMediaItem

//import com.google.android.exoplayer2.upstream.cache.Cache as ExoCache

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
            var current_song: Song? = null
            override fun onMediaItemTransition(item: ExoMediaItem?, reason: Int) {
                val song = item?.getSong()
                if (song == current_song) {
                    return
                }
                current_song = song
                onSongTransition(song)
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

    actual open fun onCreate() {
        session_started = player.mediaItemCount > 0
        for (i in 0 until player.mediaItemCount) {
            val song = player.getMediaItemAt(i).getSong()
            for (listener in listeners) {
                listener.onSongAdded(i, song)
            }
        }
    }
    actual open fun onDestroy() {}

    @Composable
    actual fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        val visualiser = remember { ExoVisualizer(MediaPlayerServiceSession.audio_processor) }
        visualiser.Visualiser(colour, modifier, opacity)
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
        if (index !in 0 .. song_count) {
            return
        }

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

    protected actual open fun onSongMoved(from: Int, to: Int) {}

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

            val custom_action = action(this)
            if (custom_action != null) {
                performAction(custom_action)
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
            for (action in action_list[action_head++]) {
                action.redo()
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
            onSongMoved(from, to)
        }
        override fun undo() {
            player.moveMediaItem(to, from)
            listeners.forEach { it.onSongMoved(to, from) }
            onSongMoved(to, from)
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
    private inner class ClearAction : Action() {
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
        onDestroy()
    }
    
    actual companion object {
        actual fun <T: MediaPlayerService> connect(context: PlatformContext, cls: Class<T>, instance: T?, onConnected: (controller: T) -> Unit): Any {
            val ctx = context.ctx.applicationContext
            val controller_future = MediaController.Builder(
                ctx,
                SessionToken(ctx, ComponentName(ctx, MediaPlayerServiceSession::class.java))
            ).buildAsync()

            controller_future.addListener(
                {
                    val controller = instance ?: cls.newInstance()
                    controller.player = controller_future.get()
                    controller.context = PlatformContext(ctx)
                    if (instance == null) {
                        controller.onCreate()
                    }
                    onConnected(controller)
                },
                MoreExecutors.directExecutor()
            )

            return controller_future
        }
        actual fun disconnect(context: PlatformContext, connection: Any) {
            MediaController.releaseFuture(connection as ListenableFuture<MediaController>)
        }
    }
}

private fun convertState(exo_state: Int): MediaPlayerState {
    return MediaPlayerState.values()[exo_state - 1]
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
