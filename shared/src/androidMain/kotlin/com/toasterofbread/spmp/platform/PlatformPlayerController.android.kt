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
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.exovisualiser.ExoVisualizer
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceState
import com.toasterofbread.spmp.platform.playerservice.fromBundle
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import androidx.media3.common.MediaItem as ExoMediaItem

@UnstableApi
actual abstract class PlatformPlayerController {
    actual val context: PlatformContext get() = _context
    actual val service_state: PlayerServiceState get() = _service_state.value

    private lateinit var _context: PlatformContext
    private lateinit var player: Player
    private lateinit var sendCustomCommand: (SessionCommand) -> Unit
    private lateinit var _service_state: State<PlayerServiceState>

    fun init(context: PlatformContext, player: Player, service_state: State<PlayerServiceState>, sendCustomCommand: (SessionCommand) -> Unit) {
        _context = context
        this.player = player
        this.sendCustomCommand = sendCustomCommand
        _service_state = service_state

        listener.addToPlayer(player)

        if (ProjectBuildConfig.MUTE_PLAYER == true) {
            player.volume = 0f
        }
    }

    private val listener = object : Listener() {
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
    private val listeners: MutableList<Listener> = mutableListOf(listener)

    // Seek
    private var pending_seek_position: Long? = null

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
        val visualiser = remember { ExoVisualizer(PlatformPlayerService.audio_processor) }
        visualiser.Visualiser(colour, modifier, opacity)
    }

    actual var session_started: Boolean by mutableStateOf(false)

    actual val state: MediaPlayerState get() = convertState(player.playbackState)
    actual val is_playing: Boolean get() = player.isPlaying
    actual val song_count: Int get() = player.mediaItemCount
    actual val current_song_index: Int get() = player.currentMediaItemIndex
    actual val current_position_ms: Long get() = player.currentPosition
    actual val duration_ms: Long get() = player.duration
    actual val undo_count: Int get() = action_head
    actual val redo_count: Int get() = action_list.size - undo_count
    actual val radio: RadioInstance.RadioState get() = service_state.radio_state

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
        val add_index = if (index < 0) song_count else index
        if (add_index !in 0 .. song_count) {
            return
        }

        val item = song.buildExoMediaItem(context)
        performAction(AddAction(item, add_index))

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

    actual fun triggerSavePersistentQueue() {
        sendCustomCommand(PlayerServiceCommand.TriggerPersistentQueueSave.getSessionCommand())
    }

    actual fun setStopAfterCurrentSong(value: Boolean) {
        sendCustomCommand(
            PlayerServiceCommand.SetStopAfterCurrentSong.getSessionCommand(
                bundleOf("value" to value)
            )
        )
    }

    private fun release() {
        player.release()
        onDestroy()
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
                    SessionToken(ctx, ComponentName(ctx, PlayerService::class.java))
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

                    controller.init(context, media_controller, service_state) { command ->
                        media_controller.sendCustomCommand(command, Bundle.EMPTY)
                    }

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

fun ExoMediaItem.getSong(): Song =
    SongRef(localConfiguration?.uri?.toString() ?: mediaId)

@UnstableApi
private fun Song.buildExoMediaItem(context: PlatformContext): ExoMediaItem =
    ExoMediaItem.Builder()
        .setRequestMetadata(ExoMediaItem.RequestMetadata.Builder().setMediaUri(id.toUri()).build())
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
