package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.SongRef
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformFile
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.utils.addUnique
import com.toasterofbread.utils.lazyAssert

abstract class PlaylistEditor(open val playlist: Playlist, val context: PlatformContext) {
    protected sealed class Action
    protected data class AddAction(val song_id: String, val index: Int?): Action()
    protected data class RemoveAction(val index: Int): Action()
    protected data class MoveAction(val from: Int, val to: Int): Action()

    private var deleted: Boolean = false
    private val pending_actions: MutableList<Action> = mutableListOf()

    fun addItem(item: Song, index: Int? = null) {
        pending_actions.add(AddAction(item.id, index))
    }
    fun removeItem(index: Int) {
        pending_actions.add(RemoveAction(index))
    }
    fun moveItem(from: Int, to: Int) {
        pending_actions.add(MoveAction(from, to))
    }

    protected abstract suspend fun performAndCommitActions(actions: List<Action>): Result<Unit>

    suspend fun applyItemChanges(): Result<Unit> {
        return performAndCommitActions(pending_actions)
    }

    open suspend fun deletePlaylist(): Result<Unit> {
        check(!deleted)

        deleted = true
        PlaylistHolder.onPlaylistDeleted(playlist)
        pending_actions.clear()
        return Result.success(Unit)
    }

    companion object {
        private val editable_playlists: MutableList<String> = mutableStateListOf()

        fun setPlaylistEditable(playlist: RemotePlaylist, editable: Boolean) {
            if (editable) {
                editable_playlists.addUnique(playlist.id)
            }
            else {
                editable_playlists.remove(playlist.id)
            }
        }

        fun LocalPlaylist.getLocalPlaylistEditor(context: PlatformContext): PlaylistEditor {
            return LocalPlaylistEditor(this, context)
        }

        fun Playlist.getEditorOrNull(context: PlatformContext): PlaylistEditor? {
            if (this is LocalPlaylist) {
                return LocalPlaylistEditor(this, context)
            }
            else if (editable_playlists.contains(id)) {
                check(this is RemotePlaylist)
                val auth = context.ytapi.user_auth_state
                auth?.AccountPlaylistEditor?.getEditor(this)
            }
            return null
        }

        @Composable
        fun Playlist.rememberEditorOrNull(context: PlatformContext): PlaylistEditor? {
            if (this is LocalPlaylist) {
                return remember { LocalPlaylistEditor(this, context) }
            }
            else if (editable_playlists.contains(id)) {
                check(this is RemotePlaylist)
                val auth = context.ytapi.user_auth_state
                return remember(auth) {
                    if (auth != null) auth.AccountPlaylistEditor.getEditor(this)
                    else null
                }
            }
            return null
        }

        fun Playlist.isPlaylistEditable(): Boolean =
            (this is LocalPlaylist) || editable_playlists.contains(id)
    }
}

class LocalPlaylistEditor(override val playlist: LocalPlaylist, context: PlatformContext): PlaylistEditor(playlist, context) {
    suspend fun convertToAccountPlaylist(ytm_auth: YoutubeApi.UserAuthState): Result<RemotePlaylistData> {
        val create_endpoint = ytm_auth.CreateAccountPlaylist
        val add_endpoint = ytm_auth.AccountPlaylistAddSongs

        if (!create_endpoint.isImplemented() || !add_endpoint.isImplemented()) {
            return Result.failure(NotImplementedError())
        }

        val db = context.database

        val playlist_data = playlist.getEmptyData().also {
            playlist.populateData(it, db)
        }

        val create_result = create_endpoint.createAccountPlaylist(
            playlist_data.title.orEmpty(),
            playlist_data.description.orEmpty()
        )

        val account_playlist = RemotePlaylistData(
            id = create_result.fold(
                { if (!it.startsWith("VL")) "VL$it" else it },
                { return Result.failure(it) }
            )
        )

        val items = playlist_data.items
        if (!items.isNullOrEmpty()) {
            add_endpoint.addSongs(
                account_playlist,
                items.mapNotNull {
                    if (it is Song) it.id else null
                }
            ).onFailure {
                return Result.failure(it)
            }
        }

        PlaylistHolder.onPlaylistReplaced(playlist, account_playlist)

        db.transaction {
            db.playlistQueries.insertById(account_playlist.id, null)

            playlist_data.saveToDatabase(db, account_playlist)

            account_playlist.apply {
                CustomImageProvider.set(playlist.CustomImageProvider.get(db), db)
                ImageWidth.set(playlist.ImageWidth.get(db), db)
            }

            db.mediaItemPlayCountQueries.updateItemId(
                from_id = playlist.id,
                to_id = account_playlist.id
            )

            db.playlistQueries.removeById(playlist.id)
        }

        return Result.success(account_playlist)
    }

    override suspend fun performAndCommitActions(actions: List<Action>): Result<Unit> {
        val data: LocalPlaylistData = playlist.loadData(context).fold(
            { it },
            {
                return Result.failure(it)
            }
        )
        val items: MutableList<SongData> = (data.items ?: emptyList()).toMutableList()

        for (action in actions) {
            when (action) {
                is AddAction -> {
                    items.add(SongData(action.song_id))
                }
                is MoveAction -> {
                    items.add(action.to, items.removeAt(action.from))
                }
                is RemoveAction -> {
                    items.removeAt(action.index)
                }
            }
        }

        data.items = items

        val file: PlatformFile = MediaItemLibrary.getLocalPlaylistFile(playlist, context)
        return data.saveToFile(file, context)
    }
}
