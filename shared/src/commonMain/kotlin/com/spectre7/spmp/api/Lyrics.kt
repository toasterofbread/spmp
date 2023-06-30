package com.toasterofbread.spmp.api

import com.atilika.kuromoji.ipadic.Tokenizer
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.utils.hasKanjiAndHiragana
import com.toasterofbread.utils.isKanji
import com.toasterofbread.utils.isJP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.nio.channels.ClosedByInterruptException
import java.util.*

suspend fun getSongLyrics(song: Song, data: Pair<Int, SongLyrics.Source>?): Result<SongLyrics> {
    if (song.title == null) {
        return Result.failure(RuntimeException("Song has no title"))
    }

    val result: Result<SongLyrics> =
        if (data != null) getLyrics(data.first, data.second)
        else {
            searchForLyrics(song.title!!, song.artist?.title).fold(
                { results ->
                    if (results.isEmpty()) Result.failure(RuntimeException("No lyrics found"))
                    else getLyrics(results.first().id, results.first().source)
                },
                {
                    Result.failure(it)
                }
            )
        }

    result.getOrNull()?.also { lyrics ->
        song.song_reg_entry.updateLyrics(lyrics.id, lyrics.source)
    }

    return result
}

fun getLyricsData(lyrics_id: Int, sync_type: SongLyrics.SyncType): Result<String> {
    val body = "key_lyricsId=$lyrics_id&lyricsType=${sync_type.ordinal + 1}&terminalType=10&clientAppId=on354007".toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url("https://p1.petitlyrics.com/api/GetPetitLyricsData.php")
        .post(body)
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val START = "<lyricsData>"
    val END = "</lyricsData>"

    val xml = result.getOrThrowHere().body!!.string()
    val start = xml.indexOf(START)
    val end = xml.indexOf(END, start + START.length)

    val decoded = Base64.getDecoder().decode(xml.substring(start + START.length, end))
    return Result.success(String(decoded))
}

private fun trimOkurigana(term: SongLyrics.Term.Text): List<SongLyrics.Term.Text> {

    if (term.furi == null || !term.text.hasKanjiAndHiragana()) {
        return listOf(term)
    }

    var trim_start: Int = 0
    for (i in 0 until term.furi!!.length) {
        if (term.text[i].isKanji() || term.text[i] != term.furi!![i]) {
            trim_start = i
            break
        }
    }

    var trim_end: Int = 0
    for (i in 1 .. term.furi!!.length) {
        if (term.text[term.text.length - i].isKanji() || term.text[term.text.length - i] != term.furi!![term.furi!!.length - i]) {
            trim_end = i - 1
            break
        }
    }

    val terms: MutableList<SongLyrics.Term.Text> = mutableListOf()
    val last_term: SongLyrics.Term.Text

    if (trim_start > 0) {
        terms.add(
            SongLyrics.Term.Text(
                term.text.substring(0, trim_start),
                null
            )
        )

        last_term = SongLyrics.Term.Text(
            term.text.substring(trim_start),
            term.furi!!.substring(trim_start)
        )
    }
    else {
        last_term = term
    }

    if (trim_end > 0) {
        terms.add(
            SongLyrics.Term.Text(
                last_term.text.substring(0, last_term.text.length - trim_end),
                last_term.furi!!.substring(0, last_term.furi!!.length - trim_end)
            )
        )
        terms.add(
            SongLyrics.Term.Text(
                last_term.text.takeLast(trim_end),
                null
            )
        )
    }
    else {
        terms.add(last_term)
    }

    return terms
}

private fun mergeAndFuriganiseTerms(tokeniser: Tokenizer, terms: List<SongLyrics.Term>): List<SongLyrics.Term> {
    if (terms.isEmpty()) {
        return emptyList()
    }

    val ret: MutableList<SongLyrics.Term> = mutableListOf()
    val terms_to_process: MutableList<SongLyrics.Term> = mutableListOf()

    for (term in terms) {
        val text = term.subterms.single().text
        if (text.any { it.isJP() }) {
            terms_to_process.add(term)
        }
        else {
            ret.addAll(_mergeAndFuriganiseTerms(tokeniser, terms_to_process))
            terms_to_process.clear()
            ret.add(term)
        }
    }

    ret.addAll(_mergeAndFuriganiseTerms(tokeniser, terms_to_process))

    return ret
}

private fun _mergeAndFuriganiseTerms(tokeniser: Tokenizer, terms: List<SongLyrics.Term>): List<SongLyrics.Term> {
    if (terms.isEmpty()) {
        return emptyList()
    }

    val ret: MutableList<SongLyrics.Term> = mutableListOf()
    val line_range = terms.first().line_range!!
    val line_index = terms.first().line_index

    var terms_text: String = ""
    for (term in terms) {
        terms_text += term.subterms.single().text
    }

    val tokens = tokeniser.tokenize(terms_text)

    var current_term: Int = 0
    var term_head: Int = 0

    for (token in tokens) {
        val token_base = token.surface

        var text: String = ""
        var start: Long = Long.MAX_VALUE
        var end: Long = Long.MIN_VALUE

        while (text.length < token_base.length) {
            val term = terms[current_term]
            val subterm = term.subterms.single()
            start = minOf(start, term.start!!)
            end = maxOf(end, term.end!!)

            val needed = token_base.length - text.length
            if (needed < subterm.text.length - term_head) {
                text += subterm.text.substring(term_head, term_head + needed)
                term_head += needed
            }
            else {
                text += subterm.text
                term_head = 0
                current_term++
            }
        }

        val term = SongLyrics.Term(trimOkurigana(SongLyrics.Term.Text(text, token.reading)), line_index, start, end)
        term.line_range = line_range
        ret.add(term)
    }

    return ret
}

