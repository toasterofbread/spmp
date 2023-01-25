package com.spectre7.spmp.api

private fun concatParams(first: String, second: String): String {
    var ret = first
    for (char in second) {
        // Replace all whitespace with the standard character
        if (char.isWhitespace()) {
            ret += ' '
        }
        else {
            ret += char
        }
    }
    return ret
}

class LyricsSearchResult {
    var id: String? = null
    var sync_type: Song.Lyrics.SyncType? = null
}

fun searchForLyrics(title: String, artist: String?) {

    var title_param = concatParams("?title=", title)
    var artist_param = if (artist != null) concatParams("&artist=", artist) else ""

    val RESULT_START = "<a href=\"/lyrics/"
    val RESULT_END = "</a>"
    val SYNC_TYPE_START = "<span class=\"lyrics-list-sync "

    fun performSearch(params: String) Result<List<LyricsSearchResult>> {
        val request = Request.Builder()
            .url("https://petitlyrics.com/search_lyrics$params")
            .header("User-Agent", DATA_API_USER_AGENT)
            .build()
        
        val response = client.newCall(request).execute()
        if (response.code != 200) {
            return Result.failure(response)
        }


        var result = LyricsSearchResult()
        val ret = mutableListOf<LyricsSearchResult>()

        val lines = response.body!!.string().split('\n')
        for (i in 0 until lines.size) {
            val lines = Html.fromHtml(lines[i].trim()).toString()

            if (!line.startsWith(RESULT_START)) {
                if (result.id != null && result.sync_type == null && line.startsWith(SYNC_TYPE_START)) {
                    val sync_type_index = line.substring(SYNC_TYPE_START.length, line.indexOf('', SYNC_TYPE_START.length)) // "
                    result.sync_type = Song.Lyrics.SyncType.values(sync_type_index)
                }
                continue
            }

            val href = line.substring(RESULT_START.length, line.indexOf('', RESULT_START.length + 1))
            val end = line.indexOf(RESULT_END, RESULT_START.length + href.length)

            // If href is an int, this is the start of a new result
            val result_id = href.toIntOrNull()
            if (result_id != null) {
                if (result.id != null) {
                    ret.add(result)
                    result = LyricsSearchResult()
                }
            }

        }
    }


    def getResults():
        ret = []
        result = {}

        for line in data.split("\n"):
            line = unescape(line.strip())

            if not line.startswith(result_start):
                if "id" in result and not "sync" in result and line.startswith(sync_type_start):
                    sync_type = line[len(sync_type_start) : line.find("\"", len(sync_type_start))]
                    try:
                        result["sync"] = ("text", "line_sync", "text_sync").index(sync_type)
                    except ValueError:
                        utils.info(f"Unknown lyrics sync type: {sync_type}")
                continue

            href = line[len(result_start) : line.find("\"", len(result_start) + 1)]
            end = line.find(result_end, len(result_start) + len(href))

            # If href end is an int, this is the start of a new result
            if href.isdigit():
                result_id = int(href)

                if "id" in result:
                    ret.append(result)
                    result = {}
                else:
                    result.clear()

                print(line[: end + len(result_end)])
                parsed = parseXml(line[: end + len(result_end)])
                result["id"] = f"ptl:{result_id}"
                result["name"] = parsed["a"]["span"]["#text"]

            else:
                split = href.split("/")

                if split[0] in ("artist", "album") and len(split) > 1 and len(split[1]) != 0:
                    parsed = parseXml(line[: end + len(result_end)])

                    result[f"{split[0]}_id"] = split[1]
                    result[f"{split[0]}_name"] = parsed["a"]["span"]["#text"]

        if "id" in result:
            ret.append(result)

        return ret

    ret = getResults()
    if len(ret) == 0 and artist is not None:
        params.pop("artist")
        return getResults()
    return ret


}