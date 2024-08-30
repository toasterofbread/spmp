package com.toasterofbread.spmp.youtubeapi.lyrics.lrclib

import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import com.toasterofbread.spmp.db.Database
import kotlinx.coroutines.withContext
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.JsonHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

internal suspend fun searchLrclibLyrics(
    title: String,
    artist: String?
): Result<List<LyricsSource.SearchResult>> = runCatching {
	val client: HttpClient = JsonHttpClient
	val response: HttpResponse =
	    client.get("https://lrclib.net/api/search") {
		url {
		    parameters.append("track_name", title)
		    if (artist != null) {
			parameters.append("artist_name", artist)
		    }
		}
	    }
	if (response.status.value != 200) {
            throw RuntimeException("Fetching lyrics for song '$title' failed: ${response.status} ${response.body() as String}")
	}
	val candidates: List<Candidate> = response.body()

        return@runCatching candidates.map { info ->
	    var sync_type = SongLyrics.SyncType.LINE_SYNC
	    if (info.syncedLyrics == null) {
		sync_type = SongLyrics.SyncType.NONE
	    }
            LyricsSource.SearchResult(
		info.id.toString(),
		info.trackName,
		sync_type,
		info.artistName,
		info.albumName
            )
	}
}

internal suspend fun getLrclibLyrics(title: String?, artist: String?, album: String?, duration: String?): Result<String> {
    throw NotImplementedError()
    val client: HttpClient = JsonHttpClient
    val response: HttpResponse =
	client.get("https://lrclib.net/api/get") {
	    url {
		parameters.append("track_name", title ?: "")
		parameters.append("artist_name", artist ?: "")
		parameters.append("album_name", album ?: "")
		parameters.append("duration", duration ?: "")
	    }
	}
	
    val candidate: Candidate = response.body()
    if (candidate.instrumental) {
	throw RuntimeException("The song is instrumental: ${candidate.id}")
    }
	
    return Result.success(
	candidate.id.toString()
    )
}
