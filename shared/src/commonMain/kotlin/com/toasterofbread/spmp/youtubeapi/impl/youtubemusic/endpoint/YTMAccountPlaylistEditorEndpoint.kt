package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.youtubeapi.endpoint.AccountPlaylistEditorEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.utils.common.lazyAssert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMAccountPlaylistEditorEndpoint(override val auth: YoutubeMusicAuthInfo): AccountPlaylistEditorEndpoint() {
    override fun getEditor(playlist: RemotePlaylist): PlaylistEditor {
        return AccountPlaylistEditor(playlist, auth, this)
    }
}

private class AccountPlaylistEditor(playlist: RemotePlaylist, val auth: YoutubeMusicAuthInfo, val endpoint: AccountPlaylistEditorEndpoint): PlaylistEditor(playlist, auth.api.context) {
    override fun canRemoveItems(): Boolean {
        return playlist is RemotePlaylistData && playlist.item_set_ids != null
    }
    override fun canMoveItems(): Boolean {
        return playlist is RemotePlaylistData && playlist.item_set_ids != null
    }

    override suspend fun performDeletion(): Result<Unit> {
        return auth.DeleteAccountPlaylist.deleteAccountPlaylist(playlist.id)
    }

    override suspend fun performAndCommitActions(actions: List<Action>): Result<Unit> {
        lazyAssert { playlist.isPlaylistEditable(context) }

        if (actions.isEmpty()) {
            return Result.success(Unit)
        }

        return withContext(Dispatchers.IO) {
            val actions_request_data: List<Map<String, String>> = actions.mapNotNull { performActionOrGetRequestData(it) }
            if (actions_request_data.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            with(endpoint) {
                val request = Request.Builder()
                    .endpointUrl("/youtubei/v1/browse/edit_playlist")
                    .addAuthApiHeaders()
                    .postWithBody(
                        mapOf(
                            "playlistId" to RemotePlaylist.formatYoutubeId(playlist.id),
                            "actions" to actions_request_data
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

    private fun performActionOrGetRequestData(action: Action): Map<String, String>? {
        when(action) {
            is Action.SetTitle -> {
                playlist.Title.set(action.title, context.database)
                return mapOf(
                    "action" to "ACTION_SET_PLAYLIST_NAME",
                    "playlistName" to action.title
                )
            }
            is Action.SetImage -> {
                playlist.CustomImageUrl.set(action.image_url, context.database)
                return null
            }
            is Action.SetImageWidth -> {
                playlist.ImageWidth.set(action.image_width, context.database)
                return null
            }
            is Action.Add -> {
                playlist.Items.addItem(SongRef(action.song_id), action.index, context.database)
                return mapOf(
                    "action" to "ACTION_ADD_VIDEO",
                    "addedVideoId" to action.song_id,
                    "dedupeOption" to "DEDUPE_OPTION_SKIP"
                )
            }
            is Action.Move -> {
                check(playlist is RemotePlaylistData && playlist.item_set_ids != null)

                val set_ids = playlist.item_set_ids!!.toMutableList()
                check(set_ids.size == playlist.items!!.size)
                check(action.from != action.to)

                playlist.Items.moveItem(action.from, action.to, context.database)

                val data = mutableMapOf(
                    "action" to "ACTION_MOVE_VIDEO_BEFORE",
                    "setVideoId" to set_ids[action.from]
                )

                val to_index = if (action.to > action.from) action.to + 1 else action.to
                if (to_index in set_ids.indices) {
                    data["movedSetVideoIdSuccessor"] = set_ids[to_index]
                }

                set_ids.add(action.to, set_ids.removeAt(action.from))
                playlist.item_set_ids = set_ids

                return data
            }
            is Action.Remove -> {
                check(playlist is RemotePlaylistData && playlist.item_set_ids != null)

                playlist.Items.removeItem(action.index, context.database)

                return mapOf(
                    "action" to "ACTION_REMOVE_VIDEO",
                    "removedVideoId" to playlist.items!![action.index].id,
                    "setVideoId" to playlist.item_set_ids!![action.index]
                )
            }
        }
    }
}
