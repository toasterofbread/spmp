package com.toasterofbread.spmp.api.lyrics

import com.toasterofbread.spmp.api.lyrics.petit.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.api.*
import com.toasterofbread.spmp.model.SongLyrics
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import androidx.compose.ui.graphics.Color
import java.util.*

private const val DATA_START = "<lyricsData>"
private const val DATA_END = "</lyricsData>"

internal class PetitLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    override fun getReadable(): String = getString("lyrics_source_petit")
    override fun getColour(): Color = Color(0xFFBD0A0F)
    
    override suspend fun getLyrics(lyrics_id: String): Result<SongLyrics> {
        var fail_result: Result<SongLyrics>? = null
        for (sync_type in SongLyrics.SyncType.byPriority()) {
            val result = getLyricsData(lyrics_id.toInt(), sync_type)
            if (!result.isSuccess) {
                if (fail_result == null) {
                    fail_result = result.cast()
                }
                continue
            }

            if (result.data.startsWith("<wsy>")) {
                val parse_result = parseTimedLyrics(result.data)
                val lyrics = parse_result.getOrNull() ?: return parse_result.cast()

                return Result.success(SongLyrics(
                    LyricsReference(lyrics_id, source_idx),
                    sync_type,
                    lyrics
                ))
            }
            else {
                return Result.success(SongLyrics(
                    LyricsReference(lyrics_id, source_idx),
                    SongLyrics.SyncType.NONE,
                    parseStaticLyrics(result.data)
                ))
            }
        }

        return fail_result ?: Result.failure(IllegalStateException())
    }

    private suspend fun getLyricsData(lyrics_id: Int, sync_type: SongLyrics.SyncType): Result<String> = withContext(Dispatchers.IO) {
        val body = "key_lyricsId=$lyrics_id&lyricsType=${sync_type.ordinal + 1}&terminalType=10&clientAppId=on354007".toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://p1.petitlyrics.com/api/GetPetitLyricsData.php")
            .post(body)
            .build()
    
        val result = Api.request(request)
        if (result.isFailure) {
            return@withContext result.cast()
        }
    
        val xml = result.getOrThrowHere().body!!.string()
        val start = xml.indexOf(DATA_START)
        val end = xml.indexOf(DATA_END, start + DATA_START.length)
    
        val decoded = Base64.getDecoder().decode(xml.substring(start + DATA_START.length, end))
        return@withContext Result.success(String(decoded))
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
