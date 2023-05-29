package com.spectre7.spmp.api

import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.getStream
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.AccountPlaylist
import com.spectre7.spmp.ui.component.MediaItemLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private fun formatPlaylistId(playlist_id: String): String = playlist_id.removePrefix("VL")

suspend fun getAccountPlaylists(): Result<List<AccountPlaylist>> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody("""{"browseId": "FEmusic_liked_playlists"}"""))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val parsed: YoutubeiBrowseResponse = DataApi.klaxon.parse(stream)!!
    stream.close()

    val playlist_data = parsed
        .contents!!
        .singleColumnBrowseResultsRenderer
        .tabs
        .first()
        .tabRenderer
        .content!!
        .sectionListRenderer
        .contents!!
        .first()
        .gridRenderer!!
        .items

    val playlists: List<AccountPlaylist> = playlist_data.mapNotNull {
        // Skip 'New playlist' item
        if (it.musicTwoRowItemRenderer?.navigationEndpoint?.browseEndpoint == null) {
            return@mapNotNull null
        }

        val item = it.toMediaItem(hl)?.first
        return@mapNotNull if (item is AccountPlaylist) item else null
    }

    return@withContext Result.success(playlists)
}

private class PlaylistCreateResponse(val playlistId: String)

suspend fun createAccountPlaylist(title: String, description: String): Result<String> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/playlist/create")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody(
            mapOf("title" to title, "description" to description)
        ))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val response: PlaylistCreateResponse = DataApi.klaxon.parse(stream)!!
    stream.close()

    return@withContext Result.success(response.playlistId)
}

suspend fun deleteAccountPlaylist(playlist_id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/playlist/delete")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody(mapOf(
            "playlistId" to formatPlaylistId(playlist_id)
        )))
        .build()

    return@withContext DataApi.request(request).unit()
}

interface AccountPlaylistEditAction {
    fun getData(playlist: AccountPlaylist): Map<String, String>

    class Add(private val song_id: String): AccountPlaylistEditAction {
        override fun getData(playlist: AccountPlaylist): Map<String, String> = mapOf(
            "action" to "ACTION_ADD_VIDEO",
            "addedVideoId" to song_id,
            "dedupeOption" to "DEDUPE_OPTION_SKIP"
        )
    }

    class Remove(private val index: Int): AccountPlaylistEditAction {
        override fun getData(playlist: AccountPlaylist): Map<String, String> = mapOf(
            "action" to "ACTION_REMOVE_VIDEO",
            "removedVideoId" to playlist.items!![index].id,
            "setVideoId" to playlist.item_set_ids!![index]
        )
    }

    class Move(
        private val from: Int,
        private val to: Int
    ): AccountPlaylistEditAction {
        override fun getData(playlist: AccountPlaylist): Map<String, String> {
            val items = playlist.items!!
            check(from in items.indices)
            check(to in items.indices)

            val set_ids = playlist.item_set_ids!!

            val ret =  mutableMapOf(
                "action" to "ACTION_MOVE_VIDEO_BEFORE",
                "setVideoId" to set_ids[from]
            )
            if (to + 1 < items.size) {
                ret["movedSetVideoIdSuccessor"] = set_ids[to]
            }

            return ret
        }
    }
}

suspend fun editAccountPlaylist(playlist: AccountPlaylist, actions: List<AccountPlaylistEditAction>): Result<Unit> {
    if (actions.isEmpty()) {
        return Result.success(Unit)
    }

    return withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .ytUrl("/youtubei/v1/browse/edit_playlist")
            .addYtHeaders()
            .post(DataApi.getYoutubeiRequestBody(mapOf(
                "playlistId" to formatPlaylistId(playlist.id),
                "actions" to actions.map { it.getData(playlist) }
            )))
            .build()

        return@withContext DataApi.request(request).unit()
    }
}

suspend fun addSongsToAccountPlaylist(playlist_id: String, song_ids: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
    if (song_ids.isEmpty()) {
        return@withContext Result.success(Unit)
    }

    val actions: List<Map<String, String>> = song_ids.map { id ->
        mapOf(
            "action" to "ACTION_ADD_VIDEO",
            "addedVideoId" to id,
            "dedupeOption" to "DEDUPE_OPTION_SKIP"
        )
    }

    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse/edit_playlist")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody(mapOf(
            "playlistId" to formatPlaylistId(playlist_id),
            "actions" to actions
        )))
        .build()

    return@withContext DataApi.request(request).unit()
}
