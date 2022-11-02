from argparse import ArgumentParser
from server import Server

DEFAULT_PORT = 3232
API_KEY = "6d5ff86c7ee15d07e0d7398b1cb0e9d1"

def main():
    parser = ArgumentParser("SPMP Server")
    parser.add_argument("-f", "--firefox", type=str, default="firefox", help="Path to a Firefox executable")
    parser.add_argument("-s", "--safe", action="store_true")
    parser.add_argument("-p", "--port", type=str, default=DEFAULT_PORT)
    args = parser.parse_args()

    # server = Server(args.port, args.firefox, API_KEY)
    # server.start(not args.safe)

    import lyrics as Lyrics
    import json

    id = Lyrics.findLyricsId("daybreak frontline")
    if id is None:
        print("NONE")
        return

    lyrics = Lyrics.getLyrics(id)
    furi = lyrics.getFurigana()

    ret = []

    for line in range(len(lyrics.lines)):
        line_data = []
        ret.append(line_data)
        furi_outer = 0
        furi_inner = 0
        borrow = 0

        print(lyrics.lines[line].text)
        print(furi[line])
        print("\n\n")

        continue

        for word in lyrics.lines[line].words:
            word_data = {"terms": [], "start_time": word.start_time, "end_time": word.end_time}
            line_data.append(word_data)
            i = len(word.text) - borrow
            borrow = 0

            while i > 0:
                print(len(furi[line]), " | ", line, " | ", furi_outer)
                el = furi[line][furi_outer]
                furi_outer += 1

                # Element fits in word
                if len(el[0]) <= i:
                    i -= len(el[0])
                    word_data["terms"].append(el)

                # Element longer than word

                # Has furigana
                elif len(el) > 0:
                    pass
                # No furigana
                else:
                    word_data["terms"].append(el)
                    furi_inner = i
                    i = 0

    # End result:
    lyrics = [ # Lyrics
        [ # Line
            { # Term
                "terms": [
                    ["目指", "めざ"]
                ],
                "start_time": 0,
                "end_time": 1
            },
            { # Term
                "terms": [
                    ["していく."]
                ],
                "start_time": 1,
                "end_time": 2
            }
        ]
    ]

if __name__ == "__main__":
    main()
