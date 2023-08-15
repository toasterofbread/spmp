package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.spmp.api.lyrics.LyricsSource
import com.toasterofbread.spmp.api.lyrics.loadLyrics
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.PlatformContext
import kotlinx.coroutines.Deferred
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock

internal object SongLyricsLoader {
    private val lock = ReentrantLock()

    private val loaded_by_reference: MutableMap<LyricsReference, WeakReference<SongLyrics>> = mutableStateMapOf()

    private val loading_by_id: MutableMap<String, Deferred<Result<SongLyrics>>> = mutableStateMapOf()
    private val loading_by_reference: MutableMap<LyricsReference, Deferred<Result<SongLyrics>>> = mutableStateMapOf()

    fun getLoadedByLyrics(reference: LyricsReference?): SongLyrics? {
        return loaded_by_reference[reference]?.get()
    }

    suspend fun loadBySong(
        song: Song,
        context: PlatformContext
    ): Result<SongLyrics> {
        val lyrics_reference: LyricsReference? = song.Lyrics.get(context.database)
        if (lyrics_reference != null) {
            return loadByLyrics(lyrics_reference)
        }

        return performSafeLoad(
            song.id,
            lock,
            loading_by_id
        ) {
            val result = LyricsSource.searchSongLyricsByPriority(song, context.database)
            result.onSuccess { lyrics ->
                loaded_by_reference[lyrics.reference] = WeakReference(lyrics)
                song.Lyrics.set(lyrics.reference, context.database)
            }
        }
    }

    suspend fun loadByLyrics(lyrics_reference: LyricsReference): Result<SongLyrics> {
        val loaded = loaded_by_reference[lyrics_reference]?.get()
        if (loaded != null) {
            return Result.success(loaded)
        }

        return performSafeLoad(
            lyrics_reference,
            lock,
            loading_by_reference
        ) {
            val result = loadLyrics(lyrics_reference)
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

    fun getItemState(song: Song, db: Database): ItemState =
        object : ItemState {
            private var song_lyrics_reference: LyricsReference? by mutableStateOf(song.Lyrics.get(db))
            init {
                db.songQueries.lyricsById(song.id).addListener {
                    song_lyrics_reference = song.Lyrics.get(db)
                }
            }

            override val song_id: String = song.id
            override val lyrics: SongLyrics?
                get() = loaded_by_reference[song_lyrics_reference]?.get()
            override val loading: Boolean
                get() = loading_by_id.containsKey(song_id) || loading_by_reference.containsKey(song_lyrics_reference)

            override fun toString(): String =
                "LyricsItemState(id=$song_id, loading=$loading, lyrics=${lyrics?.reference})"
        }
}
