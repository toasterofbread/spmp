package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.*
import app.cash.sqldelight.Query
import dev.toastbits.composekit.platform.PlatformFile
import com.toasterofbread.spmp.model.lyrics.*
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.lyrics.*
import java.lang.ref.WeakReference
import kotlinx.coroutines.*

internal object SongLyricsLoader: Loader<SongLyrics>() {
    private val loaded_by_reference: MutableMap<LyricsReference, WeakReference<SongLyrics>> = mutableStateMapOf()
    private val loaded_by_song: MutableMap<String, WeakReference<SongLyrics>> = mutableStateMapOf()

    private val loading_by_id: MutableMap<String, LoadJob<Result<SongLyrics>>> = mutableStateMapOf()
    private val loading_by_reference: MutableMap<LyricsReference, LoadJob<Result<SongLyrics>>> = mutableStateMapOf()

    fun getLoadedByLyrics(reference: LyricsReference?): SongLyrics? {
        return loaded_by_reference[reference]?.get()
    }

    suspend fun loadBySong(
        song: Song,
        context: AppContext
    ): Result<SongLyrics>? {
        loaded_by_song[song.id]?.get()?.also {
            return Result.success(it)
        }

        val local_file: PlatformFile? = MediaItemLibrary.getLocalLyrics(context, song)
        if (local_file != null) {
            val lyrics: SongLyrics? = LyricsFileConverter.loadFromFile(local_file, context)?.second
            if (lyrics != null) {
                val ref = WeakReference(lyrics)
                loaded_by_song[song.id] = ref
                loaded_by_reference[lyrics.reference] = ref
                return Result.success(lyrics)
            }
        }

        val lyrics_reference: LyricsReference? = song.Lyrics.get(context.database)
        if (lyrics_reference != null) {
            if (lyrics_reference.isNone()) {
                return null
            }
            return loadByLyrics(lyrics_reference, context)
        }

        return performSafeLoad(
            song.id,
            loading_by_id
        ) {
            val result: Result<SongLyrics> = LyricsSource.searchSongLyricsByPriority(song, context)
            result.onSuccess { lyrics ->
                loaded_by_reference[lyrics.reference] = WeakReference(lyrics)
                song.Lyrics.set(lyrics.reference, context.database)
            }
        }
    }

    suspend fun loadByLyrics(lyrics_reference: LyricsReference, context: AppContext): Result<SongLyrics> {
        require(!lyrics_reference.isNone())

        val loaded: SongLyrics? = withContext(Dispatchers.Main) { loaded_by_reference[lyrics_reference]?.get() }
        if (loaded != null) {
            return Result.success(loaded)
        }

        return performSafeLoad(
            lyrics_reference,
            lock,
            loading_by_reference
        ) {
            val result: Result<SongLyrics> = loadLyrics(lyrics_reference, context)
            result.onSuccess { lyrics ->
                loaded_by_reference[lyrics_reference] = WeakReference(lyrics)
            }
        }
    }

    interface ItemState {
        val song_id: String
        val lyrics: SongLyrics?
        val loading: Boolean
        val is_none: Boolean
    }

    @Composable
    fun rememberItemState(song: Song, context: AppContext): ItemState {
        var song_lyrics_reference: LyricsReference? by remember { mutableStateOf(song.Lyrics.get(context.database)) }
        val state: ItemState = remember(song.id) {
            object : ItemState {
                override val song_id: String = song.id
                override val lyrics: SongLyrics?
                    get() =
                        try {
                            loaded_by_song[song_id]?.get() ?: loaded_by_reference[song_lyrics_reference]?.get()
                        }
                        catch (_: IllegalStateException) { null }
                override val loading: Boolean
                    get() = loading_by_id.containsKey(song_id) || loading_by_reference.containsKey(song_lyrics_reference)
                override val is_none: Boolean
                    get() = song_lyrics_reference?.isNone() == true

                override fun toString(): String =
                    "LyricsItemState(id=$song_id, loading=$loading, lyrics=${lyrics?.reference})"
            }
        }

        DisposableEffect(song.id) {
            song_lyrics_reference = song.Lyrics.get(context.database)

            val listener: Query.Listener = Query.Listener {
                try {
                    song_lyrics_reference = song.Lyrics.get(context.database)
                }
                catch (_: IllegalStateException) {}
            }
            context.database.songQueries.lyricsById(song.id).addListener(listener)

            onDispose {
                context.database.songQueries.lyricsById(song.id).addListener(listener)
            }
        }

        return state
    }
}
