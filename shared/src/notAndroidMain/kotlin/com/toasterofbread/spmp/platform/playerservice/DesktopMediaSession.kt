package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.db.Database
import dev.toastbits.mediasession.MediaSession
import dev.toastbits.mediasession.MediaSessionMetadata
import dev.toastbits.mediasession.MediaSessionPlaybackStatus
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.coroutines.launch

internal fun createDesktopMediaSession(service: PlayerService): MediaSession? {
    val session: MediaSession? =
        try {
            MediaSession.create(
                getPositionMs = { service.current_position_ms }
            )
        }
        catch (e: Throwable) {
            RuntimeException("Ignoring exception during MediaSession creation", e).printStackTrace()
            return null
        }

    if (session == null) {
        return null
    }

    session.setIdentity("spmp")

    val listener: PlayerListener =
        object : PlayerListener() {
            override fun onSongTransition(song: Song?, manual: Boolean) {
                session.onPositionChanged()
                session.onSongChanged(song, service)
            }
            override fun onPlayingChanged(is_playing: Boolean) {
                session.setPlaybackStatus(
                    if (is_playing) MediaSessionPlaybackStatus.PLAYING
                    else MediaSessionPlaybackStatus.PAUSED
                )
            }
            override fun onDurationChanged(duration_ms: Long) {
                session.setMetadata(
                    session.metadata.copy(
                        length_ms = duration_ms
                    )
                )
            }
            override fun onSeeked(position_ms: Long) {
                session.onPositionChanged()
            }
        }
    service.addListener(listener)

    listener.onSongTransition(service.getSong(), true)
    listener.onPlayingChanged(service.is_playing)
    listener.onDurationChanged(service.duration_ms)

    session.onPlayPause = {
        service.playPause()
    }
    session.onPlay = {
        service.play()
    }
    session.onPause = {
        service.pause()
    }
    session.onNext = {
        service.seekToNext()
    }
    session.onPrevious = {
        service.seekToPrevious()
    }
    session.onSeek = { by_ms ->
        service.seekTo(service.current_position_ms + by_ms)
    }
    session.onSetPosition = { to_ms ->
        service.seekTo(to_ms)
    }

    try {
        session.setEnabled(true)
    }
    catch (e: Throwable) {
        RuntimeException("Ignoring exception when enabling media session", e).printStackTrace()
        return null
    }

    return session
}

private fun MediaSession.onSongChanged(song: Song?, service: PlayerService) {
    val db: Database = service.context.database
    val album: Playlist? = song?.Album?.get(db)
    val album_items: List<Song>? = album?.Items?.get(db)

    service.context.coroutine_scope.launch {
        setMetadata(
            MediaSessionMetadata(
                length_ms = service.duration_ms,
                art_url = song?.ThumbnailProvider?.get(db)?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH),
                album = album?.getActiveTitle(db),
                album_artists = album?.Artists?.get(db)?.firstOrNull()?.getActiveTitle(db)?.let { listOf(it) },
                artist = song?.Artists?.get(db)?.firstOrNull()?.getActiveTitle(db),
                title = song?.getActiveTitle(db),
                url = song?.getUrl(service.context),
                use_count = song?.getPlayCount(db),
                track_number = album_items?.indexOfFirst { it.id == song?.id }
            )
        )
    }
}
