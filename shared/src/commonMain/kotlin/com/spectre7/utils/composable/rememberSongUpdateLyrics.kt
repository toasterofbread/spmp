package com.spectre7.utils.composable

@Composable 
fun rememberSongUpdateLyrics(song: Song?): Song? {
    var current_song: Song? by remember { mutableStateOf(song) }
    val reg_lyrics_listener: (Pair<Int, Song.Lyrics.Source>?) -> Unit = remember { { data ->
        val lyrics_holder = current_song!!.lyrics
        if (data?.first != lyrics_holder.lyrics?.id || data?.second != lyrics_holder.lyrics?.source) {
            lyrics_holder.loadAndGet()
        }
    } }

    DisposableEffect(song) {
        current_song = song?.apply {
            song_reg_entry.lyrics_listeners.add(reg_lyrics_listener)
            lyrics.loadAndGet()
        }

        onDispose {
            current_song?.apply { song_reg_entry.lyrics_listeners.remove(reg_lyrics_listener) }
        }
    }

    return song
}
