package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState

actual class PlatformExternalPlayerService: ForegroundPlayerService(), PlayerService {
    private val service: ExternalPlayerService = ExternalPlayerService()
    
    private val service_listener = object : PlayerListener() {
        override fun onSongAdded(index: Int, song: Song) = this@PlatformExternalPlayerService.onSongAdded(index, song)
        override fun onPlayingChanged(is_playing: Boolean) = this@PlatformExternalPlayerService.onPlayingChanged(is_playing)
        override fun onSeeked(position_ms: Long) = this@PlatformExternalPlayerService.onSeeked(position_ms)
        override fun onSongMoved(from: Int, to: Int) = this@PlatformExternalPlayerService.onSongMoved(from, to)
        override fun onSongRemoved(index: Int) = this@PlatformExternalPlayerService.onSongRemoved(index)
        override fun onSongTransition(song: Song?, manual: Boolean) = this@PlatformExternalPlayerService.onSongTransition(song, manual)
    }
    
    override fun onCreate() {
        super.onCreate()

        service._context = context
        service.addListener(service_listener)

        service.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        service.onDestroy()
    }
    
    private fun onSongAdded(index: Int, song: Song) { coroutine_scope.launch(Dispatchers.Main) {
        super.addSong(song, index)
    }}
    private fun onPlayingChanged(is_playing: Boolean) { coroutine_scope.launch(Dispatchers.Main) {
        if (is_playing) super.play()
        else super.pause()
    }}
    private fun onSeeked(position_ms: Long) { coroutine_scope.launch(Dispatchers.Main) {
        super.seekTo(position_ms)
    }}
    private fun onSongMoved(from: Int, to: Int) { coroutine_scope.launch(Dispatchers.Main) {
        super.moveSong(from, to)
    }}
    private fun onSongRemoved(index: Int) { coroutine_scope.launch(Dispatchers.Main)     {
        super.removeSong(index)
    }}
    private fun onSongTransition(song: Song?, manual: Boolean) { coroutine_scope.launch(Dispatchers.Main)     {
        super.seekToSong(current_song_index)
    }}

    override val load_state: PlayerServiceLoadState get() = service.load_state
    override val state: SpMsPlayerState get() = service.state
    override val is_playing: Boolean get() = service.is_playing
    override val song_count: Int get() = service.song_count
    override val current_song_index: Int get() = service.current_song_index
    override val current_position_ms: Long get() = service.current_position_ms
    override val duration_ms: Long get() = service.duration_ms
    override val has_focus: Boolean get() = service.has_focus
    override val radio_state: RadioInstance.RadioState get() = service.radio_state
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
