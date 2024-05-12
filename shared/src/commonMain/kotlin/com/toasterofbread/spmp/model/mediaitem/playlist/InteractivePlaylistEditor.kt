package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.removeFromDatabase
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.endpoint.AccountPlaylistEditorEndpoint
import dev.toastbits.ytmkt.model.external.PlaylistEditor
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class InteractivePlaylistEditor(
    val playlist: Playlist,
    private val editor: PlaylistEditor,
    val context: AppContext
): PlaylistEditor by editor {
    private var deleted: Boolean = false
    private var pending_actions: MutableList<PlaylistEditor.Action> = mutableListOf()

    fun transferStateTo(other: InteractivePlaylistEditor) {
        require(other.playlist.id == playlist.id)
        other.pending_actions = pending_actions
        other.deleted = deleted
    }

    fun setTitle(title: String) {
        pending_actions.add(PlaylistEditor.Action.SetTitle(title))
    }
    fun setImage(image_url: String?) {
        pending_actions.add(PlaylistEditor.Action.SetImage(image_url))
    }
    fun setImageWidth(image_width: Float?) {
        pending_actions.add(PlaylistEditor.Action.SetImageWidth(image_width))
    }

    fun addItem(item: Song, index: Int? = null) {
        pending_actions.add(PlaylistEditor.Action.Add(item.id, index))
    }
    fun removeItem(index: Int) {
        pending_actions.add(PlaylistEditor.Action.Remove(index))
    }
    fun moveItem(from: Int, to: Int) {
        if (from != to) {
            pending_actions.add(PlaylistEditor.Action.Move(from, to))
        }
    }

    suspend fun applyChanges(exclude_item_changes: Boolean = false): Result<Unit> {
        if (pending_actions.isEmpty()) {
            return Result.success(Unit)
        }

        if (exclude_item_changes) {
            val result: Result<Unit> = performAndCommitActions(pending_actions.filter { !it.changes_items })
            pending_actions.removeAll { !it.changes_items }
            return result
        }

        val result: Result<Unit> = performAndCommitActions(pending_actions)
        pending_actions.clear()
        return result
    }

    override suspend fun performAndCommitActions(actions: List<PlaylistEditor.Action>): Result<Unit> {
        val result: Result<Unit> = editor.performAndCommitActions(actions)

        if (playlist !is RemotePlaylist) {
            return result
        }

        context.database.transaction {
            for (action in actions) {
                when (action) {
                    is PlaylistEditor.Action.Add -> {
                        playlist.Items.addItem(SongRef(action.song_id), null, context.database)
                    }
                    is PlaylistEditor.Action.Move -> {
                        playlist.Items.moveItem(action.from, action.to, context.database)
                    }
                    is PlaylistEditor.Action.Remove -> {
                        playlist.Items.removeItem(action.index, context.database)
                    }
                    is PlaylistEditor.Action.SetTitle -> {
                        playlist.Title.set(action.title, context.database)
                    }
                    is PlaylistEditor.Action.SetDescription -> {
                        playlist.Description.set(action.description, context.database)
                    }
                    is PlaylistEditor.Action.SetImage -> {
                        playlist.CustomImageUrl.set(action.image_url, context.database)
                    }
                    is PlaylistEditor.Action.SetImageWidth -> {
                        playlist.ImageWidth.set(action.image_width, context.database)
                    }
                }
            }
        }

        return result
    }

    suspend fun deletePlaylist(): Result<Unit> = withContext(NonCancellable) {
        assert(canPerformDeletion())

        if (deleted) {
            return@withContext Result.failure(IllegalStateException("Already deleted"))
        }

        performDeletion().onFailure {
            return@withContext Result.failure(it)
        }

        MediaItemLibrary.onPlaylistDeleted(playlist)

        deleted = true
        pending_actions.clear()

        if (playlist is LocalPlaylist) {
            context.database.pinnedItemQueries.remove(playlist.id, playlist.getType().ordinal.toLong())
        }
        else {
            playlist.removeFromDatabase(context.database)
        }

        return@withContext Result.success(Unit)
    }

    companion object {
        suspend fun Playlist.getEditorOrNull(context: AppContext): Result<InteractivePlaylistEditor?> = runCatching {
            val playlist = this@getEditorOrNull

            if (!isPlaylistEditable(context)) {
                return@runCatching null
            }

            val editor: PlaylistEditor =
                when (playlist) {
                    is LocalPlaylist -> {
                        LocalPlaylistEditor(playlist, context)
                    }
                    is RemotePlaylist -> {
                        val editor_endpoint: AccountPlaylistEditorEndpoint? = context.ytapi.user_auth_state?.AccountPlaylistEditor
                        if (editor_endpoint == null) {
                            return@runCatching null
                        }

                        var items: List<SongData>? = null
                        var item_set_ids: List<String>? = null

                        if (playlist is RemotePlaylistData) {
                            if (playlist.items != null && playlist.item_set_ids != null) {
                                items = playlist.items
                                item_set_ids = playlist.item_set_ids
                            }
                            else if (playlist.loaded) {
                                // Playlist is loaded but has no items and/or set IDs
                                return@runCatching null
                            }
                        }

                        if (items == null) {
                            val playlist_data: RemotePlaylistData = playlist.loadData(context, force = true).getOrThrow()
                            items = playlist_data.items
                            item_set_ids = playlist_data.item_set_ids
                        }

                        editor_endpoint.getEditor(
                            playlist.id,
                            items.orEmpty().map { it.id },
                            item_set_ids.orEmpty()
                        )
                    }
                    else -> throw NotImplementedError(this::class.toString())
                }

            return@runCatching InteractivePlaylistEditor(this, editor, context)
        }

        @Composable
        fun Playlist.rememberEditorOrNull(context: AppContext, onFailure: ((Throwable) -> Unit)? = null): State<PlaylistEditor?> {
            val editor_state: MutableState<PlaylistEditor?> = remember(this) { mutableStateOf(null) }
            LaunchedEffect(this) {
                editor_state.value = getEditorOrNull(context).fold(
                    { it },
                    {
                        onFailure?.invoke(it)
                        null
                    }
                )
            }
            return editor_state
        }

        fun Playlist.isPlaylistEditable(context: AppContext): Boolean =
            when (this) {
                is LocalPlaylist -> true
                is RemotePlaylist -> {
                    val owner: Artist? = Owner.get(context.database)
                    owner != null && owner.id == context.ytapi.user_auth_state?.own_channel_id
                }
                else -> throw NotImplementedError(this::class.toString())
            }
    }
}
