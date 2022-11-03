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

    server = Server(args.port, args.firefox, API_KEY)
    server.start(not args.safe)

if __name__ == "__main__":
    main()
