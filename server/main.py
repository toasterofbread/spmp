from argparse import ArgumentParser
from server import Server
import lyrics

PORT = 3232
API_KEY = "***REMOVED***"
FIREFOX_PATH = "/usr/lib/firefox-developer-edition/firefox"

def main():
    # parser = ArgumentParser("SPMP Server")
    # parser.add_argument("-f", "--firefox", type=str, help="Path to a Firefox executable")
    # args = parser.parse_args()

    # server = Server(PORT, args.firefox if args.firefox else FIREFOX_PATH, API_KEY)
    # server.start()

    id = lyrics.findLyricsId("ベノム")
    lc = lyrics.getLyrics(id)

    print(lc.getFurigana())


if __name__ == "__main__":
    main()
