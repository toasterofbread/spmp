package com.toasterofbread.spmp.youtubeapi.lyrics.petit

import com.toasterofbread.spmp.model.lyrics.SongLyrics
import nl.adaptivity.xmlutil.serialization.XML
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

internal fun parseTimedLyrics(raw_data: String): Result<List<List<SongLyrics.Term>>> {
    val data: wsy = XML.decodeFromString(wsy.serializer(), raw_data)

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

@Serializable
internal data class wsy(
    @XmlElement
    val linenum: Int,
    @XmlSerialName("line", "", "")
    val lines: List<Line>
) {
    @Serializable
    data class Line(
        @XmlElement
        val linestring: String,
        @XmlElement
        val wordnum: Int,
        @XmlSerialName("word", "", "")
        val words: List<Word>
    )
    @Serializable
    data class Word(
        @XmlElement
        val starttime: Long,
        @XmlElement
        val endtime: Long,
        @XmlElement
        val wordstring: String,
        @XmlElement
        val chanum: Int
    )
}
