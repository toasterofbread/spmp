package com.toasterofbread.spmp.youtubeapi.lyrics.petit

import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsFuriganaTokeniser
import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.mini.MiniXmlPullParser

internal fun parseTimedLyrics(data: String): Result<List<List<SongLyrics.Term>>> {
    val parser = MiniXmlPullParser(data.iterator())
    parser.nextTag()

    fun skip() {
        if (parser.eventType != EventType.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                EventType.END_TAG -> depth--
                EventType.START_TAG -> depth++
                else -> {}
            }
        }
    }

    fun readText(tag: String): String {
        parser.require(EventType.START_TAG, null, tag)

        var result = ""
        if (parser.next() == EventType.TEXT) {
            result = parser.text
            parser.nextTag()
        }

        parser.require(EventType.END_TAG, null, tag)
        return result
    }

    fun parseTerm(line_index: Int): SongLyrics.Term? {
        parser.require(EventType.START_TAG, null, "word")

        var text: String? = null
        var start: Long? = null
        var end: Long? = null

        while (parser.next() != EventType.END_TAG) {
            if (parser.eventType != EventType.START_TAG) {
                continue
            }

            when (parser.name) {
                "wordstring" -> text = readText("wordstring")
                "starttime" -> start = readText("starttime").toLong()
                "endtime" -> end = readText("endtime").toLong()
                else -> skip()
            }
        }

        parser.require(EventType.END_TAG, null, "word")

        if (text!!.isEmpty()) {
            return null
        }

        return SongLyrics.Term(listOf(SongLyrics.Term.Text(text)), line_index, start!!, end!!)
    }

    fun parseLine(index: Int): List<SongLyrics.Term> {
        parser.require(EventType.START_TAG, null, "line")

        var line_start: Long? = null
        var line_end: Long? = null

        val terms: MutableList<SongLyrics.Term> = mutableListOf()
        while (parser.next() != EventType.END_TAG) {
            if (parser.eventType != EventType.START_TAG) {
                continue
            }

            if (parser.name != "word") {
                skip()
                continue
            }

            val term = parseTerm(index)
            if (term != null) {
                terms.add(term)

                if (line_start == null) {
                    line_start = term.start
                    line_end = term.end
                }
                else {
                    if (term.start!! < line_start) {
                        line_start = term.start
                    }
                    if (term.end!! > line_end!!) {
                        line_end = term.end
                    }
                }
            }
        }

        if (terms.isNotEmpty()) {
            val range = line_start!! .. line_end!!
            for (term in terms) {
                term.line_range = range
            }
        }

        parser.require(EventType.END_TAG, null, "line")
        return terms
    }

    val ret: MutableList<List<SongLyrics.Term>> = mutableListOf()

    parser.require(EventType.START_TAG, null, "wsy")
    while (parser.next() != EventType.END_TAG) {
        if (parser.eventType != EventType.START_TAG) {
            continue
        }

        if (parser.name != "line") {
            skip()
            continue
        }

        ret.add(parseLine(ret.size))
    }

    if (ret.isEmpty()) {
        return Result.failure(RuntimeException(getStringTODO("Lyrics data is empty")))
    }

    while (ret.first().isEmpty()) {
        ret.removeFirst()
        if (ret.isEmpty()) {
            return Result.failure(RuntimeException(getStringTODO("Lyrics data is empty")))
        }
    }

    while (ret.last().isEmpty()) {
        ret.removeLast()
    }

    for (line in ret.withIndex()) {
        if (line.value.isEmpty()) {
            ret[line.index] = listOf()
        }
    }

    return Result.success(ret)
}
