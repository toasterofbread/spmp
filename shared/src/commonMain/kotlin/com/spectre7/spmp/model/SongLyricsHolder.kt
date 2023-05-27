package com.spectre7.spmp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.api.getSongLyrics
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.internal.wait
import java.util.Objects
import kotlin.concurrent.thread

class SongLyricsHolder(private val song: Song) {
    var lyrics: Song.Lyrics? by mutableStateOf(null)
    var loading: Boolean by mutableStateOf(false)
    var loaded: Boolean by mutableStateOf(false)

    private val load_lock = Object()

    suspend fun loadAndGet(): Song.Lyrics? {
        synchronized(load_lock) {
            if (loading) {
                load_lock.wait()
                return lyrics
            }
            loading = true
        }

        val data = song.song_reg_entry.getLyricsData()
        withContext(Dispatchers.IO) {
            coroutineContext.job.invokeOnCompletion {
                synchronized(load_lock) {
                    loading = false
                    load_lock.notifyAll()
                }
            }

            val result = getSongLyrics(song, data)
            synchronized(load_lock) {
                loaded = true
                lyrics = result
            }
        }

        return lyrics
    }
}
