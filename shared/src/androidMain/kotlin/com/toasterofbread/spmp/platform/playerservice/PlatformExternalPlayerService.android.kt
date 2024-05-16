package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState
import androidx.media3.common.Player
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual class PlatformExternalPlayerService: ForegroundPlayerService(play_when_ready = true), PlayerService {
    @Composable
    override fun LoadScreenExtraContent(modifier: Modifier) {
        Button({
            service = this
            server?.onDestroy()
            server = null
        }) {
            Text("LOCAL")
        }
    }

    override fun onRadioCancelled() {
        super.onRadioCancelled()
        server?.onRadioCancelled()
    }

    private var server: ExternalPlayerService? =
        object : ExternalPlayerService(plays_audio = true) {
            override fun createServicePlayer(): PlayerServicePlayer = this@PlatformExternalPlayerService.service_player
        }

    private var service: PlayerService = server!!

    private val server_listener: PlayerListener =
        object : PlayerListener() {
            override fun onSongAdded(index: Int, song: Song) = this@PlatformExternalPlayerService.onSongAdded(index, song)
            override fun onPlayingChanged(is_playing: Boolean) = this@PlatformExternalPlayerService.onPlayingChanged(is_playing)
            override fun onSeeked(position_ms: Long) = this@PlatformExternalPlayerService.onSeeked(position_ms)
            override fun onSongMoved(from: Int, to: Int) = this@PlatformExternalPlayerService.onSongMoved(from, to)
            override fun onSongRemoved(index: Int, song: Song) = this@PlatformExternalPlayerService.onSongRemoved(index)
            override fun onSongTransition(song: Song?, manual: Boolean) = this@PlatformExternalPlayerService.onSongTransition(current_song_index)
        }

    private val player_listener: Player.Listener =
        object : Player.Listener {
            private var last_seek_position: Long? = null

            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && player.currentMediaItemIndex != current_song_index) {
                    service.seekToSong(player.currentMediaItemIndex)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying == target_playing || player.playbackState != Player.STATE_READY) {
                    return
                }

                if (isPlaying) {
                    service.play()
                }
                else {
                    service.pause()
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (newPosition.positionMs == target_seek) {
                    return
                }

                if (reason == Player.DISCONTINUITY_REASON_SEEK && newPosition.positionMs != last_seek_position) {
                    last_seek_position = newPosition.positionMs
                    pause()
                    service.seekTo(newPosition.positionMs)

                    if (player.playbackState == Player.STATE_READY) {
                        server?.notifyReadyToPlay()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    server?.notifyReadyToPlay()
                }
            }
        }

    private var target_playing: Boolean = false
    private var target_seek: Long? = null

    override fun onCreate() {
        super.onCreate()

        player.addListener(player_listener)

        server?.apply {
            _context = context
            addListener(server_listener)
            onCreate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.onDestroy()
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

    override val load_state: PlayerServiceLoadState get() = service.load_state
    override val state: SpMsPlayerState get() = service.state
    override val is_playing: Boolean get() = service.is_playing
    override val song_count: Int get() = service.song_count
    override val current_song_index: Int get() = service.current_song_index
    override val current_position_ms: Long get() = service.current_position_ms
    override val duration_ms: Long get() = service.duration_ms
    override val has_focus: Boolean get() = service.has_focus
    override val radio_instance: RadioInstance get() = service.radio_instance
    override var repeat_mode: SpMsPlayerRepeatMode
        get() = service.repeat_mode
        set(value) { service.repeat_mode = value }
    override var volume: Float
        get() = service.volume
        set(value) { service.volume = value }

    override fun play() = service.play()
    override fun pause() = service.pause()
    override fun playPause() = service.playPause()
    override fun seekTo(position_ms: Long) = service.seekTo(position_ms)
    override fun seekToSong(index: Int) = service.seekToSong(index)
    override fun seekToNext() = service.seekToNext()
    override fun seekToPrevious() = service.seekToPrevious()
    override fun getSong(): Song? = service.getSong()
    override fun getSong(index: Int): Song? = service.getSong(index)
    override fun addSong(song: Song, index: Int) = service.addSong(song, index)
    override fun moveSong(from: Int, to: Int) = service.moveSong(from, to)
    override fun removeSong(index: Int) = service.removeSong(index)
    override fun addListener(listener: PlayerListener) = service.addListener(listener)
    override fun removeListener(listener: PlayerListener) = service.removeListener(listener)

    actual companion object: InternalPlayerServiceCompanion(PlatformExternalPlayerService::class), PlayerServiceCompanion {
        override fun isServiceRunning(context: AppContext): Boolean = true
    }
}
