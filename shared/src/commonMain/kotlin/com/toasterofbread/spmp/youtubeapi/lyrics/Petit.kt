package com.toasterofbread.spmp.youtubeapi.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.lyrics.petit.parseTimedLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.petit.searchPetitLyrics
import dev.toastbits.composekit.util.substringBetween
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lyrics_source_petit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration

private const val DATA_START: String = "<lyricsData>"
private const val DATA_END: String = "</lyricsData>"
private const val ENCODING_START: String = "encoding='"
private const val ENCODING_END: String = "'"

internal class PetitLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    @Composable
    override fun getReadable(): String = stringResource(Res.string.lyrics_source_petit)
    override fun getColour(): Color = Color(0xFFBD0A0F)
    override fun getUrlOfId(id: String): String? = "https://petitlyrics.com/lyrics/$id"

    override suspend fun getLyrics(
        lyrics_id: String,
        context: AppContext
    ): Result<SongLyrics> = runCatching {
        var exception: Throwable? = null

        for (sync_type in SongLyrics.SyncType.byPriority()) {
            val result = getLyricsData(lyrics_id.toInt(), sync_type)

            val data = result.getOrNull()
            if (data == null) {
                if (exception == null) {
                    exception = result.exceptionOrNull()
                }
                continue
            }

            if (data.startsWith("<wsy>")) {
                val lyrics: List<List<SongLyrics.Term>> = parseTimedLyrics(data).getOrThrow()

                return@runCatching SongLyrics(
                    LyricsReference(source_index, lyrics_id),
                    sync_type,
                    lyrics
                )
            }
            else {
                return@runCatching SongLyrics(
                    LyricsReference(source_index, lyrics_id),
                    SongLyrics.SyncType.NONE,
                    parseStaticLyrics(data)
                )
            }
        }

        throw (exception ?: IllegalStateException())
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getLyricsData(
        lyrics_id: Int,
        sync_type: SongLyrics.SyncType
    ): Result<String> = runCatching {
        val response: HttpResponse =
            HttpClient().post("https://p1.petitlyrics.com/api/GetPetitLyricsData.php") {
                expectSuccess = true
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("key_lyricsId=$lyrics_id&lyricsType=${sync_type.ordinal + 1}&terminalType=10&clientAppId=on354007")
            }

        val xml_data: String = response.bodyAsText()
        val lyrics_data_encoding: String = xml_data.substringBetween(ENCODING_START, ENCODING_END) ?: "UTF-8"

        try {
            val lyrics_data: ByteArray = Base64.decode(xml_data.substringBetween(DATA_START, DATA_END)!!)
            val string: String = lyrics_data.decodeToString()
            if (string.contains('�')) {
                throw RuntimeException("Lyrics decoding failed ($lyrics_data_encoding)")
            }

            return@runCatching string
        }
        catch (e: Throwable) {
            throw RuntimeException("Decoding lyrics data $lyrics_id with encoding $lyrics_data_encoding failed", e)
        }
    }

    override suspend fun searchForLyrics(
        title: String,
        artist_name: String?,
        album_name: String?,
        duration: Duration?
    ): Result<List<SearchResult>> = runCatching {
        val search_results: List<SearchResult> = searchPetitLyrics(title).getOrThrow()
        if (search_results.isEmpty() && artist_name != null) {
            return@runCatching searchPetitLyrics(title, artist_name).getOrThrow()
        }
        return@runCatching search_results
    }
}
