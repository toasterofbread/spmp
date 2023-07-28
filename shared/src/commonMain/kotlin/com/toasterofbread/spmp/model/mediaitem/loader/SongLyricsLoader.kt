package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.mutableStateMapOf
import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.spmp.api.lyrics.toLyricsReference
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.PlatformContext
import kotlinx.coroutines.Deferred
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock

internal object SongLyricsLoader {
    private val lock = ReentrantLock()

    private val loaded_by_id: MutableMap<String, WeakReference<SongLyrics>> = mutableStateMapOf()
    private val loaded_by_reference: MutableMap<LyricsReference, WeakReference<SongLyrics>> = mutableStateMapOf()

    private val loading_by_id: MutableMap<String, Deferred<SongLyrics>> = mutableStateMapOf()
    private val loading_by_reference: MutableMap<LyricsReference, Deferred<SongLyrics>> = mutableStateMapOf()

    suspend fun loadBySong(
        song: Song,
        context: PlatformContext
    ): Result<SongLyrics> {
        val lyrics_reference = context.database.songQueries
            .lyricsById(song.id).executeAsOneOrNull()
            .toLyricsReference()

        if (lyrics_reference != null) {
            return loadByLyrics(lyrics_reference)
        }

        val loaded = loaded_by_id[song.id]?.get()
        if (loaded != null) {
            return Result.success(loaded)
        }

        return performResultSafeLoad(
            song.id,
            lock,
            loading_by_id
        ) {
            val result = com.toasterofbread.spmp.api.lyrics.searchAndLoadSongLyrics(song, context.database)
            result.onSuccess { lyrics ->
                context.database.songQueries.updateLyricsById(
                    lyrics.reference.source_idx.toLong(),
                    lyrics.reference.id,
                    song.id
                )

                loaded_by_id[song.id] = WeakReference(lyrics)
            }
        }
    }

    suspend fun loadByLyrics(lyrics_reference: LyricsReference): Result<SongLyrics> {
        val loaded = loaded_by_reference[lyrics_reference]?.get()
        if (loaded != null) {
            return Result.success(loaded)
        }

        return performResultSafeLoad(
            lyrics_reference,
            lock,
            loading_by_reference
        ) {
            val result = com.toasterofbread.spmp.api.lyrics.loadLyrics(lyrics_reference)
            result.onSuccess { lyrics ->
                loaded_by_reference[lyrics_reference] = WeakReference(lyrics)
            }
        }
    }

    interface ItemState {
        val song_id: String
        val lyrics: SongLyrics?
        val loading: Boolean
    }

    fun getItemState(song_id: String): ItemState =
        object : ItemState {
            override val song_id: String = song_id
            override val lyrics: SongLyrics?
                get() = loaded_by_id[song_id]?.get()
            override val loading: Boolean
                get() = loading_by_id.contains(song_id)
        }
}
