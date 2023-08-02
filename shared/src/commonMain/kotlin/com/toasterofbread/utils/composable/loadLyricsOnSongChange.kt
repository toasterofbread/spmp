package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.utils.launchSingle
import com.toasterofbread.spmp.platform.PlatformContext

@Composable
fun loadLyricsOnSongChange(song: Song?, context: PlatformContext, load_lyrics: Boolean = true): SongLyrics? {
    val coroutine_scope = rememberCoroutineScope()
    var current_song: Song? by remember { mutableStateOf(song) }
    var lyrics: SongLyrics? by remember { mutableStateOf(null) }

    val lyrics_listener = remember {
        Query.Listener {
            lyrics = null

            if (load_lyrics && song != null) {
                coroutine_scope.launchSingle {
                    val result = SongLyricsLoader.loadBySong(song, context)
                    result.onSuccess {
                        lyrics = it
                    }
                }
            }
        }
    }

    DisposableEffect(song?.id) {
        lyrics = null

        if (song != null) {
            context.database.songQueries.lyricsById(song.id).addListener(lyrics_listener)
            if (load_lyrics && current_song?.id != song.id) {
                coroutine_scope.launchSingle {
                    val result = SongLyricsLoader.loadBySong(song, context)
                    result.onSuccess {
                        lyrics = it
                    }
                }
            }
        }
        current_song = song

        onDispose {
            current_song?.id?.also { current_song_id ->
                context.database.songQueries.lyricsById(current_song_id).removeListener(lyrics_listener)
            }
        }
    }

    return lyrics
}
