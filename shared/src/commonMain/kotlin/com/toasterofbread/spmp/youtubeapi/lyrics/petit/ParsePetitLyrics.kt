package com.toasterofbread.spmp.youtubeapi.lyrics.petit

import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsFuriganaTokeniser
import nl.adaptivity.xmlutil.serialization.XML
import kotlinx.serialization.Serializable

@Serializable
private data class PetitLyricsData(val lines: List<Line>) {
    @Serializable
    data class Line(val words: List<Word>)
    @Serializable
    data class Word(val starttime: Long, val endtime: Long, val wordstring: String)
}

internal fun parseTimedLyrics(raw_data: String): Result<List<List<SongLyrics.Term>>> {
    val data: PetitLyricsData = XML.decodeFromString(PetitLyricsData.serializer(), raw_data)

    return Result.success(
        data.lines.mapIndexed { line_index, line ->
            line.words.map { word ->
                SongLyrics.Term(
                    listOf(SongLyrics.Term.Text(word.wordstring)),
                    line_index,
                    word.starttime,
                    word.endtime
                )
            }
        }
    )
}
