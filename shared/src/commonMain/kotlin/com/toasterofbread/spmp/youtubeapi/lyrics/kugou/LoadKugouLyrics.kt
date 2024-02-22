package com.toasterofbread.spmp.youtubeapi.lyrics.kugou

import com.google.gson.Gson
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsFuriganaTokeniser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.nio.charset.Charset
import java.util.Base64
import com.toasterofbread.spmp.resources.uilocalisation.localised.matchesLanguage
import com.toasterofbread.spmp.resources.uilocalisation.localised.UILanguages

private const val START_SKIP_LINES: Int = 0

suspend fun loadKugouLyrics(
    hash: String,
    tokeniser: LyricsFuriganaTokeniser,
    lang: String
): Result<List<List<SongLyrics.Term>>> =
    withContext(Dispatchers.IO) { kotlin.runCatching {
        val request: Request = Request.Builder()
            .url(
                "https://krcs.kugou.com/search"
                    .toHttpUrl().newBuilder()
                    .addQueryParameter("ver", "1")
                    .addQueryParameter("man", "yes")
                    .addQueryParameter("client", "mobi")
                    .addQueryParameter("hash", hash)
                    .build()
            )
            .build()

        val response: Response = OkHttpClient().executeResult(request).getOrThrow()
        val search_response: KugouHashSearchResponse = response.body!!.charStream().use { stream ->
            Gson().fromJson(stream)
        }

        if (search_response.status != 200) {
            return@withContext Result.failure(IOException("Fetching lyrics for hash $hash failed: ${search_response.status} ${search_response.errmsg}"))
        }

        val candidate: KugouHashSearchResponse.Candidate? = search_response.getBestCandidate()
        if (candidate == null) {
            return@withContext Result.failure(RuntimeException("No candidates for hash $hash"))
        }

        val lyrics_data: String = downloadSearchCandidate(candidate).getOrThrow()

        val reverse_lines: MutableList<SongLyrics.Term> = mutableListOf()
        var previous_time: Long? = null

        for (line in lyrics_data.lines().asReversed()) {
            if (line.length < 10 || line[0] != '[' || !line[1].isDigit()) {
                continue
            }

            val split: List<String> = line.split(']', limit = 2)
            val time: Long = parseTimeString(split[0].substring(1))

            reverse_lines.add(
                SongLyrics.Term(
                    listOf(SongLyrics.Term.Text(formatLyricsLine(split[1], lang))),
                    -1,
                    start = time,
                    end = previous_time ?: Long.MAX_VALUE
                )
            )

            previous_time = time
        }

        for (term in reverse_lines.withIndex()) {
            term.value.line_index = reverse_lines.size - term.index - 1
            term.value.line_range = term.value.start!! .. term.value.end!!
        }

        return@runCatching reverse_lines.asReversed().mapIndexedNotNull { index, line ->
            if (index < START_SKIP_LINES) null
            else tokeniser.mergeAndFuriganiseTerms(listOf(line))
        }
    }}

private fun formatLyricsLine(line: String, lang: String): String {
    if (!lang.matchesLanguage(UILanguages.ja)) {
        return line
    }

    val formatted: StringBuilder = StringBuilder()
    for ((i, c) in line.withIndex()) {
        formatted.append(when (c) {
            '仆' -> '僕'
            '飞' -> '飛'
            '词' -> '詞'
            '头' -> '頭'
            '顷' -> '頃'
            '颜' -> '顔'
            '贪' -> '貪'
            '马' -> '馬'
            '赞' -> '賛'
            '杀' -> '殺'
            '别' -> '別'
            '颔' -> '頷'
            '瞒' -> '瞞'
            '爱' -> '愛'
            '叶' -> if (line.getOrNull(i - 1) == '言') '葉' else c
            '长' -> '長'
            '语' -> '語'
            '过' -> '過'
            '梦' -> '夢'
            '优' -> '優'
            '伪' -> '偽'
            '风' -> '風'
            else -> c
        })
    }
    return formatted.toString()
}

private fun parseTimeString(string: String): Long {
    var time: Long = 0L

    val split = string.split(':')
    for (part in split.withIndex()) {
        when (split.size - part.index - 1) {
            // Seconds
            0 -> time += (part.value.toFloat() * 1000L).toLong()
            // Minutes
            1 -> time += part.value.toLong() * 60000L
            // Hours
            2 -> time += part.value.toLong() * 3600000L

            else -> throw NotImplementedError("Time stage not implemented: ${split.size - part.index - 1}")
        }
    }

    return time
}

private suspend fun downloadSearchCandidate(candidate: KugouHashSearchResponse.Candidate): Result<String> =
    withContext(Dispatchers.IO) { runCatching {
        val request = Request.Builder()
            .url(
                "https://krcs.kugou.com/download"
                    .toHttpUrl().newBuilder()
                    .addQueryParameter("ver", "1")
                    .addQueryParameter("man", "yes")
                    .addQueryParameter("client", "pc")
                    .addQueryParameter("fmt", "lrc")
                    .addQueryParameter("id", candidate.id)
                    .addQueryParameter("accesskey", candidate.accesskey)
                    .build()
            )
            .build()

        val result = OkHttpClient().executeResult(request)
        val response = result.getOrThrow()

        val download_response: KugouSearchCandidateDownloadResponse =
            response.body!!.charStream().use { stream ->
                Gson().fromJson(stream)
            }

        if (download_response.status != 200) {
            return@withContext Result.failure(RuntimeException(download_response.info))
        }

        val bytes = Base64.getDecoder().decode(download_response.content)
        return@runCatching String(bytes, Charset.forName(download_response.charset))
    } }

private class KugouSearchCandidateDownloadResponse(
    val status: Int,
    val info: String,
    val charset: String,
    val content: String
)

private class KugouHashSearchResponse(
    val status: Int,
    val errmsg: String,
    val candidates: List<Candidate>
) {
    class Candidate(
        val id: String,
        val accesskey: String
    )
    fun getBestCandidate(): Candidate? = candidates.firstOrNull()
}
