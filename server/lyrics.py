from abc import ABC, abstractmethod
import base64
import requests
from pykakasi import Kakasi
from unicodeblock import blocks as unicodeBlock
from xmltodict import parse as parseXml

TERM_FURI_REPLACEMENTS = {
    "日": "ひ",
    "君": "きみ",
    "色": "いろ",
    "瞑": "つぶ",
    "足": "あし",
    "角": "かど"
}

class Lyrics(ABC):

    def __init__(self, id: str):
        self.id = id

    def getId(self) -> str:
        return self.id

    def isTimed(self) -> bool:
        return isinstance(self, TimedLyrics)

    def getSource(self) -> str:
        return self.getId().split("/")[0]

    def getText(self) -> str:
        if isinstance(self, StaticLyrics):
            return self.text
        else:
            assert(isinstance(self, TimedLyrics))
            ret = ""
            for line in self.lines:
                ret += line.text + "\n"
            return ret

    def _getFuriganaData(self):
        def hasKanjiAndHiragana(string: str):
            has_kanji = False
            has_hiragana = False

            for char in string:
                block = unicodeBlock.of(char)
                if block == "HIRAGANA":
                    if has_kanji:
                        return True
                    has_hiragana = True
                elif block == "CJK_UNIFIED_IDEOGRAPHS":
                    if has_hiragana:
                        return True
                    has_kanji = True

        def isKanji(char: str):
            return unicodeBlock.of(char) == "CJK_UNIFIED_IDEOGRAPHS"

        def hasKanji(string: str):
            for char in string:
                if isKanji(char):
                    return True
            return False

        def trimOkurigana(original: str, furigana: str) -> list:
            if (hasKanjiAndHiragana(original)):
                trim_amount = 0
                for i in range(1, len(furigana) + 1):
                    if isKanji(original[len(original) -i]) or original[len(original) -i] != furigana[len(furigana) - i]:
                        trim_amount = i - 1
                        break

                if trim_amount != 0:
                    return [[
                        original[:len(original) - trim_amount],
                        furigana[:len(furigana) - trim_amount]
                    ], [original[len(original) - trim_amount:len(original)]]]

            return [[original, furigana]]

        def getKey(term, key: str) -> str:
            return term[key].replace("\\n", "\n").replace("\\r", "\r")

        kakasi = Kakasi()
        ret = []

        for line in [line.text for line in self.lines] if isinstance(self, TimedLyrics) else self.getText().split("\n"):

            line_data = []
            ret.append(line_data)

            for term in kakasi.convert(line.replace("\n", "\\n").replace("\r", "\\r")):

                orig = getKey(term, "orig")
                hira = getKey(term, "hira")

                if orig != hira and hasKanji(orig):
                    terms = trimOkurigana(orig, hira)
                    for i in range(len(terms)):
                        if len(terms[i]) == 0:
                            continue
                        replacement = TERM_FURI_REPLACEMENTS.get(terms[i][0])
                        if replacement is not None:
                            terms[i][1] = replacement

                    line_data += terms
                else:
                    line_data.append([orig])

        return ret

    @abstractmethod
    def getWithFurigana(self) -> list:
        raise NotImplementedError

class StaticLyrics(Lyrics):
    def __init__(self, id: str, text: str):
        super().__init__(id)
        self.text = text

    def getWithFurigana(self) -> list:
        ret = []

        for line in self._getFuriganaData():
            terms = []
            for term in line:
                if term[0] == "" or (term[0] == "\r" and len(line) > 1):
                    continue
                terms.append({"subterms": [{"text": term[0], "furi": term[1] if len(term) > 1 else None}]})
            if len(terms) > 0:
                ret.append(terms)

        return ret

