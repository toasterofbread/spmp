package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.SongRelatedContentEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.RelatedGroup
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMSongRelatedContentEndpoint(override val api: YoutubeMusicApi): SongRelatedContentEndpoint() {
    override suspend fun getSongRelated(song: Song): Result<List<RelatedGroup>> = withContext(Dispatchers.IO) {
        var already_loaded = false
        var related_browse_id = api.database.transactionWithResult {
            val related_browse_id = api.database.songQueries.relatedBrowseIdById(song.id).executeAsOne().related_browse_id
            if (related_browse_id != null) {
                return@transactionWithResult related_browse_id
            }

            val loaded = api.database.mediaItemQueries.loadedById(song.id).executeAsOne().loaded != null
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
                { return@withContext Result.failure(it) }
            )
        }

        if (related_browse_id == null) {
            return@withContext Result.failure(RuntimeException("Song has no related_browse_id"))
        }

        val hl = SpMp.data_language
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(mapOf("browseId" to related_browse_id))
            .build()

        val result = api.performRequest(request)
        val parsed: BrowseResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        val groups = api.database.transactionWithResult {
            parsed.contents.sectionListRenderer?.contents?.map { group ->
                val items = group.getMediaItemsOrNull(hl)
                for (item in items ?: emptyList()) {
                    item.saveToDatabase(api.database)
                }

                RelatedGroup(
                    title = group.title?.text ?: getString("song_related_group_other"),
                    items = items,
                    description = group.description
                )
            }
        }

        if (groups == null) {
            return@withContext Result.failure(NullPointerException())
        }

        return@withContext Result.success(groups)
    }

    private class BrowseResponse(val contents: YoutubeiBrowseResponse.Content)
}
