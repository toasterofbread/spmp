package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.utils.addUnique
import com.toasterofbread.utils.lazyAssert

abstract class PlaylistEditor(val playlist: Playlist, val context: PlatformContext) {
    private var deleted: Boolean = false
    private val db: Database get() = context.database

    init {
        lazyAssert(
            { playlist.toString() }
        ) {
            playlist.isLocalPlaylist() || playlist.Loaded.get(db)
        }
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
                !playlist.isLocalPlaylist()
            }
            if (editable) {
                editable_playlists.addUnique(playlist.id)
            }
            else {
                editable_playlists.remove(playlist.id)
            }
        }

        fun Playlist.getLocalPlaylistEditor(context: PlatformContext): PlaylistEditor {
            lazyAssert { isPlaylistEditable() }
            lazyAssert { isLocalPlaylist() }
            return LocalPlaylistEditor(this, context)
        }

        @Composable
        fun Playlist.rememberEditorOrNull(context: PlatformContext): PlaylistEditor? {
            if (isLocalPlaylist()) {
                return remember { LocalPlaylistEditor(this, context) }
            }
            else if (editable_playlists.contains(id)) {
                val auth = context.ytapi.user_auth_state
                return remember(auth) {
                    if (auth != null) auth.AccountPlaylistEditor.getEditor(this)
                    else null
                }
            }
            return null
        }

        fun Playlist.isPlaylistEditable(): Boolean =
            isLocalPlaylist() || editable_playlists.contains(id)
    }
}

class LocalPlaylistEditor(playlist: Playlist, context: PlatformContext): PlaylistEditor(playlist, context) {
    suspend fun convertToAccountPlaylist(ytm_auth: YoutubeApi.UserAuthState): Result<PlaylistData> {
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

        val account_playlist = PlaylistData(
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
}
