import json
from selenium.webdriver.common.by import By
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from seleniumwire import webdriver
from bs4 import BeautifulSoup
from urllib.parse import parse_qs
from flask import Flask, request, abort
import subprocess
from time import time, sleep
from threading import Thread, Lock
from apscheduler.schedulers.background import BackgroundScheduler
import atexit
from functools import wraps

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

def getSeleniumDriver(headers: dict, headless: bool = True):

    options = webdriver.FirefoxOptions()
    options.binary_location = FIREFOX_PATH
    options.headless = headless

    ret = webdriver.Firefox(options = options)

    def interceptor(request):
        for key in headers:
            del request.headers[key]
            request.headers[key] = headers[key]
    ret.request_interceptor = interceptor

    return ret

def scrapeYTMusicHome(headers: dict, rows: int = -1) -> list[dict]:
    if rows == 0:
        return []

    driver = getSeleniumDriver(headers)
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

def main():
    app = Flask("SpMp Server")
    start_time = time()

    cached_feed = None
    refresh_mutex = Lock()

    def requireKey(func):
        @wraps(func)
        def decoratedFunction(*args, **kwargs):
            if request.args.get("key") and request.args.get("key") == API_KEY:
                return func(*args, **kwargs)
            else:
                abort(401)
        return decoratedFunction


    @app.route("/")
    def info():
        return "Hello World!"

    @app.route("/status")
    def status():
        return json.dumps({
            "uptime": int(time() - start_time)
        })

    @app.route("/feed/get")
    @requireKey
    def getFeed():
        if cached_feed is None:
            refresh_mutex.acquire()
            refresh_mutex.release()
        return json.dumps(cached_feed)

    @app.route("/feed/getlatest")
    @requireKey
    def getLatestFeed():
        if refresh_mutex.locked():
            refresh_mutex.acquire()
            refresh_mutex.release()
        return json.dumps(cached_feed)

    @app.route("/feed/getrefreshed")
    @requireKey
    def getRefreshedFeed():
        if not refresh_mutex.locked():
            refreshFeed()

        refresh_mutex.acquire()
        refresh_mutex.release()

        return json.dumps(cached_feed)

    @app.route("/feed/refresh")
    @requireKey
    def _refreshFeed():
        return refreshFeed()

    def refreshFeed():
        if refresh_mutex.locked():
            return json.dumps({"result": 1, "error": "Already refreshing"})

        def thread():
            nonlocal cached_feed
            refresh_mutex.acquire()
            cached_feed = scrapeYTMusicHome(HEADERS, RECOMMENDED_ROWS)
            refresh_mutex.release()

        Thread(target=thread).start()
        return json.dumps({"result": 0})

    refreshFeed()

    scheduler = BackgroundScheduler()
    scheduler.add_job(func=refreshFeed, trigger="interval", seconds = 60 * 60 * 6)
    scheduler.start()

    atexit.register(lambda : scheduler.shutdown())

    subprocess.Popen(["ngrok", "http", str(PORT)], stdout=open("/dev/null"))
    app.run(port = PORT)

if __name__ == "__main__":
    main()