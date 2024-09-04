package com.toasterofbread.spmp.youtubeapi.lyrics.lrclib

import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.JsonHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import kotlin.time.Duration

internal suspend fun searchLrclibLyrics(
    title: String,
    artist: String?,
	album: String?,
	duration: Duration?
): Result<List<LyricsSource.SearchResult>> = runCatching {
	val client: HttpClient = JsonHttpClient

	val response: HttpResponse =
	    client.get("https://lrclib.net/api/search") {
			url {
				parameters.append("track_name", title)
				if (artist != null) {
					parameters.append("artist_name", artist)
				}
				if (album != null) {
					parameters.append("album_name", album)
				}
				if (duration != null) {
					parameters.append("duration", duration.inWholeSeconds.toString())
				}
			}
	    }

	if (!response.status.isSuccess()) {
		throw RuntimeException("Fetching lyrics at ${response.request.url} failed (${response.status}): ${response.bodyAsText()}")
	}

	val candidates: List<Candidate> = response.body()
	return@runCatching candidates.map { info ->
	    val sync_type: SongLyrics.SyncType =
			if (info.syncedLyrics != null) SongLyrics.SyncType.LINE_SYNC
			else SongLyrics.SyncType.NONE

		LyricsSource.SearchResult(
			info.id.toString(),
			info.trackName,
			sync_type,
			info.artistName,
			info.albumName
		)
	}
}
