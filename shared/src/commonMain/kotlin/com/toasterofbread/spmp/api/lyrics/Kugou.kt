package com.toasterofbread.spmp.api.lyrics

import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.api.lyrics.kugou.loadKugouLyrics
import com.toasterofbread.spmp.api.lyrics.kugou.searchKugouLyrics
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.resources.getString

internal class KugouLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    override fun getReadable(): String = getString("lyrics_source_kugou")
    override fun getColour(): Color = Color(0xFF50A6FB)

    override suspend fun getLyrics(lyrics_id: String): Result<SongLyrics> {
        val load_result = loadKugouLyrics(lyrics_id)
        val lines = load_result.getOrNull() ?: return load_result.cast()

        return Result.success(
            SongLyrics(
                LyricsReference(lyrics_id, source_idx),
                SongLyrics.SyncType.LINE_SYNC,
                lines
            )
        )
    }

    override suspend fun searchForLyrics(title: String, artist_name: String?): Result<List<SearchResult>> {
        return searchKugouLyrics(title, artist_name)
    }
}
