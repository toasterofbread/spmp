package com.toasterofbread.spmp.api.lyrics.petit

import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.api.lyrics.mergeAndFuriganiseTerms
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import com.atilika.kuromoji.ipadic.Tokenizer
import com.toasterofbread.spmp.api.lyrics.createTokeniser
import com.toasterofbread.spmp.resources.getStringTODO

internal fun parseStaticLyrics(data: String): List<List<SongLyrics.Term>> {
    val tokeniser = Tokenizer()

    val ret: MutableList<List<SongLyrics.Term>> = mutableListOf()

    for (line in data.split('\n')) {
        val tokens = tokeniser.tokenize(line)
        ret.add(tokens.mapNotNull { token ->
            if (token.surface.isBlank()) null
            else SongLyrics.Term(listOf(SongLyrics.Term.Text(token.surface, token.reading)), ret.size)
        })
    }

    return ret
}

internal fun parseTimedLyrics(data: String): Result<List<List<SongLyrics.Term>>> {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(data.reader())
    parser.nextTag()

    fun skip() {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    fun readText(tag: String): String {
        parser.require(XmlPullParser.START_TAG, null, tag)

        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }

        parser.require(XmlPullParser.END_TAG, null, tag)
        return result
    }

    fun parseTerm(line_index: Int): SongLyrics.Term? {
        parser.require(XmlPullParser.START_TAG, null, "word")

        var text: String? = null
        var start: Long? = null
        var end: Long? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "wordstring" -> text = readText("wordstring")
                "starttime" -> start = readText("starttime").toLong()
                "endtime" -> end = readText("endtime").toLong()
                else -> skip()
            }
        }

        parser.require(XmlPullParser.END_TAG, null, "word")

        if (text!!.isEmpty()) {
            return null
        }

        return SongLyrics.Term(listOf(SongLyrics.Term.Text(text)), line_index, start!!, end!!)
    }

    val tokeniser: Tokenizer = createTokeniser()

    fun parseLine(index: Int): List<SongLyrics.Term> {
        parser.require(XmlPullParser.START_TAG, null, "line")

        var line_start: Long? = null
        var line_end: Long? = null

        val terms: MutableList<SongLyrics.Term> = mutableListOf()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
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

        parser.require(XmlPullParser.END_TAG, null, "line")
        return mergeAndFuriganiseTerms(tokeniser, terms)
    }

    val ret: MutableList<List<SongLyrics.Term>> = mutableListOf()

    parser.require(XmlPullParser.START_TAG, null, "wsy")
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }

        println("NAME ${parser.name}")
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
