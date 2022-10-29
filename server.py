import atexit
import json
import multiprocessing as mp
import subprocess
import sys
from argparse import ArgumentParser
from functools import wraps
from threading import Thread
from time import sleep, time
from urllib.parse import parse_qs
import psutil
import requests
from apscheduler.schedulers.background import BackgroundScheduler
from bs4 import BeautifulSoup
from flask import Flask, abort, request
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.wait import WebDriverWait
from seleniumwire import webdriver
from spectre7 import utils
from urllib3.exceptions import ProtocolError
from waitress import serve

PORT = 3232
API_KEY = "***REMOVED***"
FIREFOX_PATH = "/usr/lib/firefox-developer-edition/firefox"
RECOMMENDED_ROWS = 10

HEADERS = {
    'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0',
    'accept': '*/*',
    'accept-language': 'ja',
    'content-type': 'application/json',
    'x-goog-visitor-id': 'Cgt1TjR0ckUtOVNXOCiKnOmaBg%3D%3D',
    'x-youtube-client-name': '67',
    'x-youtube-client-version': '1.20221019.01.00',
    'authorization': 'SAPISIDHASH 1666862603_ad3286857ed8177c1e0f0f16fc678aaff93ad310',
    'x-goog-authuser': '1',
    'x-origin': 'https://music.youtube.com',
    'origin': 'https://music.youtube.com',
    'alt-used': 'music.youtube.com',
    'connection': 'keep-alive',
    'cookie': '***REMOVED***',
    'sec-fetch-dest': 'empty',
    'sec-fetch-mode': 'same-origin',
    'sec-fetch-site': 'same-origin',
    'pragma': 'no-cache',
    'cache-control': 'no-cache',
    'te': 'trailers',
}

def getSeleniumDriver(headers: dict, firefox_path: str = FIREFOX_PATH, headless: bool = True):
    options = webdriver.FirefoxOptions()
    options.binary_location = firefox_path
    options.headless = headless

    ret = webdriver.Firefox(options = options, service_log_path = "/dev/null")

    def interceptor(request):
        for key in headers:
            del request.headers[key]
            request.headers[key] = headers[key]
    ret.request_interceptor = interceptor

    return ret

def scrapeYTMusicHome(headers: dict, rows: int = -1, firefox_path: str = FIREFOX_PATH) -> list:
    if rows == 0:
        return []

    driver = getSeleniumDriver(headers, firefox_path)
    driver.get("https://music.youtube.com")

    def getBrowseItemPlaylistId(browse_id: str):
        driver.get(f"https://music.youtube.com/browse/{browse_id}")
        WebDriverWait(driver, 10).until(EC.url_contains("https://music.youtube.com/playlist?list="))
        ret = driver.current_url
        return ret

    def getSectionCount():
        sections = driver.find_element(value = "contents").find_elements(By.XPATH, "*")
        return len(sections)

    prev_height = driver.execute_script("return document.body.scrollHeight")
    while rows > 0 and getSectionCount() < rows:
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        sleep(0.5)

        height = driver.execute_script("return document.body.scrollHeight")
        if height == prev_height:
            break
        prev_height = height

    soup = BeautifulSoup(driver.page_source, "html.parser")
    ret = []

    for section in soup.find_all("div", class_="style-scope ytmusic-section-list-renderer", id="contents")[0].find_all(recursive=False, limit = rows if rows > 0 else None):

        details = section.find("div", id="details").find("yt-formatted-string", recursive=False)
        details_a = details.find("a")
        title = details.text if details_a is None else details_a.text

        subtitle = section.find("h2", id="content-group")["aria-label"]
        if subtitle == title:
            subtitle = None
        else:
            subtitle = subtitle[:-(len(title) + 1)]

        entry = {
            "title": details.text if details_a is None else details_a.text,
            "subtitle": subtitle,
            "items": []
        }
        ret.append(entry)

        for item in section.find("ul", id="items").find_all(recursive=False):
            item_entry = {}
            entry["items"].append(item_entry)

            href: str = item.find("a")["href"]

            if href.startswith("channel/"):
                type_ = "artist"
                id = href[8:]
            elif href.startswith("watch?v="):
                type_ = "song"

                q = parse_qs(href[6:])
                id = q["v"][0]

                if "list" in q:
                    item_entry["playlist_id"] = q["list"][0]

            elif href.startswith("playlist"):
                type_ = "playlist"
                id = href[14:]

            elif href.startswith("browse"):
                type_ = "playlist"
                id = getBrowseItemPlaylistId(href[7:])

            else:
                raise RuntimeError(href)

            item_entry["type"] = type_
            item_entry["id"] = id

    driver.close()
    return ret

def getNgrokUrl():
    result = requests.get("https://api.ngrok.com/tunnels", headers={"Authorization": "Bearer ***REMOVED***", "Ngrok-Version": "2"})
    return json.loads(result.text)["tunnels"][0]["public_url"]

