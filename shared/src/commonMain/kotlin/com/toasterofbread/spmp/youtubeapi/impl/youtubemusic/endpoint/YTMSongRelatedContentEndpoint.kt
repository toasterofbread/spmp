package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.SongRelatedContentEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.RelatedGroup
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMSongRelatedContentEndpoint(override val api: YoutubeMusicApi): SongRelatedContentEndpoint() {
    override suspend fun getSongRelated(song: Song): Result<List<RelatedGroup>> {
        var already_loaded = false
        var related_browse_id = api.db.transactionWithResult {
            val related_browse_id = api.db.songQueries.relatedBrowseIdById(song.id).executeAsOne().related_browse_id
            if (related_browse_id != null) {
                return@transactionWithResult related_browse_id
            }

            val loaded = api.db.mediaItemQueries.loadedById(song.id).executeAsOne().loaded != null
            if (loaded) {
                already_loaded = true
            }

            return@transactionWithResult null
        }

        if (related_browse_id == null && !already_loaded) {
            val load_result = MediaItemLoader.loadSong(
                if (song is SongData) song else SongData(song.id),
                api.context
            )

            related_browse_id = load_result.fold(
                { it.related_browse_id },
                { return Result.failure(it) }
            )
        }

        if (related_browse_id == null) {
            return Result.failure(RuntimeException("Song has no related_browse_id"))
        }

        return loadBrowseEndpoint(related_browse_id)
    }

    private class BrowseResponse(val contents: YoutubeiBrowseResponse.Content)

    private suspend fun loadBrowseEndpoint(browse_id: String): Result<List<RelatedGroup>> = withContext(Dispatchers.IO) {
        val hl = SpMp.data_language
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(mapOf("browseId" to browse_id))
            .build()

        val result = api.performRequest(request)
        val parsed: BrowseResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        return@withContext Result.success(
            parsed.contents.sectionListRenderer.contents!!.map { group ->
                RelatedGroup(
                    title = group.title?.text ?: getString("song_related_group_other"),
                    items = group.getMediaItemsOrNull(hl),
                    description = group.description
                )
            }
        )
    }
}
