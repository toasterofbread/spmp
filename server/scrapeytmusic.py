from time import sleep
from urllib.parse import parse_qs
from bs4 import BeautifulSoup
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.wait import WebDriverWait
from seleniumwire import webdriver


def getSeleniumDriver(headers: dict, firefox_path: str, headless: bool = True):
  options = webdriver.FirefoxOptions()
  options.binary_location = firefox_path
  options.headless = headless

  ret = webdriver.Firefox(options=options, service_log_path="/dev/null")

  def interceptor(request):
    for key in headers:
      del request.headers[key]
      request.headers[key] = headers[key]

  ret.request_interceptor = interceptor

  return ret


def scrapeYTMusicHome(headers: dict,
                      firefox_path: str,
                      rows: int = 5,
                      shouldCancel=lambda: bool(False)):
  if rows == 0:
    return []

  driver = getSeleniumDriver(headers, firefox_path)
  driver.get("https://music.youtube.com")

  if shouldCancel():
    return None

  def getBrowseItemPlaylistId(browse_id: str):
    driver.get(f"https://music.youtube.com/browse/{browse_id}")
    WebDriverWait(driver, 10).until(
      EC.url_contains("https://music.youtube.com/playlist?list="))
    ret = driver.current_url
    return ret

  def getSectionCount():
    sections = driver.find_element(value="contents").find_elements(
      By.XPATH, "*")
    return len(sections)

  prev_height = driver.execute_script("return document.body.scrollHeight")
  while rows > 0 and getSectionCount() < rows:
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    sleep(0.5)

    height = driver.execute_script("return document.body.scrollHeight")
    if height == prev_height:
      break
    prev_height = height

    if shouldCancel():
      return None

  soup = BeautifulSoup(driver.page_source, "html.parser")
  ret = []

  for section in soup.find_all(
      "div", class_="style-scope ytmusic-section-list-renderer",
      id="contents")[0].find_all(recursive=False,
                                 limit=rows if rows > 0 else None):

    details = section.find("div", id="details").find("yt-formatted-string",
                                                     recursive=False)
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

    if shouldCancel():
      return None

  driver.close()
  return ret
