package com.toasterofbread.spmp.api.lyrics

import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.resources.getStringTODO

sealed class LyricsSource protected constructor(val idx: Int) {
    data class SearchResult(
        var id: Int,
        var name: String,
        var sync_type: SongLyrics.SyncType,
        var artist_name: String?,
        var album_name: String?
    )
    
    abstract fun getReadable(): String
    abstract fun getColour(): Color
    
    abstract suspend fun getLyrics(lyrics_id: Int): Result<SongLyrics>
    abstract suspend fun searchForLyrics(title: String, artist_name: String? = null): Result<List<SearchResult>>

    companion object {
        val SOURCE_AMOUNT: Int = 1
        fun fromIdx(source_idx: Int): LyricsSource = when (source_idx) {
            0 -> PetitLyricsSource(source_idx)
            else -> throw NotImplementedError(source_idx.toString())
        }
    }
}

suspend fun getSongLyrics(song: Song, data: Pair<Int, Int>?): Result<SongLyrics> {
    val title = song.title ?: return Result.failure(RuntimeException("Song has no title"))

    if (data != null) {
        val source = LyricsSource.fromIdx(data.second)
        return source.getLyrics(data.first)
    }

    var fail_result: Result<SongLyrics>? = null
    for (source_idx in 0 until LyricsSource.SOURCE_AMOUNT) {
        val source = LyricsSource.fromIdx(source_idx)

        val result: LyricsSource.SearchResult = source.searchForLyrics(title, song.artist?.title).fold(
            { results ->
                if (results.isEmpty()) {
                    fail_result = null
                    null
                }
                else {
                    results.first()
                }
            },
            { 
                if (fail_result == null) {
                    fail_result = Result.failure(it)
                }
                null
            }
        ) ?: continue

        val lyrics_result = source.getLyrics(result.id)
        if (lyrics_result.isSuccess) {
            return lyrics_result
        }

        if (fail_result == null) {
            fail_result = lyrics_result
        }
    }

    return fail_result ?: Result.failure(RuntimeException(getStringTODO("No lyrics found")))
}
