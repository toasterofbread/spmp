package com.toasterofbread.spmp.youtubeapi.lyrics.kugou

import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.JsonHttpClient
import dev.toastbits.ytmkt.uistrings.localised.UILanguages
import dev.toastbits.ytmkt.uistrings.localised.matchesLanguage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.CharsetDecoder
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.charsets.forName
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val START_SKIP_LINES: Int = 0

suspend fun loadKugouLyrics(
    hash: String,
    lang: String
): Result<List<List<SongLyrics.Term>>> = runCatching {
    val client: HttpClient = JsonHttpClient
    val response: HttpResponse =
        client.get("https://krcs.kugou.com/search") {
            url {
                parameters.append("ver", "1")
                parameters.append("man", "yes")
                parameters.append("client", "mobi")
                parameters.append("hash", hash)
            }
        }

    val search_response: KugouHashSearchResponse = response.body()
    if (search_response.status != 200) {
        throw RuntimeException("Fetching lyrics for hash $hash failed: ${search_response.status} ${search_response.errmsg}")
    }

    val candidate: KugouHashSearchResponse.Candidate? = search_response.getBestCandidate()
    if (candidate == null) {
        throw RuntimeException("No candidates for hash $hash")
    }

    val lyrics_data: String = client.downloadSearchCandidate(candidate).getOrThrow()

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

    return@runCatching reverse_lines.asReversed().drop(START_SKIP_LINES).map { listOf(it) }
}

private fun formatLyricsLine(line: String, lang: String): String {
    if (!lang.matchesLanguage(UILanguages.ja_JP)) {
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

@OptIn(ExperimentalEncodingApi::class)
private suspend fun HttpClient.downloadSearchCandidate(
    candidate: KugouHashSearchResponse.Candidate
): Result<String> = runCatching {
    val response: HttpResponse =
        get("https://krcs.kugou.com/download") {
            url {
                parameters.append("ver", "1")
                parameters.append("man", "yes")
                parameters.append("client", "pc")
                parameters.append("fmt", "lrc")
                parameters.append("id", candidate.id)
                parameters.append("accesskey", candidate.accesskey)
            }
        }

    val download_response: KugouSearchCandidateDownloadResponse = response.body()
    if (download_response.status != 200) {
        throw RuntimeException(download_response.info)
    }

    val bytes: ByteArray = Base64.decode(download_response.content)
    val decoder: CharsetDecoder = Charsets.forName(download_response.charset).newDecoder()
    return@runCatching decoder.decode(Buffer().apply { write(bytes) })
}

@Serializable
private class KugouSearchCandidateDownloadResponse(
    val status: Int,
    val info: String,
    val charset: String,
    val content: String
)

@Serializable
internal class KugouHashSearchResponse(
    val status: Int,
    val errmsg: String,
    val candidates: List<Candidate>
) {
    @Serializable
    data class Candidate(
        val id: String,
        val accesskey: String
    )

    fun getBestCandidate(): Candidate? = candidates.firstOrNull()
}