def requireKey(func):
    @wraps(func)
    def decoratedFunction(*args, **kwargs):
        if request.remote_addr == "127.0.0.1" or (request.args.get("key") and request.args.get("key") == API_KEY):
            return func(*args, **kwargs)
        else:
            abort(401)
    return decoratedFunction

def isMutexLocked(mutex):
    locked = mutex.acquire(blocking=False)
    if locked == False:
        return True
    else:
        mutex.release()
        return False

class Server:

    def __init__(self, port: int, firefox_path: str = FIREFOX_PATH):
        self.port = port
        self.firefox_path = firefox_path

        self.app = Flask("SpMp Server")
        self.start_time = 0
        self.restart_queue = None

        manager = mp.Manager()
        self.refresh_mutex = manager.Lock()
        self.cached_feed = manager.Value("l", [])
        self.cached_feed_set = manager.Value("b", False)

        @self.app.route("/")
        def index():
            return "Hello World!"

        @self.app.route("/status/")
        def status():
            return json.dumps({
                "uptime": int(time() - self.start_time)
            })

        @self.app.route("/restart/")
        @requireKey
        def _restart():
            self.restart()
            return "Restarting"

        @self.app.route("/update/")
        @requireKey
        def update():
            subprocess.getoutput("git fetch")
            if (subprocess.getoutput(f"git diff --stat main origin/main") == ""):
                return "Already running the latest version"
            subprocess.getoutput("git pull")
            self.restart()
            return "Updated and restarting"

        @self.app.route("/feed/")
        @requireKey
        def feed():
            if not self.cached_feed_set.get():
                self.refresh_mutex.acquire()
                self.refresh_mutex.release()
            return json.dumps(self.cached_feed.get())

        @self.app.route("/feed/latest/")
        @requireKey
        def latestFeed():
            if isMutexLocked(self.refresh_mutex):
                self.refresh_mutex.acquire()
                self.refresh_mutex.release()
            return json.dumps(self.cached_feed.get())

        @self.app.route("/feed/refreshed/")
        @requireKey
        def refreshedFeed():
            if not isMutexLocked(self.refresh_mutex):
                self.refreshFeed()

            self.refresh_mutex.acquire()
            self.refresh_mutex.release()

            return json.dumps(self.cached_feed.get())

        @self.app.route("/feed/refresh/")
        @requireKey
        def refreshFeed():
            return self.refreshFeed()

    def refreshFeed(self):
        if isMutexLocked(self.refresh_mutex):
            return json.dumps({"result": 1, "error": "Already refreshing"})

        def thread():
            try:
                self.refresh_mutex.acquire()
                utils.log("Refreshing feed...")
                self.cached_feed.set(scrapeYTMusicHome(HEADERS, RECOMMENDED_ROWS, self.firefox_path))
                self.cached_feed_set.set(True)
                utils.log("Feed refresh completed")
                self.refresh_mutex.release()
            except ProtocolError as e:
                try:
                    print(e)
                except:
                    pass

        Thread(target=thread).start()
        return json.dumps({"result": 0})

    def start(self):
        self.start_time = time()
        sys.stdout = open("/dev/null", "w")
        # sys.stderr = sys.stdout

        self.scheduler = BackgroundScheduler()
        self.scheduler.add_job(func=self.refreshFeed, trigger="interval", seconds = 60 * 60 * 6)
        self.scheduler.start()
        atexit.register(self.scheduler.shutdown)

        def run(queue):
            self.restart_queue = queue
            serve(self.app, host="0.0.0.0", port = PORT)

        self.ngrok_process = subprocess.Popen(f"exec ngrok http {self.port}", shell=True, stdout=open("/dev/null", "w"))

        q = mp.Queue()
        app_process = mp.Process(target=run, args=(q,))
        app_process.start()

        for msg in (
            f"Running locally on http://127.0.0.1:{self.port}",
            f"Public URL is {getNgrokUrl()}"
        ):
            utils.info(f"* {msg}")

        self.refreshFeed()

        # Wait for restart request
        while True:
            try:
                sleep(1)
            except KeyboardInterrupt:
                utils.info("\nExiting...")
                exit()

            if not q.empty():
                break

        app_process.terminate()
        subprocess.call([sys.executable] + sys.argv)

    def restart(self):
        self.scheduler.shutdown()
        psutil.Process(self.ngrok_process.pid).kill()
        self.restart_queue.put("foo")
        utils.log("Restarting...")

def main():
    parser = ArgumentParser("SPMP Server")
    parser.add_argument("-f", "--firefox", type=str, help="Path to a Firefox executable")
    args = parser.parse_args()

    server = Server(PORT, args.firefox if args.firefox else FIREFOX_PATH)
    server.start()

if __name__ == "__main__":
    main()
