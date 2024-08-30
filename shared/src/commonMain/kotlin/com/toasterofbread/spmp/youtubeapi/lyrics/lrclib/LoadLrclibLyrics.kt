package com.toasterofbread.spmp.youtubeapi.lyrics.lrclib

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.JsonHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

internal suspend fun loadLrclibLyrics(lyrics_id: String): Result<List<List<SongLyrics.Term>>> = runCatching {
	val client: HttpClient = JsonHttpClient
	val response: HttpResponse =
	    client.get("https://lrclib.net/api/get/$lyrics_id")
		
	if (response.status.value != 200) {
            throw RuntimeException("Fetching lyrics for id $lyrics_id failed: ${response.status} ${response.body() as String}")	}
	val candidate: Candidate = response.body()
	
	if (candidate.plainLyrics == null) {
	    throw RuntimeException("No lyrics found for id $lyrics_id")
	}
	
	if (candidate.syncedLyrics == null) {
	    return Result.success(
		    candidate.plainLyrics
			.lines()
			.map { listOf(SongLyrics.Term(listOf(SongLyrics.Term.Text(it)), -1)) }
	    )
	}

	val lyrics_data: String = candidate.syncedLyrics
	
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
	
	return@runCatching reverse_lines.asReversed().map { listOf(it) }
}


@Serializable
class Candidate(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)


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
