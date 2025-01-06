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
import kotlin.time.Duration

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
            override fun onSongTransition(song: Song?, manual: Boolean) = this@PlatformExternalPlayerService.onSongTransition(current_item_index)
        }

    @Composable
    override fun PersistentContent(requestServiceChange: (PlayerServiceCompanion) -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        val launch_arguments: ProgramArguments = LocalProgramArguments.current
        val ui_only: Boolean by player.settings.Platform.EXTERNAL_SERVER_MODE_UI_ONLY.observe()
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
    override fun getNotificationPlayer(): PlayerService =
        object : ForwardingPlayerService(this) {
            override fun play() {
                server.play()
            }

            override fun pause() {
                server.pause()
            }

            override fun seekToNext(): Boolean {
                return server.seekToNext()
            }

            override fun seekToPrevious(repeat_threshold: Duration?): Boolean {
                return server.seekToPrevious(repeat_threshold)
            }

            override fun seekToTime(position_ms: Long) {
                server.seekToTime(position_ms)
            }

            override fun seekToItem(index: Int, position_ms: Long) {
                server.seekToItem(index, position_ms)
            }
        }

    private val player_listener: Player.Listener =
        object : Player.Listener {
            private var last_seek_position: Long? = null

            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && player.currentMediaItemIndex != current_item_index) {
                    server.seekToItem(player.currentMediaItemIndex)
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
                    server.seekToTime(newPosition.positionMs)

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
        super.seekToTime(position_ms)
    }}
    private fun onSongMoved(from: Int, to: Int) { coroutine_scope.launch(Dispatchers.Main) {
        super.moveItem(from, to)
    }}
    private fun onSongRemoved(index: Int) { coroutine_scope.launch(Dispatchers.Main) {
        super.removeItem(index)
    }}
    private fun onSongTransition(index: Int) { coroutine_scope.launch(Dispatchers.Main) {
        if (index < 0 || index == player.currentMediaItemIndex) {
            return@launch
        }
        try {
            super.seekToItem(index, 0)
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
    override val item_count: Int get() = server.item_count
    override val current_item_index: Int get() = server.current_item_index
    override val current_position_ms: Long get() = server.current_position_ms
    override val duration_ms: Long get() = server.duration_ms
    override val radio_instance: RadioInstance get() = server.radio_instance
    override val repeat_mode: SpMsPlayerRepeatMode
        get() = server.repeat_mode
    override val volume: Float
        get() = server.volume

    override fun play() = server.play()
    override fun pause() = server.pause()
    override fun playPause() = server.playPause()
    override fun seekToTime(position_ms: Long) = server.seekToTime(position_ms)
    override fun seekToItem(index: Int, position_ms: Long) = server.seekToItem(index, position_ms)
    override fun seekToNext() = server.seekToNext()
    override fun seekToPrevious(repeat_threshold: Duration?) = server.seekToPrevious(repeat_threshold)
    override fun getSong(): Song? = server.getSong()
    override fun getSong(index: Int): Song? = server.getSong(index)
    override fun addSong(song: Song, index: Int) = server.addSong(song, index)
    override fun moveItem(from: Int, to: Int) = server.moveItem(from, to)
    override fun removeItem(index: Int) = server.removeItem(index)
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
            if (context.settings.Platform.EXTERNAL_SERVER_MODE_UI_ONLY.get()) {
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
