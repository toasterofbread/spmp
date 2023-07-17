package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.utils.launchSingle

@Composable
fun rememberSongUpdateLyrics(song: Song?, update_lyrics: Boolean = true): State<Song?> {
    val current_song: MutableState<Song?> = remember { mutableStateOf(song) }
    val coroutine_scope = rememberCoroutineScope()
    val reg_lyrics_listener: (LyricsReference?) -> Unit = remember(update_lyrics) { { data ->
        if (update_lyrics) {
            val lyrics_holder = current_song.value!!.lyrics
            if (data?.id != lyrics_holder.lyrics?.id || data?.source_idx != lyrics_holder.lyrics?.source_idx) {
                coroutine_scope.launchSingle { lyrics_holder.loadAndGet() }
            }
        }
    } }

    DisposableEffect(song) {
        current_song.value = song?.apply {
            song_reg_entry.lyrics_listeners.add(reg_lyrics_listener)
            if (update_lyrics && current_song.value != song) {
                coroutine_scope.launchSingle { lyrics.loadAndGet() }
            }
        }

        onDispose {
            current_song.value?.apply { song_reg_entry.lyrics_listeners.remove(reg_lyrics_listener) }
        }
    }

    return current_song
}
