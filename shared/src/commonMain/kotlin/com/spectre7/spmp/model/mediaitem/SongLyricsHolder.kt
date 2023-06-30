package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.api.getSongLyrics
import com.toasterofbread.spmp.model.SongLyrics
import kotlinx.coroutines.*

class SongLyricsHolder(private val song: Song) {
    var lyrics: SongLyrics? by mutableStateOf(null)
    var loading: Boolean by mutableStateOf(false)
    var loaded: Boolean by mutableStateOf(false)

    private val load_lock = Object()

    suspend fun loadAndGet(): Result<SongLyrics> = withContext(Dispatchers.IO) {
        synchronized(load_lock) {
            if (loading) {
                load_lock.wait()
                return@withContext lyrics?.let { Result.success(it) } ?: Result.failure(RuntimeException("Lyrics not loaded"))
            }
            loading = true
        }

        coroutineContext.job.invokeOnCompletion {
            synchronized(load_lock) {
                loading = false
                load_lock.notifyAll()
            }
        }

        val result = getSongLyrics(song, song.song_reg_entry.getLyricsData())
        synchronized(load_lock) {
            loaded = true
            lyrics = result.getOrNull()
        }

        return@withContext result
    }
}
