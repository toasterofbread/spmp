package com.spectre7.spmp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.api.getSongLyrics
import kotlin.concurrent.thread

class SongLyricsHolder(private val song: Song) {
    var lyrics: Song.Lyrics? by mutableStateOf(null)
    var loading: Boolean by mutableStateOf(false)
    var loaded: Boolean by mutableStateOf(false)

    private val load_lock = Object()
    private var loading_data: Pair<Int, Song.Lyrics.Source>? by mutableStateOf(null)
    private var load_thread: Thread? = null

    fun loadAndGet(): Song.Lyrics? {
        synchronized(load_lock) {
            val data = song.song_reg_entry.getLyricsData()
            if (loading) {
                if (loading_data != data) {
                    load_thread!!.interrupt()
                }
                else {
                    return lyrics
                }
            }
            else if (loaded && loading_data == data) {
                return lyrics
            }

            loading = true
            loaded = false
            loading_data = data
            lyrics = null


            song.onLoaded {
                synchronized(load_lock) {
                    if (data != loading_data) {
                        return@onLoaded
                    }

                    load_thread = thread {
                        val result = getSongLyrics(song, data)

                        synchronized(load_lock) {
                            if (Thread.interrupted()) {
                                return@thread
                            }
                            loaded = true
                            loading = false
                            lyrics = result
                        }
                    }
                }
            }
        }
        return lyrics
    }
}
