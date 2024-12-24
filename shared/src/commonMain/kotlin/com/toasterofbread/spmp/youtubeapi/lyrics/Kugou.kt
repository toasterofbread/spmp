package com.toasterofbread.spmp.youtubeapi.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.youtubeapi.lyrics.kugou.loadKugouLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.kugou.searchKugouLyrics
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lyrics_source_kugou
import kotlin.time.Duration

internal class KugouLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    @Composable
    override fun getReadable(): String = stringResource(Res.string.lyrics_source_kugou)
    override fun getColour(): Color = Color(0xFF50A6FB)
    override fun getUrlOfId(id: String): String? = null

    override suspend fun getLyrics(
        lyrics_id: String,
        context: AppContext
    ): Result<SongLyrics> = runCatching {
        val lines: List<List<SongLyrics.Term>> = loadKugouLyrics(lyrics_id, context.getUiLanguage().toTag()).getOrThrow()

        return@runCatching SongLyrics(
            LyricsReference(source_index, lyrics_id),
            SongLyrics.SyncType.LINE_SYNC,
            lines
        )
    }

    override suspend fun searchForLyrics(
        title: String,
        artist_name: String?,
        album_name: String?,
        duration: Duration?
    ): Result<List<SearchResult>> {
        return searchKugouLyrics(title, artist_name)
    }
}
