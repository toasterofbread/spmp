package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.SongData
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

internal fun ZmqSpMsPlayerService.applyPlayerEvent(event: SpMsPlayerEvent) {
    when (event.type) {
        SpMsPlayerEvent.Type.ITEM_TRANSITION -> {
            _current_song_index = event.properties["index"]!!.int
            _duration_ms = -1
            updateCurrentSongPosition(0)
            listeners.forEach {
                it.onSongTransition(getSong(_current_song_index), false)
                it.onEvents()
            }
        }
        SpMsPlayerEvent.Type.PROPERTY_CHANGED -> {
            val key: String = event.properties["key"]!!.content
            val value: JsonPrimitive = event.properties["value"]!!
            when (key) {
                "state" -> {
                    if (value.int != _state.ordinal) {
                        _state = MediaPlayerState.values()[value.int]
                        listeners.forEach {
                            it.onStateChanged(_state)
                            it.onEvents()
                        }
                    }
                }
                "is_playing" -> {
                    if (value.boolean != _is_playing) {
                        updateIsPlaying(value.boolean)
                        listeners.forEach {
                            it.onPlayingChanged(_is_playing)
                            it.onEvents()
                        }
                    }
                }
                "repeat_mode" -> {
                    if (value.int != _repeat_mode.ordinal) {
                        _repeat_mode = MediaPlayerRepeatMode.values()[value.int]
                        listeners.forEach {
                            it.onRepeatModeChanged(_repeat_mode)
                            it.onEvents()
                        }
                    }
                }
                "volume" -> {
                    if (value.float != _volume) {
                        _volume = value.float
                        listeners.forEach {
                            it.onVolumeChanged(_volume)
                            it.onEvents()
                        }
                    }
                }
                "duration_ms" -> {
                    if (value.long != _duration_ms) {
                        _duration_ms = value.long
                        listeners.forEach {
                            it.onDurationChanged(_duration_ms)
                            it.onEvents()
                        }
                    }
                }
                else -> throw NotImplementedError(key)
            }
        }
        SpMsPlayerEvent.Type.SEEKED -> {
            val position_ms: Long = event.properties["position_ms"]!!.long
            updateCurrentSongPosition(position_ms)
            listeners.forEach {
                it.onSeeked(position_ms)
                it.onEvents()
            }
        }
        SpMsPlayerEvent.Type.ITEM_ADDED -> {
            val song: SongData = SongData(event.properties["item_id"]!!.content)
            val index: Int = event.properties["index"]!!.int
            playlist.add(index, song)
            listeners.forEach {
                it.onSongAdded(index, song)
                it.onEvents()
            }
            service_player.session_started = true
        }
        SpMsPlayerEvent.Type.ITEM_REMOVED -> {
            val index: Int = event.properties["index"]!!.int
            playlist.removeAt(index)
            listeners.forEach {
                it.onSongRemoved(index)
                it.onEvents()
            }
        }
        SpMsPlayerEvent.Type.ITEM_MOVED -> {
            val to: Int = event.properties["to"]!!.int
            val from: Int = event.properties["from"]!!.int
            playlist.add(to, playlist.removeAt(from))
            listeners.forEach {
                it.onSongMoved(from, to)
                it.onEvents()
            }
        }
        else -> throw NotImplementedError(event.toString())
    }
}
