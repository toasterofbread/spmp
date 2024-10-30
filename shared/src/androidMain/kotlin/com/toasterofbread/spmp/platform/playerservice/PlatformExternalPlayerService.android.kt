package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import androidx.media3.common.Player
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import ProgramArguments
import LocalProgramArguments
import LocalPlayerState
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.loading_splash_button_launch_without_server

actual class PlatformExternalPlayerService: ForegroundPlayerService(play_when_ready = false), PlayerService {
    private var target_playing: Boolean = false
    private var target_seek: Long? = null

    private val server: ExternalPlayerService =
        object : ExternalPlayerService(plays_audio = true) {
            override fun createServicePlayer(): PlayerServicePlayer = this@PlatformExternalPlayerService.service_player
        }

    private val server_listener: PlayerListener =
        object : PlayerListener() {
            override fun onSongAdded(index: Int, song: Song) = this@PlatformExternalPlayerService.onSongAdded(index, song)
            override fun onPlayingChanged(is_playing: Boolean) = this@PlatformExternalPlayerService.onPlayingChanged(is_playing)
            override fun onSeeked(position_ms: Long) = this@PlatformExternalPlayerService.onSeeked(position_ms)
            override fun onSongMoved(from: Int, to: Int) = this@PlatformExternalPlayerService.onSongMoved(from, to)
            override fun onSongRemoved(index: Int, song: Song) = this@PlatformExternalPlayerService.onSongRemoved(index)
            override fun onSongTransition(song: Song?, manual: Boolean) = this@PlatformExternalPlayerService.onSongTransition(current_song_index)
        }

    @Composable
    override fun PersistentContent(requestServiceChange: (PlayerServiceCompanion) -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        val launch_arguments: ProgramArguments = LocalProgramArguments.current
        val ui_only: Boolean by player.settings.platform.EXTERNAL_SERVER_MODE_UI_ONLY.observe()
        LaunchedEffect(ui_only) {
            if (ui_only && PlatformExternalPlayerService.isAvailable(player.context, launch_arguments)) {
                requestServiceChange(PlatformExternalPlayerService.Companion)
            }
        }
    }

    @Composable
    override fun LoadScreenExtraContent(item_modifier: Modifier, requestServiceChange: (PlayerServiceCompanion) -> Unit) {
        val launch_arguments: ProgramArguments = LocalProgramArguments.current
        val internal_service_available: Boolean = remember(launch_arguments) { PlatformInternalPlayerService.Companion.isAvailable(context, launch_arguments) }

        if (internal_service_available) {
            Button(
                {
                    requestServiceChange(PlatformInternalPlayerService.Companion)
                },
                item_modifier
            ) {
                Text(stringResource(Res.string.loading_splash_button_launch_without_server))
            }
        }
    }

    override fun onRadioCancelled() {
        super.onRadioCancelled()
        server.onRadioCancelled()
    }

    @OptIn(UnstableApi::class)
    override fun getNotificationPlayer(player: Player): Player =
        object : ForwardingPlayer(player) {
            override fun play() {
                server.play()
            }

            override fun pause() {
                server.pause()
            }

            override fun seekToNext() {
                server.seekToNext()
            }

            override fun seekToNextMediaItem() {
                server.seekToNext()
            }

            override fun seekToPrevious() {
                server.seekToPrevious()
            }

            override fun seekToPreviousMediaItem() {
                server.seekToPrevious()
            }

            override fun seekTo(index: Int, position_ms: Long) {
                server.seekToSong(index)
                server.seekTo(position_ms)
            }
        }

    private val player_listener: Player.Listener =
        object : Player.Listener {
            private var last_seek_position: Long? = null

            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && player.currentMediaItemIndex != current_song_index) {
                    server.seekToSong(player.currentMediaItemIndex)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying == target_playing || player.playbackState != Player.STATE_READY) {
                    return
                }

                if (isPlaying) {
                    server.play()
                }
                else {
                    server.pause()
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (newPosition.positionMs == target_seek) {
                    return
                }

                if (reason == Player.DISCONTINUITY_REASON_SEEK && newPosition.positionMs != last_seek_position) {
                    last_seek_position = newPosition.positionMs
                    pause()
                    server.seekTo(newPosition.positionMs)

                    if (player.playbackState == Player.STATE_READY) {
                        onPlaybackReady()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onPlaybackReady()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()

        player.addListener(player_listener)

        server._context = context
        server.addListener(server_listener)
        server.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        server.onDestroy()
    }

    private fun onSongAdded(index: Int, song: Song) { coroutine_scope.launch(Dispatchers.Main) {
        super.addSong(song, index)
    }}
    private fun onPlayingChanged(is_playing: Boolean) { coroutine_scope.launch(Dispatchers.Main) {
        target_playing = is_playing
        if (is_playing) super.play()
        else super.pause()
    }}
    private fun onSeeked(position_ms: Long) { coroutine_scope.launch(Dispatchers.Main) {
        target_seek = position_ms
        super.seekTo(position_ms)
    }}
    private fun onSongMoved(from: Int, to: Int) { coroutine_scope.launch(Dispatchers.Main) {
        super.moveSong(from, to)
    }}
    private fun onSongRemoved(index: Int) { coroutine_scope.launch(Dispatchers.Main) {
        super.removeSong(index)
    }}
    private fun onSongTransition(index: Int) { coroutine_scope.launch(Dispatchers.Main) {
        if (index < 0 || index == player.currentMediaItemIndex) {
            return@launch
        }
        try {
            super.seekToSong(index)
        }
        catch (e: Throwable) {
            throw RuntimeException("seekToSong($index) failed", e)
        }
    }}

    private fun onPlaybackReady() {
        server.notifyReadyToPlay(super.duration_ms)
    }

    override val load_state: PlayerServiceLoadState get() = server.load_state
    override val state: SpMsPlayerState get() = server.state
    override val is_playing: Boolean get() = server.is_playing
    override val song_count: Int get() = server.song_count
    override val current_song_index: Int get() = server.current_song_index
    override val current_position_ms: Long get() = server.current_position_ms
    override val duration_ms: Long get() = server.duration_ms
    override val has_focus: Boolean get() = server.has_focus
    override val radio_instance: RadioInstance get() = server.radio_instance
    override var repeat_mode: SpMsPlayerRepeatMode
        get() = server.repeat_mode
        set(value) { server.repeat_mode = value }
    override var volume: Float
        get() = server.volume
        set(value) { server.volume = value }

    override fun play() = server.play()
    override fun pause() = server.pause()
    override fun playPause() = server.playPause()
    override fun seekTo(position_ms: Long) = server.seekTo(position_ms)
    override fun seekToSong(index: Int) = server.seekToSong(index)
    override fun seekToNext() = server.seekToNext()
    override fun seekToPrevious() = server.seekToPrevious()
    override fun getSong(): Song? = server.getSong()
    override fun getSong(index: Int): Song? = server.getSong(index)
    override fun addSong(song: Song, index: Int) = server.addSong(song, index)
    override fun moveSong(from: Int, to: Int) = server.moveSong(from, to)
    override fun removeSong(index: Int) = server.removeSong(index)
    override fun addListener(listener: PlayerListener) = server.addListener(listener)
    override fun removeListener(listener: PlayerListener) = server.removeListener(listener)

    actual companion object: InternalPlayerServiceCompanion(PlatformExternalPlayerService::class), PlayerServiceCompanion {
        override fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean = true

        override fun isServiceRunning(context: AppContext): Boolean = true
        override fun playsAudio(): Boolean = true

        override suspend fun connect(
            context: AppContext,
            launch_arguments: ProgramArguments,
            instance: PlayerService?,
            onConnected: (PlayerService) -> Unit,
            onDisconnected: () -> Unit
        ): Any {
            if (context.settings.platform.EXTERNAL_SERVER_MODE_UI_ONLY.get()) {
                require(instance is ExternalPlayerService?)
                val service: ExternalPlayerService =
                    if (instance != null) instance.also { it.setContext(context) }
                    else HeadlessExternalPlayerService().also {
                        it.setContext(context)
                        it.onCreate()
                    }
                onConnected(service)
                return service
            }
            else {
                return super.connect(context, launch_arguments, instance, onConnected, onDisconnected)
            }
        }

        override fun disconnect(context: AppContext, connection: Any) {
            if (connection is ExternalPlayerService) {
                connection.onDestroy()
            }
            else {
                super.disconnect(context, connection)
            }
        }
    }
}
