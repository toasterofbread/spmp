package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import dev.toastbits.spms.socketapi.shared.SpMsPlayerEvent
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import PlatformIO

internal suspend fun SpMsPlayerService.applyPlayerEvents(events: List<SpMsPlayerEvent>) = withContext(Dispatchers.PlatformIO) {
    var item_transition_event: SpMsPlayerEvent? = null

    for (event in events) {
        try {
            if (event.type == SpMsPlayerEvent.Type.ITEM_TRANSITION) {
                item_transition_event = event
                continue
            }
            applyEvent(event)
        }
        catch (e: Throwable) {
            throw RuntimeException("Processing event $event failed", e)
        }
    }

    if (item_transition_event != null) {
        applyEvent(item_transition_event)
    }
}

private fun SpMsPlayerService.applyEvent(event: SpMsPlayerEvent) {
    println("Applying event $event")

    when (event.type) {
        SpMsPlayerEvent.Type.ITEM_TRANSITION -> {
            val target_index: Int = event.properties["index"]!!.int
            if (target_index == _current_item_index) {
                return
            }

            _current_item_index = target_index
            _duration_ms = -1
            updateCurrentSongPosition(0)

            val song: Song? = if (_current_item_index < 0) null else getSong(_current_item_index)
            listeners.forEach {
                it.onSongTransition(song, false)
                it.onEvents()
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

            if (index <= _current_item_index) {
                _current_item_index++
            }

            playlist.add(minOf(playlist.size, index), song)
            listeners.forEach {
                it.onSongAdded(index, song)
                it.onEvents()
            }
            service_player.session_started = true
        }
        SpMsPlayerEvent.Type.ITEM_REMOVED -> {
            val index: Int = event.properties["index"]!!.int
            if (index !in playlist.indices) {
                return
            }

            val song: Song = playlist.removeAt(index)
            val transitioning: Boolean = index == _current_item_index

            if (index <= _current_item_index) {
                _current_item_index--
            }

            listeners.forEach {
                it.onSongRemoved(index, song)
                if (transitioning) {
                    it.onSongTransition(playlist.getOrNull(_current_item_index), false)
                }
                it.onEvents()
            }
        }
        SpMsPlayerEvent.Type.ITEM_MOVED -> {
            val to: Int = event.properties["to"]!!.int
            val from: Int = event.properties["from"]!!.int

            val song: Song = playlist.removeAt(from)
            playlist.add(to, song)

            if (from == _current_item_index) {
                _current_item_index = to
            }

            listeners.forEach {
                it.onSongMoved(from, to)
                it.onEvents()
            }
        }
        SpMsPlayerEvent.Type.CLEARED -> {
            for (i in playlist.indices.reversed()) {
                val song: Song = playlist.removeAt(i)
                listeners.forEach {
                    it.onSongRemoved(i, song)
                }
            }
            listeners.forEach {
                it.onEvents()
            }
        }
        SpMsPlayerEvent.Type.CANCEL_RADIO -> {
            onRadioCancelRequested()
        }
        SpMsPlayerEvent.Type.PROPERTY_CHANGED -> {
            val key: String = event.properties["key"]!!.content
            val value: JsonPrimitive = event.properties["value"]!!
            when (key) {
                "state" -> {
                    if (value.int != _state.ordinal) {
                        _state = SpMsPlayerState.entries[value.int]
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
                        _repeat_mode = SpMsPlayerRepeatMode.entries[value.int]
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
        SpMsPlayerEvent.Type.READY_TO_PLAY -> {}
    }
}
