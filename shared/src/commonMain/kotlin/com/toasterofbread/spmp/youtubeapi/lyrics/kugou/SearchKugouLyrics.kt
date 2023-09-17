package com.toasterofbread.spmp.youtubeapi.lyrics.kugou

import com.google.gson.Gson
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
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


    val result = OkHttpClient().executeResult(request)
    val response = result.getOrNull() ?: return@withContext result.cast()
    val stream = response.body?.charStream()

    val parsed: KugouSearchResponse = try {
        Gson().fromJson(stream!!)
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
    data class Info(val hash: String, val songname: String, val singername: String?, val album_name: String? = null)
}
