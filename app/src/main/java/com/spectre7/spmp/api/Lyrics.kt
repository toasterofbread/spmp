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
import java.util.Base64

fun getSongLyrics(song: Song): Song.Lyrics? {
    val id = song.registry.lyrics_id
    val ret: Song.Lyrics?

    if (id != null) {
        throw NotImplementedError()
//        ret = getLyrics(id)
    }
    else {
        val results = searchForLyrics(song.title, song.artist.name).getDataOrThrow()
        if (results.isEmpty()) {
            return null
        }

        val lyrics = results.first()
        ret = getLyrics(lyrics.id, lyrics.source).getDataOrThrow()
    }

    song.registry.lyrics_id = ret.id
    song.registry.lyrics_source = ret.source.ordinal
    return ret
}

fun getLyricsData(lyrics_id: Int, sync_type: Song.Lyrics.SyncType): Result<String> {

    val body = "key_lyricsId=$lyrics_id&lyricsType=${sync_type.ordinal + 1}&terminalType=10&clientAppId=on354007".toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url("https://p1.petitlyrics.com/api/GetPetitLyricsData.php")
        .post(body)
        .build()

    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result.failure(response)
    }

    val START = "<lyricsData>"
    val END = "</lyricsData>"

    val xml = response.body!!.string()
    val start = xml.indexOf(START)
    val end = xml.indexOf(END, start + START.length)

    return Result.success(String(Base64.getDecoder().decode(xml.substring(start + START.length, end))))
}

