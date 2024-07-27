package com.toasterofbread.spmp.youtubeapi.lyrics.kugou

import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.JsonHttpClient
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal suspend fun searchKugouLyrics(
    title: String,
    artist: String?
): Result<List<LyricsSource.SearchResult>> = runCatching {
    val response: HttpResponse =
        JsonHttpClient.get("https://mobileservice.kugou.com/api/v3/search/song") {
            url {
                parameters.append("version", "9108")
                parameters.append("plat", "0")
                parameters.append("pagesize", "8")
                parameters.append("showtype", "0")
                parameters.append("keyword", if (artist == null) title else "$artist - $title")
            }
        }

    val parsed: KugouSearchResponse =
        // Response content-type isn't JSON for some reason
        Json { ignoreUnknownKeys = true }.decodeFromString(response.bodyAsText())

    return@runCatching parsed.data.info.map { info ->
        LyricsSource.SearchResult(
            info.hash,
            info.songname,
            SongLyrics.SyncType.LINE_SYNC,
            info.singername,
            info.album_name
        )
    }
}

@Serializable
internal data class KugouSearchResponse(
    val status: Int,
    val data: Data
) {
    @Serializable
    data class Data(val info: List<Info>)
    @Serializable
    data class Info(val hash: String, val songname: String, val singername: String?, val album_name: String? = null)
}