class TimedLyrics(Lyrics):

    def __init__(self, id: str, xml_data: str):
        super().__init__(id)
        prev_line = None
        index = 0

        self.lines: list[TimedLyrics.Line] = []
        self.first_word: TimedLyrics.Word | None = None

        for line_data in parseXml(xml_data)["wsy"]["line"]:
            line = TimedLyrics.Line(line_data, index)

            self.lines.append(line)
            index += len(line.text)
            if prev_line != None:
                prev_line.next_line = line
                prev_line.words[-1].next_word = line.words[0]
                line.words[0].prev_word = prev_line.words[-1]

            line.prev_line = prev_line
            prev_line = line

            if self.first_word == None:
                self.first_word = line.words[0]

    class Word:

        def __init__(self, data: dict, index: int):
            self.text = data["wordstring"]
            self.start_time = int(data["starttime"]) / 1000
            self.end_time = int(data["endtime"]) / 1000
            self.next_word: TimedLyrics.Word | None = None
            self.prev_word: TimedLyrics.Word | None = None
            self.index = index

    class Line:

        def __init__(self, data: dict, _index: int):
            self.text: str = data["linestring"]
            self.is_space = self.text == None
            self.words: list[TimedLyrics.Word] = []
            self.next_line: TimedLyrics.Line | None = None
            self.prev_line: TimedLyrics.Line | None = None

            self.lines: list = []
            self.first_word: TimedLyrics.Word | None = None

            if self.is_space:
                self.words.append(TimedLyrics.Word({
                    "wordstring": "\n",
                    "starttime": -1,
                    "endtime": -1
                }, _index))
                self.text = "\n"
            else:
                index = _index
                prev_word: TimedLyrics.Word | None = None
                for word_data in data["word"] if isinstance(data["word"], list) else [data["word"]]:
                    if word_data["wordstring"] is None:
                        continue

                    word = TimedLyrics.Word(word_data, index)
                    if len(word.text) > 0:
                        self.words.append(word)
                        index += len(word.text)
                        if prev_word != None:
                            prev_word.next_word = word

                        word.prev_word = prev_word
                        prev_word = word

    def getWithFurigana(self) -> list:
        ret = []
        furigana = self._getFuriganaData()

        for line_i in range(len(self.lines)):

            line_entry = []
            ret.append(line_entry)

            lyr_line = self.lines[line_i]
            if lyr_line.is_space:
                line_entry.append({"subterms": [{"text": "", "furi": None}], "start": -1, "end": -1})
                continue

            furi_line: list = furigana[line_i]
            borrow = 0

            for word in lyr_line.words:
                terms: list[dict[str, str | None]] = []
                i = len(word.text) - borrow
                borrow = max(0, borrow - len(word.text))

                while i > 0 and len(furi_line) > 0:
                    orig = furi_line[0][0]
                    furi = furi_line[0][1] if len(furi_line[0]) > 1 else None

                    if len(orig.strip()) == 0:
                        furi_line.pop(0)
                        continue

                    # Term fits into word
                    if len(orig) <= i:
                        i -= len(orig)
                        terms.append({"text": orig, "furi": furi})
                        furi_line.pop(0)

                    # Term doesn't fit into word
                    else:

                        # Term has no furigana, so we can disect it
                        if furi is None:
                            terms.append({"text": orig[:i], "furi": None})
                            furi_line[0][0] = orig[i:]
                        else:
                            terms.append({"text": orig, "furi": furi})
                            furi_line.pop(0)
                            borrow = len(orig) - i

                        i = 0

                if len(terms) > 0:
                    word_entry = {"subterms": terms, "start": word.start_time, "end": word.end_time}
                    line_entry.append(word_entry)

        return ret

def getLyricsData(song_id: int, lyrics_type: int) -> str | None:
    if lyrics_type <= 0 or lyrics_type > 3:
        raise RuntimeError()

    response = requests.post(
        "https://p1.petitlyrics.com/api/GetPetitLyricsData.php",
        data={
            "key_lyricsId": song_id,
            "lyricsType": lyrics_type,
            "terminalType": "10",
            "clientAppId": "on354007"
        })

    response.raise_for_status()

    try:
        lyrics_data: str = parseXml(response.text)["response"]["songs"]["song"]["lyricsData"]
        return base64.b64decode(lyrics_data).decode("utf-8")
    except (KeyError, UnicodeDecodeError):
        return None

def getLyrics(lyrics_id: str) -> Lyrics | None:

    split = lyrics_id.split(":")
    if len(split) != 2:
        return None

    method = split[0]
    id = split[1]

    match (method):
        case "ptl":
            data = None
            try:
                song_id = int(id)
            except ValueError:
                return None

            for i in range(3, 0, -1):
                data = getLyricsData(song_id, i)
                if data is not None:
                    break

            if data is None:
                return None

            return TimedLyrics(lyrics_id, data) if data.startswith("<wsy>") and data.endswith("</wsy>") else StaticLyrics(lyrics_id, data)
        case _:
            return None

    # data = getLyricsData(song_id, 1)
    # if data is not None:
    #     return StaticLyrics(data)

    # return None

def searchForLyrics(title: str, artist: str | None = None) -> list[dict[str, str]]:

    params = {"title": title}
    if artist is not None:
        params["artist"] = artist

    # Replace all whitespace with the standard character
    for key in params:
        new = ""
        for char in params[key]:
            if char.isspace():
                new += " "
            else:
                new += char
        params[key] = new

    result_start = "<a href=\"/lyrics/"
    result_end = "</a>"

    def getResults():
        response = requests.get("https://petitlyrics.com/search_lyrics", params=params)
        response.raise_for_status()
        data = response.text

        ret = []
        result = {}

        start = data.find(result_start)
        while start != -1:
            href = data[start + len(result_start) : data.find("\"", start + len(result_start) + 1)]
            end = data.find(result_end, start + len(result_start))

            try:
                # If first char is an int, this is the start of a new result
                result_id = int(href)

                if "id" in result:
                    ret.append(result)
                    result = {}
                else:
                    result.clear()

                parsed = parseXml(data[start : end + len(result_end)])
                result["id"] = f"ptl:{result_id}"
                result["name"] = parsed["a"]["span"]["#text"]

            except ValueError:
                split = href.split("/")

                if split[0] in ("artist", "album") and len(split) > 1 and len(split[1]) != 0:
                    parsed = parseXml(data[start : end + len(result_end)])

                    result[f"{split[0]}_id"] = split[1]
                    result[f"{split[0]}_name"] = parsed["a"]["span"]["#text"]

            start = data.find(result_start, end + len(result_end))

        if "id" in result:
            ret.append(result)

        return ret

    ret = getResults()
    if len(ret) == 0 and artist is not None:
        params.pop("artist")
        return getResults()
    return ret
