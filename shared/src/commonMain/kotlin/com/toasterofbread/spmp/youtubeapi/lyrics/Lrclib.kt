package com.toasterofbread.spmp.youtubeapi.lyrics 

import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import com.toasterofbread.spmp.db.Database
import kotlinx.coroutines.withContext
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.JsonHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.youtubeapi.lyrics.lrclib.loadLrclibLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.lrclib.searchLrclibLyrics
import com.toasterofbread.spmp.youtubeapi.lyrics.lrclib.getLrclibLyrics

internal class LrclibLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    override fun getReadable(): String = "lrclib"
    override fun getColour(): Color = Color(0x0C0E41)
    override fun getUrlOfId(id: String): String? = null

    override fun supportsLyricsBySong(): Boolean = false
    override fun supportsLyricsBySearching(): Boolean = true
    
    override suspend fun getReferenceBySong(song: Song, context: AppContext): Result<LyricsReference?> {
	val db: Database = context.database
	val (track_name, artist_name, album_name, duration) = db.transactionWithResult {
	    listOf(
		song.getActiveTitle(db)?.toString(),
		song.Artists.get(db)?.firstOrNull()?.getActiveTitle(db)?.toString(),
		song.Album.get(db)?.getActiveTitle(db)?.toString(),
		song.Duration.get(db)?.toString(),
	    )
	}
	val lyrics_id: String = getLrclibLyrics(track_name, artist_name, album_name, duration).getOrThrow()
	return Result.success(
	    LyricsReference(source_index, lyrics_id)
	)
    }
    
					     
    override suspend fun getLyrics(
        lyrics_id: String,
        context: AppContext
    ): Result<SongLyrics> = runCatching {
	val lines: List<List<SongLyrics.Term>> = loadLrclibLyrics(lyrics_id).getOrThrow()
	
	return@runCatching SongLyrics(
	    LyricsReference(source_index, lyrics_id),
	    SongLyrics.SyncType.LINE_SYNC,
	    lines
	)
    }

    override suspend fun searchForLyrics(title: String, artist_name: String?): Result<List<SearchResult>> = runCatching {
        return searchLrclibLyrics(title, artist_name)
    }
}