fun getLyrics(lyrics_id: Int, lyrics_source: Song.Lyrics.Source): Result<Song.Lyrics> {

    when (lyrics_source) {
        Song.Lyrics.Source.PETITLYRICS -> {
            for (sync_type in Song.Lyrics.SyncType.byPriority()) {
                val data = getLyricsData(lyrics_id, sync_type)
                if (!data.success) {
                    return Result.failure(data.exception)
                }

                val tokens = Tokenizer().tokenize(data.data)
                for (token in tokens) {
                    println(token.allFeatures)
                }

                TODO()
            }
        }
    }

    return Result.failure(NotImplementedError())

//    match (method):
//        case "ptl":
//            try:
//                song_id = int(id)
//            except ValueError:
//                return None
//
//            for lyrics_type in range(3, 0, -1):
//                data = getLyricsData(song_id, lyrics_type)
//                if data is not None:
//                    return TimedLyrics(lyrics_id, lyrics_type - 1, data) if data.startswith("<wsy>") and data.endswith("</wsy>") else StaticLyrics(lyrics_id, data)
//
//            return None
//
//        case _:
//            return None
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

        val response = client.newCall(request).execute()
        if (response.code != 200) {
            return Result.failure(response)
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

        val lines = response.body!!.string().split('\n')
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
    if (!ret.success) {
        return ret
    }

    if (ret.data.isEmpty() && artist != null) {
        return performSearch(title_param)
    }

    return ret
}


val TERM_FURI_REPLACEMENTS = mapOf(
    "日" to "ひ",
    "君" to "きみ",
    "色" to "いろ",
    "瞑" to "つぶ",
    "足" to "あし",
    "角" to "かど"
)

abstract class Lyrics(val id: Int, val source: Song.Lyrics.Source) {

    abstract fun getSyncType(): Song.Lyrics.SyncType
//    abstract fun getWithFurigana():

//    fun getText(): String {
//        return when (this) {
//            is StaticLyrics -> _text
//            is TimedLyrics -> {
//                var ret = ""
//                for (line in lines) {
//                    ret += line.text + '\n'
//                }
//                ret
//            }
//            else -> throw NotImplementedError(this.javaClass.name)
//        }
//    }

    fun getFuriganaData() {
        fun trimOkurigana(original: String, furigana: String): List<Pair<String, String?>> {
            if (original.hasKanjiAndHiragana()) {
                var trim_amount = 0
                for (i in 1 .. furigana.length) {
                    if (original[original.length - i].isKanji() || original[original.length - i] != furigana[furigana.length - i]) {
                        trim_amount = i - 1
                        break
                    }
                }

                if (trim_amount != 0) {
                    return listOf(
                        Pair(original.substring(0, original.length - trim_amount), furigana.substring(0, furigana.length - trim_amount)),
                        Pair(original.substring(original.length - trim_amount, original.length), null)
                    )
                }
            }
            return listOf(Pair(original, furigana))
        }

//        fun getKey(term: List<Pair<String, String?>>, key: String): String {
//            return term[key]
//        }
//
//        val ret = mutableListOf<List<>>()

//        val lines = when (this) {
//            is StaticLyrics -> _text.split('\n')
//            is TimedLyrics -> List(lines.size) {
//                lines[i].text
//            }
//            else -> throw NotImplementedError()
//        }
//
//        for (line in lines) {
//            val line_data = mutableListOf()
//            ret.add(line_data)
//
//            for (term in )
//        }

    }
//        for line in [line.text for line in self.lines] if isinstance(self, TimedLyrics) else self.getText().split("\n"):
//
//            line_data = []
//            ret.append(line_data)
//
//            for term in kakasi.convert(line.replace("\n", "\\n").replace("\r", "\\r")):
//
//                orig = getKey(term, "orig")
//                hira = getKey(term, "hira")
//
//                if orig != hira and hasKanji(orig):
//                    terms = trimOkurigana(orig, hira)
//                    for i in range(len(terms)):
//                        if len(terms[i]) == 0:
//                            continue
//                        replacement = TERM_FURI_REPLACEMENTS.get(terms[i][0])
//                        if replacement is not None:
//                            terms[i][1] = replacement
//
//                    line_data += terms
//                else:
//                    line_data.append([orig])
//
//        return ret
}

//class StaticLyrics(id: Int, source: Song.Lyrics.Source, val _text: String): Lyrics(id, source) {
//
//    def getSyncType(self) -> int:
//        return 0
//
//    def getWithFurigana(self) -> list:
//        ret = []
//
//        for line in self._getFuriganaData():
//            terms = []
//            for term in line:
//                if term[0] == "" or (term[0] == "\r" and len(line) > 1):
//                    continue
//                terms.append({"subterms": [{"text": term[0], "furi": term[1] if len(term) > 1 else None}]})
//            if len(terms) > 0:
//                ret.append(terms)
//
//        return ret
//
//}
//
//class TimedLyrics(
//    id: Int,
//    source: Song.Lyrics.Source,
//    private val sync_type: Song.Lyrics.SyncType,
//    xml_data: String
//): Lyrics(id, source) {
//
//    val lines: List<Line>
//    var first_word: Word? = null
//
//    override fun getSyncType(): Song.Lyrics.SyncType {
//        return sync_type
//    }
//
//    init {
//        var prev_line: Line? = null
//        var index = 0
//
//        val _lines = mutableListOf<Line>()
//
//        val parser = Xml.newPullParser()
//        parser.setInput(xml_data.reader())
//        parser.nextTag()
//
//        parser.require(XmlPullParser.START_TAG, null, "line")
//        while (parser.next() != XmlPullParser.END_TAG) {
//            if (parser.eventType != XmlPullParser.START_TAG) {
//                continue
//            }
//            if (parser.name == "line") {
////                val line = Line(parser, index)
////                _lines.add(line)
////                index += line.text.length
////
////                if (prev_line != null) {
////                    prev_line.next_line = line
////                    prev_line.words.last().next_word = line.words.first()
////                    line.words.first().prev_word = prev_line.words.last()
////                }
////
////                line.prev_line = prev_line
////                prev_line = line
////
////                if (first_word == null) {
////                    first_word = line.words.first()
////                }
//            }
//            else {
//                var depth = 1
//                while (depth != 0) {
//                    when (parser.next()) {
//                        XmlPullParser.END_TAG -> depth--
//                        XmlPullParser.START_TAG -> depth++
//                    }
//                }
//            }
//
//        }
//    }

//    class Word(val index: Int) {
//        val text: String
//        val start_time: Long
//        val end_time: Long
//        val next_word: Word?
//        val prev_word: Word?
////        def __init__(self, data: dict, index: int):
////            self.text = data["wordstring"]
////            self.start_time = int(data["starttime"]) / 1000
////            self.end_time = int(data["endtime"]) / 1000
////            self.next_word: TimedLyrics.Word | None = None
////            self.prev_word: TimedLyrics.Word | None = None
////            self.index = index
//    }
//
//
//    class Line:
//
//        def __init__(self, data: dict, _index: int):
//            self.text: str = data["linestring"]
//            self.is_space = self.text is None
//            self.words: list[TimedLyrics.Word] = []
//            self.next_line: TimedLyrics.Line | None = None
//            self.prev_line: TimedLyrics.Line | None = None
//
//            self.lines: list = []
//            self.first_word: TimedLyrics.Word | None = None
//
//            if self.is_space:
//                self.words.append(TimedLyrics.Word({
//                    "wordstring": "\n",
//                    "starttime": -1,
//                    "endtime": -1
//                }, _index))
//                self.text = "\n"
//            else:
//                index = _index
//                prev_word: TimedLyrics.Word | None = None
//                for word_data in data["word"] if isinstance(data["word"], list) else [data["word"]]:
//                    if word_data["wordstring"] is None:
//                        continue
//
//                    word = TimedLyrics.Word(word_data, index)
//                    if len(word.text) > 0:
//                        self.words.append(word)
//                        index += len(word.text)
//                        if prev_word is not None:
//                            prev_word.next_word = word
//
//                        word.prev_word = prev_word
//                        prev_word = word
//
//    def getWithFurigana(self) -> list:
//        ret = []
//        furigana = self._getFuriganaData()
//
//        for line_i in range(len(self.lines)):
//
//            line_entry = []
//            ret.append(line_entry)
//
//            lyr_line = self.lines[line_i]
//            if lyr_line.is_space:
//                line_entry.append({"subterms": [{"text": "", "furi": None}], "start": -1, "end": -1})
//                continue
//
//            furi_line: list = furigana[line_i]
//            borrow = 0
//
//            for word in lyr_line.words:
//                terms: list[dict[str, str | None]] = []
//                i = len(word.text) - borrow
//                borrow = max(0, borrow - len(word.text))
//
//                while i > 0 and len(furi_line) > 0:
//                    orig = furi_line[0][0]
//                    furi = furi_line[0][1] if len(furi_line[0]) > 1 else None
//
//                    if len(orig.strip()) == 0:
//                        furi_line.pop(0)
//                        continue
//
//                    # Term fits into word
//                    if len(orig) <= i:
//                        i -= len(orig)
//                        terms.append({"text": orig, "furi": furi})
//                        furi_line.pop(0)
//
//                    # Term doesn't fit into word
//                    else:
//
//                        # Term has no furigana, so we can disect it
//                        if furi is None:
//                            terms.append({"text": orig[:i], "furi": None})
//                            furi_line[0][0] = orig[i:]
//                        else:
//                            terms.append({"text": orig, "furi": furi})
//                            furi_line.pop(0)
//                            borrow = len(orig) - i
//
//                        i = 0
//
//                if len(terms) > 0:
//                    word_entry = {"subterms": terms, "start": word.start_time, "end": word.end_time}
//                    line_entry.append(word_entry)
//
//        return ret
//}
