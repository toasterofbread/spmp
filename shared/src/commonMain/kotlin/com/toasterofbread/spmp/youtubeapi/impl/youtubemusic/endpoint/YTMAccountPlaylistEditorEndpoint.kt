package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor
import com.toasterofbread.spmp.youtubeapi.endpoint.AccountPlaylistEditorEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.utils.lazyAssert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMAccountPlaylistEditorEndpoint(override val auth: YoutubeMusicAuthInfo): AccountPlaylistEditorEndpoint() {
    override fun getEditor(playlist: Playlist): PlaylistEditor {
        require(!playlist.isLocalPlaylist())
        return AccountPlaylistEditor(playlist, auth, this)
    }
}

private class AccountPlaylistEditor(playlist: Playlist, val auth: YoutubeMusicAuthInfo, val endpoint: AccountPlaylistEditorEndpoint): PlaylistEditor(playlist, auth.api.context) {
    private val initial_playlist_data = playlist.getEmptyData().also { playlist.populateData(it, context.database) }
    private val pending_edit_actions: MutableList<AccountPlaylistEditAction> = mutableListOf()

    override fun addItem(item: Song, index: Int?) {
        super.addItem(item, index)
        pending_edit_actions.add(AccountPlaylistEditAction.Add(item.id))
    }

    override fun removeItem(index: Int) {
        super.removeItem(index)
        pending_edit_actions.add(AccountPlaylistEditAction.Remove(index))
    }

    override fun moveItem(from: Int, to: Int) {
        super.moveItem(from, to)
        pending_edit_actions.add(AccountPlaylistEditAction.Move(from, to))
    }

    override suspend fun deletePlaylist(): Result<Unit> {
        super.deletePlaylist().onFailure {
            return Result.failure(it)
        }
        pending_edit_actions.clear()
        return auth.DeleteAccountPlaylist.deleteAccountPlaylist(playlist.id)
    }

    override suspend fun applyItemChanges(): Result<Unit> {
        super.applyItemChanges().onFailure {
            return Result.failure(it)
        }

        val actions: List<AccountPlaylistEditAction>
        synchronized(pending_edit_actions) {
            actions = pending_edit_actions.toList()
            pending_edit_actions.clear()
        }


        return editAccountPlaylist(initial_playlist_data, actions)
    }

    interface AccountPlaylistEditAction {
        fun getData(playlist: PlaylistData, current_set_ids: MutableList<String>): Map<String, String>

        class Add(private val song_id: String): AccountPlaylistEditAction {
            override fun getData(playlist: PlaylistData, current_set_ids: MutableList<String>): Map<String, String> = mapOf(
                "action" to "ACTION_ADD_VIDEO",
                "addedVideoId" to song_id,
                "dedupeOption" to "DEDUPE_OPTION_SKIP"
            )
        }

        class Remove(private val index: Int): AccountPlaylistEditAction {
            override fun getData(playlist: PlaylistData, current_set_ids: MutableList<String>): Map<String, String> = mapOf(
                "action" to "ACTION_REMOVE_VIDEO",
                "removedVideoId" to playlist.items!![index].id,
                "setVideoId" to playlist.item_set_ids!![index]
            )
        }

        class Move(
            private val from: Int,
            private val to: Int
        ): AccountPlaylistEditAction {
            override fun getData(playlist: PlaylistData, current_set_ids: MutableList<String>): Map<String, String> {
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

    suspend fun editAccountPlaylist(playlist: PlaylistData, actions: List<AccountPlaylistEditAction>): Result<Unit> {
        lazyAssert { !playlist.isLocalPlaylist() }
        lazyAssert { playlist.isPlaylistEditable() }

        if (actions.isEmpty()) {
            return Result.success(Unit)
        }

        val current_set_ids: MutableList<String> = playlist.item_set_ids!!.toMutableList()

        return withContext(Dispatchers.IO) {
            with(endpoint) {
                val request = Request.Builder()
                    .endpointUrl("/youtubei/v1/browse/edit_playlist")
                    .addAuthApiHeaders()
                    .postWithBody(
                        mapOf(
                            "playlistId" to Playlist.formatYoutubeId(playlist.id),
                            "actions" to actions.map { it.getData(playlist, current_set_ids) }
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

}
