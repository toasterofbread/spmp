package com.toasterofbread.spmp.model.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

@Composable
fun loadLyricsOnSongChange(song: Song?, context: AppContext, load_lyrics: Boolean = true): SongLyrics? {
    val db: Database = context.database
    val coroutine_scope = rememberCoroutineScope()
    var current_song: Song? by remember { mutableStateOf(null) }

    var lyrics: SongLyrics? by remember {
        mutableStateOf(
            if (song != null) SongLyricsLoader.getLoadedByLyrics(song.Lyrics.get(db))
            else null
        )
    }

    val lyrics_listener = remember {
        Query.Listener {
            if (song == null) {
                return@Listener
            }

            val reference = song.Lyrics.get(db)
            if (lyrics != null && lyrics?.reference == reference) {
                return@Listener
            }

            lyrics = null

            if (load_lyrics) {
                coroutine_scope.coroutineContext.cancelChildren()
                coroutine_scope.launch {
                    val result: Result<SongLyrics>? =
                        if (reference != null) SongLyricsLoader.loadByLyrics(reference, context)
                        else SongLyricsLoader.loadBySong(song, context)

                    result?.onSuccess {
                        lyrics = it
                    }
                }
            }
        }
    }

    DisposableEffect(song?.id) {
        if (song != null) {
            db.songQueries.lyricsById(song.id).addListener(lyrics_listener)

            if (current_song?.id != song.id) {
                val reference = song.Lyrics.get(db)
                if (lyrics == null || lyrics?.reference != reference) {
                    val loaded_lyrics = reference?.let {
                        SongLyricsLoader.getLoadedByLyrics(it)
                    }

                    if (loaded_lyrics != null) {
                        lyrics = loaded_lyrics
                    }
                    else if (load_lyrics) {
                        coroutine_scope.coroutineContext.cancelChildren()
                        coroutine_scope.launch {
                            val result: Result<SongLyrics>? = SongLyricsLoader.loadBySong(song, context)
                            result?.onSuccess {
                                lyrics = it
                            }
                        }
                    }
                }
            }
        }
        current_song = song

        onDispose {
            current_song?.id?.also { current_song_id ->
                db.songQueries.lyricsById(current_song_id).removeListener(lyrics_listener)
            }
        }
    }

    return lyrics
}
