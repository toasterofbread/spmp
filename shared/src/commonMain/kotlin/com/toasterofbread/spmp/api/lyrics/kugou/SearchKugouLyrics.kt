package com.toasterofbread.spmp.api.lyrics.kugou

import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.api.lyrics.LyricsSource
import com.toasterofbread.spmp.model.SongLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

internal suspend fun searchKugouLyrics(title: String, artist: String?): Result<List<LyricsSource.SearchResult>> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(
            "https://mobileservice.kugou.com/api/v3/search/song"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("version", "9108")
                .addQueryParameter("plat", "0")
                .addQueryParameter("pagesize", "8")
                .addQueryParameter("showtype", "0")
                .addQueryParameter("keyword", if (artist == null) title else "$artist - $title")
                .build()
        )
        .build()

    val result = Api.request(request, is_gzip = false)
    val response = result.getOrNull() ?: return@withContext result.cast()
    val stream = response.body?.charStream()

    val parsed: KugouSearchResponse = try {
        Klaxon().parse(stream!!)!!
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream?.close()
    }

    return@withContext Result.success(
        parsed.data.info.map { info ->
            LyricsSource.SearchResult(
                info.hash,
                info.songname,
                SongLyrics.SyncType.LINE_SYNC,
                info.singername,
                info.album_name
            )
        }
    )
}

private data class KugouSearchResponse(
    val status: Int,
    val data: Data
) {
    data class Data(val info: List<Info>)
    data class Info(val hash: String, val songname: String, val singername: String? = null, val album_name: String? = null)
}
