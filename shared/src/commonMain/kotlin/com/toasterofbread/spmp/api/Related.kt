package com.toasterofbread.spmp.api

import SpMp
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class RelatedGroup(val title: String, val items: List<MediaItem>?, val description: String?)

private class BrowseResponse(val contents: YoutubeiBrowseResponse.Content)

private suspend fun loadBrowseEndpoint(browse_id: String): Result<List<RelatedGroup>> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf("browseId" to browse_id)))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()

    try {
        val parsed: BrowseResponse = Api.klaxon.parse(stream)!!
        return@withContext Result.success(parsed.contents.sectionListRenderer.contents!!.map { group ->
            RelatedGroup(
                group.title?.text ?: getString("song_related_group_other"),
                group.getMediaItemsOrNull(hl),
                group.description
            )
        })
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream.close()
    }
}

suspend fun getSongRelated(song: Song, db: Database): Result<List<RelatedGroup>> {
    var already_loaded = false
    var related_browse_id = db.transactionWithResult {
        val related_browse_id = db.songQueries.relatedBrowseIdById(song.id).executeAsOne().related_browse_id
        if (related_browse_id != null) {
            return@transactionWithResult related_browse_id
        }

        val loaded = db.mediaItemQueries.loadedById(song.id).executeAsOne().loaded != null
        if (loaded) {
            already_loaded = true
        }

        return@transactionWithResult null
    }

    if (related_browse_id == null && !already_loaded) {
        val load_result = MediaItemLoader.loadSong(
            if (song is SongData) song else SongData(song.id),
            db
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
