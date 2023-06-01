package com.spectre7.spmp.api

import com.spectre7.spmp.api.Api.Companion.addYtHeaders
import com.spectre7.spmp.api.Api.Companion.getStream
import com.spectre7.spmp.api.Api.Companion.ytUrl
import com.spectre7.spmp.model.mediaitem.AccountPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

suspend fun getAccountPlaylists(): Result<List<AccountPlaylist>> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody("""{"browseId": "FEmusic_liked_playlists"}"""))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val parsed: YoutubeiBrowseResponse = Api.klaxon.parse(stream)!!
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
        .post(
            Api.getYoutubeiRequestBody(
                mapOf("title" to title, "description" to description),
                Api.Companion.YoutubeiContextType.UI_LANGUAGE
            )
        )
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val response: PlaylistCreateResponse = Api.klaxon.parse(stream)!!
    stream.close()

    return@withContext Result.success(response.playlistId)
}

suspend fun deleteAccountPlaylist(playlist_id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/playlist/delete")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf(
            "playlistId" to AccountPlaylist.formatId(playlist_id)
        )))
        .build()

    return@withContext Api.request(request).unit()
}

interface AccountPlaylistEditAction {
    fun getData(playlist: AccountPlaylist, current_set_ids: MutableList<String>): Map<String, String>

    class Add(private val song_id: String): AccountPlaylistEditAction {
        override fun getData(playlist: AccountPlaylist, current_set_ids: MutableList<String>): Map<String, String> = mapOf(
            "action" to "ACTION_ADD_VIDEO",
            "addedVideoId" to song_id,
            "dedupeOption" to "DEDUPE_OPTION_SKIP"
        )
    }

    class Remove(private val index: Int): AccountPlaylistEditAction {
        override fun getData(playlist: AccountPlaylist, current_set_ids: MutableList<String>): Map<String, String> = mapOf(
            "action" to "ACTION_REMOVE_VIDEO",
            "removedVideoId" to playlist.items!![index].id,
            "setVideoId" to playlist.item_set_ids!![index]
        )
    }

    class Move(
        private val from: Int,
        private val to: Int
    ): AccountPlaylistEditAction {
        override fun getData(playlist: AccountPlaylist, current_set_ids: MutableList<String>): Map<String, String> {
            val set_ids = playlist.item_set_ids!!.toMutableList()
            check(set_ids.size == playlist.items!!.size)
            check(from != to)

            val ret =  mutableMapOf(
                "action" to "ACTION_MOVE_VIDEO_BEFORE",
                "setVideoId" to set_ids[from]
            )

            val to_index = if (to > from) to + 1 else to
            if (to_index in set_ids.indices) {
                ret["movedSetVideoIdSuccessor"] = set_ids[to_index]
            }

            set_ids.add(to, set_ids.removeAt(from))
            playlist.item_set_ids = set_ids

            return ret
        }
    }
}

suspend fun editAccountPlaylist(playlist: AccountPlaylist, actions: List<AccountPlaylistEditAction>): Result<Unit> {
    if (actions.isEmpty()) {
        return Result.success(Unit)
    }

    val current_set_ids: MutableList<String> = playlist.item_set_ids!!.toMutableList()

    return withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .ytUrl("/youtubei/v1/browse/edit_playlist")
            .addYtHeaders()
            .post(Api.getYoutubeiRequestBody(mapOf(
                "playlistId" to AccountPlaylist.formatId(playlist.id),
                "actions" to actions.map { it.getData(playlist, current_set_ids) }
            )))
            .build()

        return@withContext Api.request(request).unit()
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
        .post(Api.getYoutubeiRequestBody(mapOf(
            "playlistId" to AccountPlaylist.formatId(playlist_id),
            "actions" to actions
        )))
        .build()

    val result = Api.request(request)
    result.getOrNull()?.close()

    return@withContext result.unit()
}
