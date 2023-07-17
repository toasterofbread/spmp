package com.toasterofbread.spmp.api.lyrics.kugou

import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.lyrics.createTokeniser
import com.toasterofbread.spmp.api.lyrics.mergeAndFuriganiseTerms
import com.toasterofbread.spmp.model.SongLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.nio.charset.Charset
import java.util.Base64

private const val START_SKIP_LINES: Int = 4

suspend fun loadKugouLyrics(hash: String): Result<List<List<SongLyrics.Term>>> =
    withContext(Dispatchers.IO) { kotlin.runCatching {
        val request = Request.Builder()
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

        val response = Api.request(request, is_gzip = false).getOrThrow()
        val search_response: KugouHashSearchResponse = response.body!!.charStream().use { stream ->
            Klaxon().parse(stream)!!
        }

        if (search_response.status != 200) {
            return@withContext Result.failure(RuntimeException(search_response.errmsg))
        }

        val lyrics_data = downloadSearchCandidate(search_response.getBestCandidate()).getOrThrow()

        val reverse_lines: MutableList<SongLyrics.Term> = mutableListOf()
        var previous_time: Long? = null

        for (line in lyrics_data.lines().asReversed()) {
            if (line.length < 10 || line[0] != '[' || !line[1].isDigit()) {
                continue
            }

            val split = line.split(']', limit = 2)
            val time = parseTimeString(split[0].substring(1))

            reverse_lines.add(
                SongLyrics.Term(
                    listOf(SongLyrics.Term.Text(split[1])),
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

        val tokeniser = createTokeniser()
        return@runCatching reverse_lines.asReversed().mapIndexedNotNull { index, line ->
            if (index < START_SKIP_LINES) null
            else mergeAndFuriganiseTerms(tokeniser, listOf(line))
        }
    }}

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

        val result = Api.request(request)
        val response = result.getOrThrow()

        val download_response: KugouSearchCandidateDownloadResponse =
            response.body!!.charStream().use { stream ->
                Klaxon().parse(stream)!!
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
    fun getBestCandidate(): Candidate = candidates.first()
}
