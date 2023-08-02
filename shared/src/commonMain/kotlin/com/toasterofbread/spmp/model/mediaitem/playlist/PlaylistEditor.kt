package com.toasterofbread.spmp.model.mediaitem.playlist

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.AccountPlaylistEditAction
import com.toasterofbread.spmp.api.addSongsToAccountPlaylist
import com.toasterofbread.spmp.api.createAccountPlaylist
import com.toasterofbread.spmp.api.deleteAccountPlaylist
import com.toasterofbread.spmp.api.editAccountPlaylist
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.PlaylistHolder
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.isLocalPlaylist
import com.toasterofbread.utils.addUnique
import com.toasterofbread.utils.lazyAssert

abstract class PlaylistEditor(val playlist: Playlist, val db: Database) {
    private var deleted: Boolean = false

    init {
        lazyAssert { playlist.Loaded.get(db) }
    }

    open fun addItem(item: Song, index: Int? = null) {
        check(!deleted)
        playlist.Items.addItem(item, index, db)
    }
    open fun removeItem(index: Int) {
        check(!deleted)
        playlist.Items.removeItem(index, db)
    }
    open fun moveItem(from: Int, to: Int) {
        check(!deleted)
        playlist.Items.moveItem(from, to, db)
    }

    open suspend fun applyItemChanges(): Result<Unit> {
        check(!deleted)
        return Result.success(Unit)
    }

    open suspend fun deletePlaylist(): Result<Unit> {
        check(!deleted)

        deleted = true
        PlaylistHolder.onPlaylistDeleted(playlist)
        db.playlistQueries.removeById(playlist.id)
        return Result.success(Unit)
    }

    companion object {
        private val editable_playlists: MutableList<String> = mutableStateListOf()

        fun setPlaylistEditable(playlist: Playlist, editable: Boolean) {
            lazyAssert {
                !playlist.isLocalPlaylist(SpMp.context.database)
            }
            if (editable) {
                editable_playlists.addUnique(playlist.id)
            }
            else {
                editable_playlists.remove(playlist.id)
            }
        }

        fun Playlist.getEditor(db: Database): PlaylistEditor {
            lazyAssert { isPlaylistEditable(db) }

            if (isLocalPlaylist(db)) {
                return LocalPlaylistEditor(this, db)
            }
            else {
                return AccountPlaylistEditor(this, db)
            }
        }

        @Composable
        fun Playlist.rememberEditorOrNull(db: Database): PlaylistEditor? {
            if (isLocalPlaylist(db)) {
                return remember { LocalPlaylistEditor(this, db) }
            }
            else if (editable_playlists.contains(id)) {
                return remember { AccountPlaylistEditor(this, db) }
            }
            return null
        }

        fun Playlist.isPlaylistEditable(db: Database): Boolean =
            isLocalPlaylist(db) || editable_playlists.contains(id)
    }
}

class LocalPlaylistEditor(playlist: Playlist, db: Database): PlaylistEditor(playlist, db) {
    suspend fun convertToAccountPlaylist(ytm_auth: YoutubeMusicAuthInfo): Result<PlaylistData> {
        require(ytm_auth.is_initialised)

        val playlist_data = playlist.getEmptyData().also {
            playlist.populateData(it, db)
        }

        val create_result = createAccountPlaylist(
            playlist_data.title.orEmpty(),
            playlist_data.description.orEmpty()
        )

        val account_playlist = PlaylistData(
            id = create_result.fold(
                { if (!it.startsWith("VL")) "VL$it" else it },
                { return Result.failure(it) }
            )
        )

        val items = playlist_data.items
        if (!items.isNullOrEmpty()) {
            addSongsToAccountPlaylist(
                account_playlist.id,
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
}

class AccountPlaylistEditor(playlist: Playlist, db: Database): PlaylistEditor(playlist, db) {
    private val initial_playlist_data = playlist.getEmptyData().also { playlist.populateData(it, db) }
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
        return deleteAccountPlaylist(playlist.id)
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
}
