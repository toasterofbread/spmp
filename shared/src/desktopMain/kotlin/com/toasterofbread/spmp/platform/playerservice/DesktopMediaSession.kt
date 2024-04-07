package com.toasterofbread.spmp.platform.playerservice

import io.github.selemba1000.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.db.Database
import spms.socketapi.shared.SpMsPlayerRepeatMode

internal fun createDesktopMediaSession(service: PlatformPlayerService): Boolean {
    println("START SESSION")

    val session: JMTC =
        try {
            JMTC.getInstance(JMTCSettings("spmp", "spmp"))
        }
        catch (e: Throwable) {
            e.printStackTrace()
            return false
        }

    session.setEnabledButtons(
        JMTCEnabledButtons(
            true, // isPlayEnabled
            true, // isPauseEnabled
            false, // isStopEnabled
            true, // isNextEnabled
            true // isPreviousEnabled
        )
    )

    session.enabled = true
    session.setMediaType(JMTCMediaType.Music)
    session.setPlayingState(JMTCPlayingState.PAUSED)
    session.updateDisplay()

    val listener: PlayerListener =
        object : PlayerListener() {
            override fun onSongTransition(song: Song?, manual: Boolean) {
                session.onSongChanged(song, service.context)
                session.updateDisplay()
            }
            override fun onPlayingChanged(is_playing: Boolean) {
                session.setPlayingState(
                    if (is_playing) JMTCPlayingState.PLAYING
                    else JMTCPlayingState.PAUSED
                )
                session.updateDisplay()
            }
            override fun onDurationChanged(duration_ms: Long) {
                session.setTimelineProperties(
                    JMTCTimelineProperties(
                        0, // start
                        duration_ms, // end
                        0, // seekStart
                        duration_ms // seekEnd
                    )
                )
                session.updateDisplay()
            }
        }
    service.addListener(listener)

    listener.onSongTransition(service.getSong(), true)
    listener.onPlayingChanged(service.is_playing)
    listener.onDurationChanged(service.duration_ms)

    val callbacks: JMTCCallbacks =
        JMTCCallbacks().apply {
            onPlay = JMTCButtonCallback {
                service.play()
            }
            onPause = JMTCButtonCallback {
                service.pause()
            }
            onNext = JMTCButtonCallback {
                service.seekToNext()
            }
            onPrevious = JMTCButtonCallback {
                service.seekToPrevious()
            }
            onSeek = JMTCSeekCallback { time ->
                service.seekTo(time)
            }
            onLoop = JMTCValueChangedCallback<JMTCParameters.LoopStatus> { loop_status ->
                service.repeat_mode =
                    when (loop_status) {
                        JMTCParameters.LoopStatus.None -> SpMsPlayerRepeatMode.NONE
                        JMTCParameters.LoopStatus.Track -> SpMsPlayerRepeatMode.ONE
                        JMTCParameters.LoopStatus.Playlist -> SpMsPlayerRepeatMode.ALL
                    }
            }
        }
    session.setCallbacks(callbacks)

    return true
}

private fun JMTC.onSongChanged(song: Song?, context: AppContext) {
    val db: Database = context.database
    val album: Playlist? = song?.Album?.get(db)
    val album_items: List<Song>? = album?.Items?.get(db)

    setMediaProperties(
        JMTCMusicProperties(
            // title
            song?.getActiveTitle(db) ?: "",

            // artist
            song?.Artists?.get(db)?.firstOrNull()?.getActiveTitle(db) ?: "",

            // albumTitle
            album?.getActiveTitle(db) ?: "",

            // albumArtist
            album?.Artists?.get(db)?.firstOrNull()?.getActiveTitle(db) ?: "",

            // genres
            emptyArray(),

            // albumTracks
            album?.ItemCount?.get(db) ?: album_items?.size ?: 0,

            // track
            album_items?.indexOfFirst { it.id == song?.id } ?: 0,

            // art
            null
        )
    )
}