private fun parseStaticLyrics(data: String): List<List<SongLyrics.Term>> {
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

private fun parseTimedLyrics(data: String): List<List<SongLyrics.Term>> {
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

    val tokeniser: Tokenizer
    try {
        tokeniser = Tokenizer()
    }
    catch (e: RuntimeException) {
        if (e.cause is ClosedByInterruptException) {
            throw InterruptedException()
        }
        else {
            throw e
        }
    }

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

        if (parser.name != "line") {
            skip()
            continue
        }

        ret.add(parseLine(ret.size))
    }

    while (ret.first().isEmpty()) {
        ret.removeFirst()
    }
    while (ret.last().isEmpty()) {
        ret.removeLast()
    }

    for (line in ret.withIndex()) {
        if (line.value.isEmpty()) {
            ret[line.index] = listOf()
        }
    }

    return ret
}

fun getLyrics(lyrics_id: Int, lyrics_source: SongLyrics.Source): Result<SongLyrics> {
    when (lyrics_source) {
        SongLyrics.Source.PETITLYRICS -> {
            for (sync_type in SongLyrics.SyncType.byPriority()) {
                val result = getLyricsData(lyrics_id, sync_type)
                if (!result.isSuccess) {
                    return result.cast()
                }

                val lyrics: List<List<SongLyrics.Term>>
                if (result.data.startsWith("<wsy>")) {
                    lyrics = parseTimedLyrics(result.data)
                }
                else {
                    lyrics = parseStaticLyrics(result.data)
                    return Result.success(SongLyrics(lyrics_id, lyrics_source, SongLyrics.SyncType.NONE, lyrics))
                }

                return Result.success(SongLyrics(lyrics_id, lyrics_source, sync_type, lyrics))
            }
        }
    }

    return Result.failure(NotImplementedError())
}

private fun concatParams(first: String, second: String): String {
    var ret = first
    for (char in second) {
        // Replace all whitespace with the standard character
        if (char.isWhitespace()) {
            ret += ' '
        }
        else {
            ret += char
        }
    }
    return ret
}

data class LyricsSearchResult(
    var id: Int,
    val source: SongLyrics.Source,
    var name: String,
    var sync_type: SongLyrics.SyncType,
    var artist_id: String?,
    var artist_name: String?,
    var album_id: String?,
    var album_name: String?
)

suspend fun searchForLyrics(title: String, artist: String?): Result<List<LyricsSearchResult>> = withContext(Dispatchers.IO) {

    val title_param = concatParams("?title=", title)
    val artist_param = if (artist != null) concatParams("&artist=", artist) else ""

    val RESULT_START = "<a href=\"/lyrics/"
    val RESULT_END = "</a>"
    val SYNC_TYPE_START = "<span class=\"lyrics-list-sync "

    fun performSearch(params: String): Result<List<LyricsSearchResult>> {
        val request = Request.Builder()
            .url("https://petitlyrics.com/search_lyrics$params")
            .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/114.0")
            .build()

        val result = Api.request(request)
        if (result.isFailure) {
            return result.cast()
        }

        val ret = mutableListOf<LyricsSearchResult>()

        var r_id: Int? = null
        val r_source: SongLyrics.Source = SongLyrics.Source.PETITLYRICS
        var r_name: String? = null
        var r_sync_type: SongLyrics.SyncType? = null
        var r_artist_id: String? = null
        var r_artist_name: String? = null
        var r_album_id: String? = null
        var r_album_name: String? = null

        val lines = result.getOrThrowHere().body!!.string().split('\n')
        for (element in lines) {
            val line = element.trim()

            if (!line.startsWith(RESULT_START)) {
                if (r_id != null && r_sync_type == null && line.startsWith(SYNC_TYPE_START)) {
                    val sync_type = line.substring(SYNC_TYPE_START.length, line.indexOf('"', SYNC_TYPE_START.length))
                    r_sync_type = SongLyrics.SyncType.fromKey(sync_type)
                }
                continue
            }

            val href = line.substring(RESULT_START.length, line.indexOf('"', RESULT_START.length + 1))
            val end = line.indexOf(RESULT_END, RESULT_START.length + href.length)

            // If href is an int, this is the start of a new result
            val result_id = href.toIntOrNull()
            if (result_id != null) {
                if (r_id != null) {
                    ret.add(
                        LyricsSearchResult(
                            r_id,
                            r_source,
                            r_name!!,
                            r_sync_type!!,
                            r_artist_id,
                            r_artist_name,
                            r_album_id,
                            r_album_name
                        )
                    )
                }
                else {
                    r_sync_type = null
                    r_artist_id = null
                    r_artist_name = null
                    r_album_id = null
                    r_album_name = null
                }

                r_id = result_id
                r_name = Jsoup.parse(line.substring(0, end + RESULT_END.length)).body().text()
            }
            else {
                val split = href.split('/')

                when (split[0]) {
                    "artist" -> {
                        r_artist_id = split[1]
                        r_artist_name = Jsoup.parse(line.substring(0, end + RESULT_END.length)).body().text()
                    }
                    "album" -> {
                        r_album_id = split[1]
                        r_album_name = Jsoup.parse(line.substring(0, end + RESULT_END.length)).body().text()
                    }
                }
            }
        }

        if (r_id != null) {
            ret.add(
                LyricsSearchResult(
                    r_id,
                    r_source,
                    r_name!!,
                    r_sync_type!!,
                    r_artist_id,
                    r_artist_name,
                    r_album_id,
                    r_album_name
                )
            )
        }

        return Result.success(ret)
    }

    try {
        val result = performSearch(title_param + artist_param)
        result.onSuccess { results ->
            if (results.isEmpty() && artist != null) {
                return@withContext performSearch(title_param)
            }
        }
        return@withContext result
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
}
