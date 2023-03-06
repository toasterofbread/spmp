package com.spectre7.spmp.api

import android.text.Html
import android.util.Xml
import com.atilika.kuromoji.ipadic.Tokenizer
import com.spectre7.spmp.model.Song
import com.spectre7.utils.hasKanjiAndHiragana
import com.spectre7.utils.isKanji
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.lang.Float.max
import java.lang.Float.min
import java.util.*

fun getSongLyrics(song: Song): Song.Lyrics? {
    val ret: Song.Lyrics?

    val id: Int? = song.song_reg_entry.lyrics_id
    val source: Song.Lyrics.Source? = song.song_reg_entry.lyrics_source

    if (id != null && source != null) {
        ret = getLyrics(id, source).getOrThrowHere()
    }
    else {
        val results = searchForLyrics(song.title!!, song.artist!!.title).getOrThrowHere()
        if (results.isEmpty()) {
            return null
        }

        val lyrics = results.first()
        ret = getLyrics(lyrics.id, lyrics.source).getOrThrowHere()
    }

    song.song_reg_entry.lyrics_id = ret.id
    song.song_reg_entry.lyrics_source = ret.source
    return ret
}

fun getLyricsData(lyrics_id: Int, sync_type: Song.Lyrics.SyncType): Result<String> {
    val body = "key_lyricsId=$lyrics_id&lyricsType=${sync_type.ordinal + 1}&terminalType=10&clientAppId=on354007".toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url("https://p1.petitlyrics.com/api/GetPetitLyricsData.php")
        .post(body)
        .build()

    val result = DataApi.request(request)
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

private fun trimOkurigana(term: Song.Lyrics.Term.Text): List<Song.Lyrics.Term.Text> {

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

    val terms: MutableList<Song.Lyrics.Term.Text> = mutableListOf()
    val last_term: Song.Lyrics.Term.Text

    if (trim_start > 0) {
        terms.add(
            Song.Lyrics.Term.Text(
                term.text.substring(0, trim_start),
                null
            )
        )

        last_term = Song.Lyrics.Term.Text(
            term.text.substring(trim_start),
            term.furi!!.substring(trim_start)
        )
    }
    else {
        last_term = term
    }

    if (trim_end > 0) {
        terms.add(
            Song.Lyrics.Term.Text(
                last_term.text.substring(0, last_term.text.length - trim_end),
                last_term.furi!!.substring(0, last_term.furi!!.length - trim_end)
            )
        )
        terms.add(
            Song.Lyrics.Term.Text(
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

private fun mergeAndFuriganiseTerms(tokeniser: Tokenizer, terms: List<Song.Lyrics.Term>): List<Song.Lyrics.Term> {

    val ret: MutableList<Song.Lyrics.Term> = mutableListOf()

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
        var start: Float = Float.POSITIVE_INFINITY
        var end: Float = Float.NEGATIVE_INFINITY

        while (text.length < token_base.length) {
            val term = terms[current_term]
            val subterm = term.subterms.single()
            start = min(start, term.start!!)
            end = max(end, term.end!!)

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

        ret.add(Song.Lyrics.Term(trimOkurigana(Song.Lyrics.Term.Text(text, token.reading)), start, end))
    }

    return ret
}

private fun parseStaticLyrics(data: String): List<List<Song.Lyrics.Term>> {
    val tokeniser = Tokenizer()

    val ret: MutableList<List<Song.Lyrics.Term>> = mutableListOf()

    for (line in data.split('\n')) {
        val tokens = tokeniser.tokenize(line)
        ret.add(List(tokens.size) { i ->
            val token = tokens[i]
            Song.Lyrics.Term(listOf(Song.Lyrics.Term.Text(token.surface, token.reading)))
        })
    }

    return ret
}

private fun parseTimedLyrics(data: String): List<List<Song.Lyrics.Term>> {
    val parser = Xml.newPullParser()
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

    fun parseTerm(): Song.Lyrics.Term? {
        parser.require(XmlPullParser.START_TAG, null, "word")

        var text: String? = null
        var start: Float? = null
        var end: Float? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "wordstring" -> text = readText("wordstring")
                "starttime" -> start = readText("starttime").toInt() / 1000f
                "endtime" -> end = readText("endtime").toInt() / 1000f
                else -> skip()
            }
        }

        parser.require(XmlPullParser.END_TAG, null, "word")

        if (text!!.isBlank()) {
            return null
        }

        return Song.Lyrics.Term(listOf(Song.Lyrics.Term.Text(text)), start!!, end!!)
    }

    val tokeniser = Tokenizer()

    fun parseLine(): List<Song.Lyrics.Term> {
        parser.require(XmlPullParser.START_TAG, null, "line")

        val terms: MutableList<Song.Lyrics.Term> = mutableListOf()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            if (parser.name != "word") {
                skip()
                continue
            }

            val term = parseTerm()
            if (term != null) {
                terms.add(term)
            }
        }

        parser.require(XmlPullParser.END_TAG, null, "line")
        return mergeAndFuriganiseTerms(tokeniser, terms)
    }

    val ret: MutableList<List<Song.Lyrics.Term>> = mutableListOf()

    parser.require(XmlPullParser.START_TAG, null, "wsy")
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }

        if (parser.name != "line") {
            skip()
            continue
        }

        ret.add(parseLine())
    }

    while (ret.first().isEmpty()) {
        ret.removeFirst()
    }
    while (ret.last().isEmpty()) {
        ret.removeLast()
    }

    for (line in ret.withIndex()) {
        if (line.value.isEmpty()) {
            ret[line.index] = listOf(Song.Lyrics.Term.EMPTY)
        }
    }

    return ret
}

