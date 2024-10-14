package com.toasterofbread.spmp.youtubeapi.lyrics 

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.lrclib.loadLrclibLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.lrclib.searchLrclibLyrics
import kotlin.time.Duration

internal class LrclibLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    @Composable
    override fun getReadable(): String = "lrclib"
    override fun getColour(): Color = Color(0x0C0E41)
    override fun getUrlOfId(id: String): String? = null

    override fun supportsLyricsBySong(): Boolean = false
    override fun supportsLyricsBySearching(): Boolean = true

    override suspend fun getLyrics(
        lyrics_id: String,
        context: AppContext
    ): Result<SongLyrics> = runCatching {
		val lines: List<List<SongLyrics.Term>> = loadLrclibLyrics(lyrics_id).getOrThrow()

		return@runCatching SongLyrics(
			LyricsReference(source_index, lyrics_id),
			SongLyrics.SyncType.LINE_SYNC,
			lines
		)
    }

    override suspend fun searchForLyrics(
        title: String,
        artist_name: String?,
        album: String?,
        duration: Duration?
    ): Result<List<SearchResult>> = runCatching {
        return searchLrclibLyrics(title, artist_name, album, duration)
    }
}

