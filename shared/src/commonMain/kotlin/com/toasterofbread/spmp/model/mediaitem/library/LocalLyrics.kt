package com.toasterofbread.spmp.model.mediaitem.library

import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun MediaItemLibrary.saveLyricsToFile(lyrics: SongLyrics) = withContext(Dispatchers.IO) {
    TODO()
    Unit
}

suspend fun MediaItemLibrary.loadLyricsFromFile(reference: LyricsReference) = withContext(Dispatchers.IO) {
    TODO()
    Unit
}
