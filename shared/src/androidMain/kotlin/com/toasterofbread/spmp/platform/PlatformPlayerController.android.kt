package com.toasterofbread.spmp.platform

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.exovisualiser.ExoVisualizer
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceState
import com.toasterofbread.spmp.platform.playerservice.fromBundle
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import androidx.media3.common.MediaItem as ExoMediaItem

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
actual class PlatformPlayerController {
    actual val context: PlatformContext get() = _context
    actual val service_state: PlayerServiceState get() = _service_state.value

    private lateinit var _context: PlatformContext
    private lateinit var player: MediaController
    private lateinit var _service_state: State<PlayerServiceState>

    fun init(context: PlatformContext, player: MediaController, service_state: State<PlayerServiceState>) {
        _context = context
        this.player = player
        _service_state = service_state

        PlatformPlayerService.addListener(listener)

        if (ProjectBuildConfig.MUTE_PLAYER == true) {
            player.volume = 0f
        }
    }

    fun release() {
        PlatformPlayerService.removeListener(listener)
        player.release()
        onDestroy()
    }

    private val listener = object : PlayerListener() {
        override fun onSongTransition(song: Song?, manual: Boolean) {
            pending_seek_position = null
        }

        override fun onStateChanged(state: MediaPlayerState) {
            if (state == MediaPlayerState.READY) {
                pending_seek_position?.also { position_ms ->
                    seekTo(position_ms)
                    pending_seek_position = null
                }
            }
        }
    }

    // Seek
    private var pending_seek_position: Long? = null

    actual open fun onCreate() {}
    actual open fun onDestroy() {}

    @Composable
    actual fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        val visualiser = remember { ExoVisualizer(PlatformPlayerService.audio_processor) }
        visualiser.Visualiser(colour, modifier, opacity)
    }

    actual val state: MediaPlayerState get() = convertState(player.playbackState)
    actual val is_playing: Boolean get() = player.isPlaying
    actual val song_count: Int get() = PlatformPlayerService.instance.song_count//player.mediaItemCount
    actual val current_song_index: Int get() = player.currentMediaItemIndex
    actual val current_position_ms: Long get() = player.currentPosition
    actual val duration_ms: Long get() = player.duration
    actual val undo_count: Int get() = service_state.undo_count
    actual val redo_count: Int get() = service_state.redo_count
    actual val radio_state: RadioInstance.RadioState get() = service_state.radio_state
    actual val active_queue_index: Int get() = service_state.active_queue_index
    actual val session_started: Boolean get() = service_state.session_started

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
        if (player.playbackState != Player.STATE_READY) {
            pending_seek_position = position_ms
        }
        else {
            player.seekTo(position_ms)
        }
    }

    actual open fun seekBy(delta_ms: Long) {
        seekTo(current_position_ms + delta_ms)
    }

    actual open fun seekToSong(index: Int) {
        player.seekTo(index, 0)
    }
    actual open fun seekToNext() = player.seekToNextMediaItem()
    actual open fun seekToPrevious() = player.seekToPreviousMediaItem()

    actual fun continueRadio(is_retry: Boolean) {
        PlayerServiceCommand.ContinueRadio(is_retry).sendCommand(player)
    }

    actual fun dismissRadioLoadError() {
        PlayerServiceCommand.DismissRadioLoadError.sendCommand(player)
    }

    actual fun setRadioFilter(filter_index: Int?) {
        PlayerServiceCommand.SetRadioFilter(filter_index).sendCommand(player)
    }

    actual fun startRadioAtIndex(
        index: Int,
        item: MediaItem?,
        item_index: Int?,
        add_item: Boolean,
        skip_first: Boolean,
        shuffle: Boolean,
        on_load_seek_index: Int?
    ) {
        PlayerServiceCommand.StartRadio(index, item?.getUid(), item_index, add_item, skip_first, shuffle, on_load_seek_index).sendCommand(player)
    }

    actual fun getSong(): Song? = getSong(current_song_index)
    actual fun getSong(index: Int): Song? {
        if (index < 0 || index >= song_count) {
            return null
        }
        return player.getMediaItemAt(index).getSong()
    }

    actual fun cancelSession() {
        PlayerServiceCommand.CancelSession.sendCommand(player)
    }

    actual fun addSong(song: Song) {
        addSong(song, song_count)
    }
    actual fun addSong(song: Song, index: Int, is_active_queue: Boolean, start_radio: Boolean) {
        val add_index = if (index < 0) song_count else index
        if (add_index !in 0 .. song_count) {
            return
        }

        PlayerServiceCommand.AddSong(song.id, add_index, is_active_queue, start_radio).sendCommand(player)
//        addNotificationToPlayer()
    }
    actual fun addMultipleSongs(
        songs: List<Song>,
        index: Int,
        skip_first: Boolean,
        is_active_queue: Boolean,
        skip_existing: Boolean,
        clear: Boolean
    ) {
        PlayerServiceCommand.AddMultipleSongs(songs.map { it.id }, index, skip_first, is_active_queue, skip_existing, clear)
    }

    actual fun moveSong(from: Int, to: Int) {
        PlayerServiceCommand.MoveSong(from, to).sendCommand(player)
    }
    actual fun removeSong(index: Int) {
        PlayerServiceCommand.RemoveSong(index).sendCommand(player)
    }
    actual fun removeMultipleSongs(indices: List<Int>) {
        PlayerServiceCommand.RemoveMultipleSongs(indices).sendCommand(player)
    }

    actual fun clearQueue(from: Int, keep_current: Boolean, cancel_radio: Boolean) {
        PlayerServiceCommand.ClearQueue(from, keep_current, cancel_radio).sendCommand(player)
    }

    actual fun shuffleQueue(start: Int, end: Int) {
        PlayerServiceCommand.ShuffleQueue(start, end).sendCommand(player)
    }

    actual fun shuffleQueueIndices(indices: List<Int>) {
        PlayerServiceCommand.ShuffleQueueIndices(indices).sendCommand(player)
    }

    actual fun triggerSavePersistentQueue() {
        PlayerServiceCommand.SavePersistentQueue.sendCommand(player)
    }

    actual fun setStopAfterCurrentSong(value: Boolean) {
        PlayerServiceCommand.SetStopAfterCurrentSong(value).sendCommand(player)
    }

    actual fun updateActiveQueueIndex(delta: Int) {
        PlayerServiceCommand.UpdateActiveQueueIndex(delta).sendCommand(player)
    }

    actual fun setActiveQueueIndex(value: Int) {
        PlayerServiceCommand.SetActiveQueueIndex(value).sendCommand(player)
    }

    actual companion object {
        actual fun <C: PlatformPlayerController> connect(
            context: PlatformContext,
            controller_class: Class<C>,
            instance: C?,
            onConnected: (service: C) -> Unit,
            onCancelled: () -> Unit,
        ): Any {
            val ctx: Context = context.ctx.applicationContext
            val service_state: MutableState<PlayerServiceState> = mutableStateOf(PlayerServiceState())

            val controller_future: ListenableFuture<MediaController> =
                MediaController.Builder(
                    ctx,
                    SessionToken(ctx, ComponentName(ctx, PlatformPlayerService::class.java))
                )
                    .setListener(
                        object : MediaController.Listener {
                            override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                                service_state.value = PlayerServiceState.fromBundle(extras)
                            }
                        }
                    )
                    .buildAsync()

            controller_future.addListener(
                {
                    if (controller_future.isCancelled) {
                        onCancelled()
                        return@addListener
                    }

                    val controller: C = instance ?: controller_class.newInstance()
                    val media_controller: MediaController = controller_future.get()

                    controller.init(context, media_controller, service_state)
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

    actual fun undo() {
        PlayerServiceCommand.Undo.sendCommand(player)
    }
    actual fun redo() {
        PlayerServiceCommand.Redo.sendCommand(player)
    }

    actual fun undoAll() {
        PlayerServiceCommand.UndoAll.sendCommand(player)
    }
    actual fun redoAll() {
        PlayerServiceCommand.RedoAll.sendCommand(player)
    }
}

fun convertState(exo_state: Int): MediaPlayerState {
    return MediaPlayerState.values()[exo_state - 1]
}

fun ExoMediaItem.getSong(): Song {
    return SongRef(mediaMetadata.artworkUri.toString())
}
