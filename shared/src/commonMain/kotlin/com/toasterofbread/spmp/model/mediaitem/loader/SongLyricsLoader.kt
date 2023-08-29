package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.spmp.youtubeapi.lyrics.loadLyrics
import kotlinx.coroutines.Deferred
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock

internal object SongLyricsLoader: Loader<SongLyrics>() {
    private val loaded_by_reference: MutableMap<LyricsReference, WeakReference<SongLyrics>> = mutableStateMapOf()

    private val loading_by_id: MutableMap<String, LoadJob<Result<SongLyrics>>> = mutableStateMapOf()
    private val loading_by_reference: MutableMap<LyricsReference, LoadJob<Result<SongLyrics>>> = mutableStateMapOf()

    fun getLoadedByLyrics(reference: LyricsReference?): SongLyrics? {
        return loaded_by_reference[reference]?.get()
    }

    suspend fun loadBySong(
        song: Song,
        context: PlatformContext
    ): Result<SongLyrics> {
        val lyrics_reference: LyricsReference? = song.Lyrics.get(context.database)
        if (lyrics_reference != null) {
            return loadByLyrics(lyrics_reference, context)
        }

        return performSafeLoad(
            song.id,
            loading_by_id
        ) {
            val result = LyricsSource.searchSongLyricsByPriority(song, context)
            result.onSuccess { lyrics ->
                loaded_by_reference[lyrics.reference] = WeakReference(lyrics)
                song.Lyrics.set(lyrics.reference, context.database)
            }
        }
    }

    suspend fun loadByLyrics(lyrics_reference: LyricsReference, context: PlatformContext): Result<SongLyrics> {
        val loaded = loaded_by_reference[lyrics_reference]?.get()
        if (loaded != null) {
            return Result.success(loaded)
        }

        return performSafeLoad(
            lyrics_reference,
            lock,
            loading_by_reference
        ) {
            val result = loadLyrics(lyrics_reference, context)
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

    fun getItemState(song: Song, context: PlatformContext): ItemState =
        object : ItemState {
            private var song_lyrics_reference: LyricsReference? by mutableStateOf(song.Lyrics.get(context.database))
            init {
                context.database.songQueries.lyricsById(song.id).addListener {
                    song_lyrics_reference = song.Lyrics.get(context.database)
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