fun getLyrics(lyrics_id: Int, lyrics_source: Song.Lyrics.Source): Result<Song.Lyrics> {

    when (lyrics_source) {
        Song.Lyrics.Source.PETITLYRICS -> {
            for (sync_type in Song.Lyrics.SyncType.byPriority()) {
                val result = getLyricsData(lyrics_id, sync_type)
                if (!result.isSuccess) {
                    return result.cast()
                }

                val lyrics: List<List<Song.Lyrics.Term>>
                if (result.data.startsWith("<wsy>")) {
                    lyrics = parseTimedLyrics(result.data)
                }
                else {
                    lyrics = parseStaticLyrics(result.data)
                }

                return Result.success(Song.Lyrics(lyrics_id, lyrics_source, sync_type, lyrics))
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
    val source: Song.Lyrics.Source,
    var name: String,
    var sync_type: Song.Lyrics.SyncType,
    var artist_id: String?,
    var artist_name: String?,
    var album_id: String?,
    var album_name: String?
)

fun searchForLyrics(title: String, artist: String?): Result<List<LyricsSearchResult>> {

    var title_param = concatParams("?title=", title)
    var artist_param = if (artist != null) concatParams("&artist=", artist) else ""

    val RESULT_START = "<a href=\"/lyrics/"
    val RESULT_END = "</a>"
    val SYNC_TYPE_START = "<span class=\"lyrics-list-sync "

    fun performSearch(params: String): Result<List<LyricsSearchResult>> {
        val request = Request.Builder()
            .url("https://petitlyrics.com/search_lyrics$params")
            .header("User-Agent", DATA_API_USER_AGENT)
            .build()

        val result = DataApi.request(request)
        if (result.isFailure) {
            return result.cast()
        }

        val ret = mutableListOf<LyricsSearchResult>()

        var r_id: Int? = null
        val r_source: Song.Lyrics.Source = Song.Lyrics.Source.PETITLYRICS
        var r_name: String? = null
        var r_sync_type: Song.Lyrics.SyncType? = null
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
                    r_sync_type = Song.Lyrics.SyncType.fromKey(sync_type)
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
                r_name = Html.fromHtml(line.substring(0, end + RESULT_END.length), 0).toString()
            }
            else {
                val split = href.split('/')

                when (split[0]) {
                    "artist" -> {
                        r_artist_id = split[1]
                        r_artist_name = Html.fromHtml(line.substring(0, end + RESULT_END.length), 0).toString()
                    }
                    "album" -> {
                        r_album_id = split[1]
                        r_album_name = Html.fromHtml(line.substring(0, end + RESULT_END.length), 0).toString()
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

    val ret = performSearch(title_param + artist_param)
    if (!ret.isSuccess) {
        return ret
    }

    if (ret.data.isEmpty() && artist != null) {
        return performSearch(title_param)
    }

    return ret
}
