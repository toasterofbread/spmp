package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor
import com.toasterofbread.spmp.youtubeapi.endpoint.AccountPlaylistEditorEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.utils.lazyAssert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMAccountPlaylistEditorEndpoint(override val auth: YoutubeMusicAuthInfo): AccountPlaylistEditorEndpoint() {
    override fun getEditor(playlist: RemotePlaylist): PlaylistEditor {
        return AccountPlaylistEditor(playlist, auth, this)
    }
}

private class AccountPlaylistEditor(playlist: RemotePlaylist, val auth: YoutubeMusicAuthInfo, val endpoint: AccountPlaylistEditorEndpoint): PlaylistEditor(playlist, auth.api.context) {
    private val initial_playlist_data = playlist.getEmptyData().also { playlist.populateData(it, context.database) }

    override suspend fun deletePlaylist(): Result<Unit> {
        super.deletePlaylist().onFailure {
            return Result.failure(it)
        }
        return auth.DeleteAccountPlaylist.deleteAccountPlaylist(playlist.id)
    }

    override suspend fun performAndCommitActions(actions: List<Action>): Result<Unit> {
        lazyAssert { playlist.isPlaylistEditable() }

        if (actions.isEmpty()) {
            return Result.success(Unit)
        }

        return withContext(Dispatchers.IO) {
            with(endpoint) {
                val request = Request.Builder()
                    .endpointUrl("/youtubei/v1/browse/edit_playlist")
                    .addAuthApiHeaders()
                    .postWithBody(
                        mapOf(
                            "playlistId" to RemotePlaylist.formatYoutubeId(playlist.id),
                            "actions" to actions.map { it.getRequestData(initial_playlist_data) }
                        )
                    )
                    .build()

                val result = api.performRequest(request)
                return@withContext result.fold(
                    {
                        it.close()
                        Result.success(Unit)
                    },
                    { Result.failure(it) }
                )
            }
        }
    }

    private fun Action.getRequestData(playlist: RemotePlaylistData): Map<String, String> =
        when(this) {
            is AddAction ->
                mapOf(
                    "action" to "ACTION_ADD_VIDEO",
                    "addedVideoId" to song_id,
                    "dedupeOption" to "DEDUPE_OPTION_SKIP"
                )
            is MoveAction -> {
                val set_ids = playlist.item_set_ids!!.toMutableList()
                check(set_ids.size == playlist.items!!.size)
                check(from != to)

                val data = mutableMapOf(
                    "action" to "ACTION_MOVE_VIDEO_BEFORE",
                    "setVideoId" to set_ids[from]
                )

                val to_index = if (to > from) to + 1 else to
                if (to_index in set_ids.indices) {
                    data["movedSetVideoIdSuccessor"] = set_ids[to_index]
                }

                set_ids.add(to, set_ids.removeAt(from))
                playlist.item_set_ids = set_ids

                data
            }
            is RemoveAction ->
                mapOf(
                    "action" to "ACTION_REMOVE_VIDEO",
                    "removedVideoId" to playlist.items!![index].id,
                    "setVideoId" to playlist.item_set_ids!![index]
                )
        }
}
