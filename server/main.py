from argparse import ArgumentParser
from server import Server
from os import path
import json
from spectre7 import utils

DEFAULT_CREDENTIALS_PATH = path.join(path.dirname(__file__), "credentials.json")
DEFAULT_PORT = 3232

def runServer(integrated: bool, port: int = DEFAULT_PORT):
    f = open(DEFAULT_CREDENTIALS_PATH, "r")
    data = f.read()
    f.close()

    try:
        creds = json.loads(data)
    except json.JSONDecodeError as e:
        utils.err(f"Parsing credentials file at {path.abspath(DEFAULT_CREDENTIALS_PATH)} failed:\n" + e.msg)
        return

    server = Server(port, creds, integrated=integrated)
    server.start()

    return server

def main():

    parser = ArgumentParser("SPMP Server")
    parser.add_argument("-p", "--port", type=str, default=DEFAULT_PORT)
    parser.add_argument("-c", "--credentials", type=str, dest="creds_path", default=DEFAULT_CREDENTIALS_PATH)
    parser.add_argument("-i", "--integrated", action="store_true")
    args = parser.parse_args()

    if not path.isfile(args.creds_path):
        utils.err(f"No credentials file found at {path.abspath(args.creds_path)}")
        return

    return runServer(args.integrated, args.port)

if __name__ == "__main__":
    main()
