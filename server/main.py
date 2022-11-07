from argparse import ArgumentParser
from server import Server
from os import path
from spectre7 import utils
import json

DEFAULT_CREDENTIALS_PATH = path.join(path.dirname(__file__), "credentials.json")
DEFAULT_PORT = 3232

def main():

    parser = ArgumentParser("SPMP Server")
    parser.add_argument("-p", "--port", type=str, default=DEFAULT_PORT)
    parser.add_argument("-c", "--credentials", type=str, dest="creds_path", default=DEFAULT_CREDENTIALS_PATH)
    args = parser.parse_args()

    if not path.isfile(args.creds_path):
        utils.err(f"No credentials file found at {path.abspath(args.creds_path)}")
        return

    f = open(args.creds_path, "r")
    data = f.read()
    f.close()
    try:
        creds = json.loads(data)
    except json.JSONDecodeError as e:
        utils.err(f"Parsing credentials file at {path.abspath(args.creds_path)} failed:\n" + e.msg)
        return

    server = Server(args.port, creds)
    server.start()

if __name__ == "__main__":
    main()
