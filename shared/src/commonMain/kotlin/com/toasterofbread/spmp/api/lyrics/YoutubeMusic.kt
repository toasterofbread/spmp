package com.toasterofbread.spmp.api.lyrics

import androidx.compose.ui.graphics.Color
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.api.parseJsonResponse
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private class LyricsBrowseResponse(val contents: YoutubeiBrowseResponse.Content)

internal class YoutubeMusicLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    override fun getReadable(): String = getString("lyrics_source_ytm")
    override fun getColour(): Color = Color(0xFE0000)

    override fun supportsLyricsBySong(): Boolean = true
    override fun supportsLyricsBySearching(): Boolean = false
    override suspend fun searchForLyrics(title: String, artist_name: String?): Result<List<SearchResult>> { throw NotImplementedError() }

    override suspend fun getReferenceBySong(song: Song, db: Database): Result<LyricsReference?> = withContext(Dispatchers.IO) {
        val browse_id = song.LyricsBrowseId.get(db)
        if (browse_id != null) {
            return@withContext Result.success(referenceOfSource(browse_id))
        }

        if (song.Loaded.get(db)) {
            return@withContext Result.success(null)
        }

        val loaded_item = MediaItemLoader.loadSong(song.getEmptyData(), db)
            .fold(
                { it },
                { return@withContext Result.failure(it) }
            )

        return@withContext Result.success(
            loaded_item.lyrics_browse_id?.let { browse_id ->
                referenceOfSource(browse_id)
            }
        )
    }

    override suspend fun getLyrics(lyrics_id: String): Result<SongLyrics> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .ytUrl("/youtubei/v1/browse")
            .addYtHeaders()
            .post(Api.getYoutubeiRequestBody(
                mapOf(
                    "browseId" to lyrics_id
                )
            ))
            .build()

        val parsed_response: LyricsBrowseResponse = Api.request(request).parseJsonResponse {
            return@withContext Result.failure(it)
        }

        val lyrics_text: String? =
            parsed_response.contents.sectionListRenderer.contents?.firstOrNull()?.musicDescriptionShelfRenderer?.description?.firstTextOrNull()

        if (lyrics_text == null) {
            return@withContext Result.failure(RuntimeException("Browse response for ID $lyrics_id contained no lyrics"))
        }

        return@withContext Result.success(
            SongLyrics(
                LyricsReference(source_index, lyrics_id),
                SongLyrics.SyncType.NONE,
                parseStaticLyrics(lyrics_text)
            )
        )
    }
}
