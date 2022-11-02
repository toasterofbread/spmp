import base64
import requests
from pykakasi import Kakasi
from unicodeblock import blocks as unicodeBlock
from xmltodict import parse as parseXml

class Lyrics:

    def getFurigana(self):
        kakasi = Kakasi()
        ret = []

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

        for line in [line.text for line in self.lines] if type(self) == TimedLyrics else self.text.split("\n"):

            line_data = []
            ret.append(line_data)

            for term in kakasi.convert(
                    line.replace("\n", "\\n").replace("\r", "\\r")):

                orig = getKey(term, "orig")
                hira = getKey(term, "hira")

                if orig != hira:
                    line_data += trimOkurigana(orig, hira)
                else:
                    line_data.append([orig])

        return ret

class StaticLyrics(Lyrics):
    def __init__(self, text: str):
        self.text = text

class TimedLyrics(Lyrics):

    def __init__(self, xml_data: str):
        prev_line = None
        index = 0

        self.lines = []
        self.first_word: TimedLyrics.Word | None = None

        for line_data in parseXml(xml_data)["wsy"]["line"]:
            line = TimedLyrics.Line(line_data, index)
            if len(line.words) > 0:
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
            prev_word: TimedLyrics.Word | None = None
            index = _index

            self.text: str = data["linestring"]
            self.words = []
            self.next_line: TimedLyrics.Line | None = None
            self.prev_line: TimedLyrics.Line | None = None

            self.lines: list = []
            self.first_word: TimedLyrics.Word | None = None

            for word_data in data["word"] if type(
                    data["word"]) == list else [data["word"]]:
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

def getLyricsData(song_id: int, lyrics_type: int) -> str:
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

    lyrics_data: str = parseXml(
        response.text)["response"]["songs"]["song"]["lyricsData"]
    return base64.b64decode(lyrics_data).decode("utf-8")

def getLyrics(song_id: int) -> Lyrics:
    ret: Lyrics | None = None

    # try:
    ret = TimedLyrics(getLyricsData(song_id, 3))
    # catch (e: SAXParseException):
    #         ret = StaticLyrics(getLyricsData(song_id, 1))

    return ret

def findLyricsId(title: str, artist = None):
    params = {"title": title}
    if artist is not None:
        params["artist"] = artist

    response = requests.get("https://petitlyrics.com/search_lyrics",
                                                    params=params)
    response.raise_for_status()

    id_index = response.text.find("				<a href=\"/lyrics/")
    if id_index < 0:
        return None

    return int(response.text[id_index + 21:][:7])
