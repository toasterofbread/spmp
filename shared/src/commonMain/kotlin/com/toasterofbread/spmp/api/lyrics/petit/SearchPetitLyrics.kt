package com.toasterofbread.spmp.api.lyrics.petit

import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.api.getOrThrowHere
import com.toasterofbread.spmp.api.lyrics.LyricsSource.SearchResult
import com.toasterofbread.spmp.model.SongLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

private const val SEARCH_RESULT_START = "<a href=\"/lyrics/"
private const val SEARCH_RESULT_END = "</a>"
private const val SEARCH_RESULT_SYNC_TYPE_START = "<span class=\"lyrics-list-sync "

internal suspend fun searchPetitLyrics(params: String): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("https://petitlyrics.com/search_lyrics$params")
        .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/114.0")
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val ret = mutableListOf<SearchResult>()

    var r_id: Int? = null
    var r_name: String? = null
    var r_sync_type: SongLyrics.SyncType? = null
    var r_artist_name: String? = null
    var r_album_name: String? = null

    val lines = result.getOrThrowHere().body!!.string().split('\n')
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
            r_name = Jsoup.parse(line.substring(0, end + SEARCH_RESULT_END.length)).body().text()
        }
        else {
            val split = href.split('/')

            when (split[0]) {
                "artist" -> {
                    r_artist_name = Jsoup.parse(line.substring(0, end + SEARCH_RESULT_END.length)).body().text()
                }
                "album" -> {
                    r_album_name = Jsoup.parse(line.substring(0, end + SEARCH_RESULT_END.length)).body().text()
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

    return@withContext Result.success(ret)
}
