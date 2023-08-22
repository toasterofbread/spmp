package com.toasterofbread.spmp.youtubeapi.lyrics

import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.spmp.youtubeapi.lyrics.kugou.loadKugouLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.kugou.searchKugouLyrics

internal class KugouLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    override fun getReadable(): String = getString("lyrics_source_kugou")
    override fun getColour(): Color = Color(0xFF50A6FB)

    override suspend fun getLyrics(lyrics_id: String, context: PlatformContext): Result<SongLyrics> {
        val load_result = loadKugouLyrics(lyrics_id)
        val lines = load_result.getOrNull() ?: return load_result.cast()

        return Result.success(
            SongLyrics(
                LyricsReference(source_index, lyrics_id),
                SongLyrics.SyncType.LINE_SYNC,
                lines
            )
        )
    }

    override suspend fun searchForLyrics(title: String, artist_name: String?): Result<List<SearchResult>> {
        return searchKugouLyrics(title, artist_name)
    }
}
