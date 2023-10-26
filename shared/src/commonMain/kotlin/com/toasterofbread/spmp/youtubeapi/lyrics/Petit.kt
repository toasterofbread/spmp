package com.toasterofbread.spmp.youtubeapi.lyrics

import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.spmp.youtubeapi.lyrics.petit.parseTimedLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.petit.searchPetitLyrics
import com.toasterofbread.toastercomposetools.utils.common.substringBetween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.Base64
import java.util.UnknownFormatConversionException

private const val DATA_START = "<lyricsData>"
private const val DATA_END = "</lyricsData>"
private const val ENCODING_START = "encoding='"
private const val ENCODING_END = "'"

internal class PetitLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    override fun getReadable(): String = getString("lyrics_source_petit")
    override fun getColour(): Color = Color(0xFFBD0A0F)
    
    override suspend fun getLyrics(lyrics_id: String, context: AppContext): Result<SongLyrics> {
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
                val parse_result = parseTimedLyrics(data)
                val lyrics = parse_result.getOrNull() ?: return parse_result.cast()

                return Result.success(
                    SongLyrics(
                        LyricsReference(source_index, lyrics_id),
                        sync_type,
                        lyrics
                    )
                )
            }
            else {
                return Result.success(
                    SongLyrics(
                        LyricsReference(source_index, lyrics_id),
                        SongLyrics.SyncType.NONE,
                        parseStaticLyrics(data)
                    )
                )
            }
        }

        return Result.failure(exception ?: IllegalStateException())
    }

    private suspend fun getLyricsData(lyrics_id: Int, sync_type: SongLyrics.SyncType): Result<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://p1.petitlyrics.com/api/GetPetitLyricsData.php")
            .post(
                "key_lyricsId=$lyrics_id&lyricsType=${sync_type.ordinal + 1}&terminalType=10&clientAppId=on354007"
                    .toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType())
            )
            .build()
    
        val response =
            OkHttpClient().executeResult(request)
                .fold(
                    { it },
                    { return@withContext Result.failure(it) }
                )

        val xml: String
        try {
            xml = response.body!!.string()
        }
        catch (e: Throwable) {
            return@withContext Result.failure(e)
        }

        val lyrics_data_encoding: String = xml.substringBetween(ENCODING_START, ENCODING_END) ?: "UTF-8"

        try {
            val string_decoder = Charset.forName(lyrics_data_encoding)
            val lyrics_data: ByteArray = Base64.getDecoder().decode(xml.substringBetween(DATA_START, DATA_END))

            val string = string_decoder.decode(ByteBuffer.wrap(lyrics_data)).toString()
            if (string.contains('�')) {
                throw UnknownFormatConversionException(lyrics_data_encoding)
            }

            return@withContext Result.success(string)
        }
        catch (e: Throwable) {
            return@withContext Result.failure(RuntimeException("Decoding lyrics data with encoding $lyrics_data_encoding failed", e))
        }
    }
    
    override suspend fun searchForLyrics(title: String, artist_name: String?): Result<List<SearchResult>> {
        val title_param = concatParams("?title=", title)
        val artist_param = if (artist_name != null) concatParams("&artist=", artist_name) else ""
    
        try {
            val result = searchPetitLyrics(title_param + artist_param)
            result.onSuccess { results ->
                if (results.isEmpty() && artist_name != null) {
                    return searchPetitLyrics(title_param)
                }
            }
            return result
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
    }
}

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
