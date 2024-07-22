package com.toasterofbread.spmp.youtubeapi.lyrics.petit

import com.mohamedrejeb.ksoup.entities.KsoupEntities
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

private const val SEARCH_RESULT_START = "<a href=\"/lyrics/"
private const val SEARCH_RESULT_END = "</a>"
private const val SEARCH_RESULT_SYNC_TYPE_START = "<span class=\"lyrics-list-sync "

internal suspend fun searchPetitLyrics(
    title: String,
    artist: String? = null
): Result<List<SearchResult>> = runCatching {
    val response: HttpResponse =
        HttpClient().get("https://petitlyrics.com/search_lyrics") {
            url {
                parameters.append("title", title)
                if (artist != null) {
                    parameters.append("artist", artist)
                }
            }

            headers.append("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/114.0")
        }

    val ret: MutableList<SearchResult> = mutableListOf()

    var r_id: Int? = null
    var r_name: String? = null
    var r_sync_type: SongLyrics.SyncType? = null
    var r_artist_name: String? = null
    var r_album_name: String? = null

    val lines: List<String> = response.bodyAsText().split('\n')
    for (element in lines) {
        val line = element.trim()

        if (!line.startsWith(SEARCH_RESULT_START)) {
            if (r_id != null && r_sync_type == null && line.startsWith(SEARCH_RESULT_SYNC_TYPE_START)) {
                val sync_type = line.substring(SEARCH_RESULT_SYNC_TYPE_START.length, line.indexOf('"', SEARCH_RESULT_SYNC_TYPE_START.length))
                r_sync_type = SongLyrics.SyncType.fromKey(sync_type)
            }
            continue
        }

        val href = line.substring(SEARCH_RESULT_START.length, line.indexOf('"', SEARCH_RESULT_START.length + 1))
        val end = line.indexOf(SEARCH_RESULT_END, SEARCH_RESULT_START.length + href.length)

        // If href is an int, this is the start of a new result
        val result_id = href.toIntOrNull()
        if (result_id != null) {
            if (r_id != null) {
                ret.add(
                    SearchResult(
                        r_id.toString(),
                        r_name!!,
                        r_sync_type!!,
                        r_artist_name,
                        r_album_name
                    )
                )
            }
            else {
                r_sync_type = null
                r_artist_name = null
                r_album_name = null
            }

            r_id = result_id
            r_name = line.substring(0, end + SEARCH_RESULT_END.length).getHtmlBody()
        }
        else {
            val split: List<String> = href.split('/')

            when (split[0]) {
                "artist" -> {
                    r_artist_name = line.substring(0, end + SEARCH_RESULT_END.length).getHtmlBody()
                }
                "album" -> {
                    r_album_name = line.substring(0, end + SEARCH_RESULT_END.length).getHtmlBody()
                }
            }
        }
    }

    if (r_id != null) {
        ret.add(
            SearchResult(
                r_id.toString(),
                r_name!!,
                r_sync_type!!,
                r_artist_name,
                r_album_name
            )
        )
    }

    return@runCatching ret
}

private fun String.getHtmlBody(): String {
    var ret: String? = null
    val handler: KsoupHtmlHandler =
        KsoupHtmlHandler.Builder()
            .onText { ret = it }
            .build()

    val parser: KsoupHtmlParser = KsoupHtmlParser(handler)
    parser.write(this)
    parser.end()

    return ret?.let { KsoupEntities.decodeHtml(it) } ?: throw RuntimeException("HTML body not found in string '$this'")
}
