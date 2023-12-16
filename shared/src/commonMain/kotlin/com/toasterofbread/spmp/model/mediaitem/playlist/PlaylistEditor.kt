package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.removeFromDatabase
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.endpoint.AccountPlaylistEditorEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException

abstract class PlaylistEditor(open val playlist: Playlist, val context: AppContext) {
    protected sealed class Action(val changes_items: Boolean = false) {
        data class SetTitle(val title: String): Action()
        data class SetImage(val image_url: String?): Action()
        data class SetImageWidth(val image_width: Float?): Action()

        data class Add(val song_id: String, val index: Int?): Action(true)
        data class Remove(val index: Int): Action(true)
        data class Move(val from: Int, val to: Int): Action(true) {
            init {
                require(from != to) { from.toString() }
                require(from >= 0) { from.toString() }
                require(to >= 0) { to.toString() }
            }
        }
    }

    private var deleted: Boolean = false
    private var pending_actions: MutableList<Action> = mutableListOf()

    fun transferStateTo(other: PlaylistEditor) {
        require(other.playlist.id == playlist.id)
        other.pending_actions = pending_actions
        other.deleted = deleted
    }

    fun setTitle(title: String) {
        pending_actions.add(Action.SetTitle(title))
    }
    fun setImage(image_url: String?) {
        pending_actions.add(Action.SetImage(image_url))
    }
    fun setImageWidth(image_width: Float?) {
        pending_actions.add(Action.SetImageWidth(image_width))
    }

    fun addItem(item: Song, index: Int? = null) {
        pending_actions.add(Action.Add(item.id, index))
    }
    fun removeItem(index: Int) {
        pending_actions.add(Action.Remove(index))
    }
    fun moveItem(from: Int, to: Int) {
        assert(canMoveItems())

        if (from != to) {
            pending_actions.add(Action.Move(from, to))
        }
    }

    open fun canAddItems(): Boolean = true
    open fun canRemoveItems(): Boolean = true
    open fun canMoveItems(): Boolean = true

    suspend fun applyChanges(exclude_item_changes: Boolean = false): Result<Unit> {
        if (pending_actions.isEmpty()) {
            return Result.success(Unit)
        }

        if (exclude_item_changes) {
            val result = performAndCommitActions(pending_actions.filter { !it.changes_items })
            pending_actions.removeAll { !it.changes_items }
            return result
        }

        val result = performAndCommitActions(pending_actions)
        pending_actions.clear()
        return result
    }

    suspend fun deletePlaylist(): Result<Unit> = withContext(NonCancellable) {
        if (deleted) {
            return@withContext Result.failure(IllegalStateException("Already deleted"))
        }

        performDeletion().onFailure {
            return@withContext Result.failure(it)
        }

        MediaItemLibrary.onPlaylistDeleted(playlist)

        deleted = true
        pending_actions.clear()

        if (playlist !is LocalPlaylist) {
            playlist.removeFromDatabase(context.database)
        }

        return@withContext Result.success(Unit)
    }

    protected abstract suspend fun performAndCommitActions(actions: List<Action>): Result<Unit>
    protected abstract suspend fun performDeletion(): Result<Unit>

    companion object {
        suspend fun Playlist.getEditorOrNull(context: AppContext): Result<PlaylistEditor?> = withContext(Dispatchers.IO) {
            val playlist = this@getEditorOrNull

            if (!isPlaylistEditable(context)) {
                return@withContext Result.success(null)
            }

            when (playlist) {
                is LocalPlaylist -> {
                    return@withContext Result.success(LocalPlaylistEditor(playlist, context))
                }
                is RemotePlaylist -> {
                    val editor_endpoint: AccountPlaylistEditorEndpoint? = context.ytapi.user_auth_state?.AccountPlaylistEditor
                    if (editor_endpoint == null) {
                        return@withContext Result.success(null)
                    }
                    else if (!editor_endpoint.isImplemented()) {
                        return@withContext Result.failure(EndpointNotImplementedException(editor_endpoint))
                    }

                    val playlist_data: Result<RemotePlaylistData> = playlist.loadData(context, force = true)

                    return@withContext playlist_data.fold(
                        { Result.success(editor_endpoint.getEditor(it)) },
                        { Result.failure(it) }
                    )
                }
                else -> {
                    return@withContext Result.failure(NotImplementedError(this::class.toString()))
                }
            }
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

        fun Playlist.isPlaylistEditable(context: AppContext): Boolean {
            when (this) {
                is LocalPlaylist -> {
                    return true
                }
                is RemotePlaylistData -> {
                    return false
                }
                is RemotePlaylist -> {
                    val owner: Artist? = Owner.get(context.database)
                    return owner != null && owner.id == context.ytapi.user_auth_state?.own_channel?.id
                }
                else -> {
                    throw NotImplementedError(this::class.toString())
                }
            }
        }
    }
}

class LocalPlaylistEditor(override val playlist: LocalPlaylist, context: AppContext): PlaylistEditor(playlist, context) {
    override suspend fun performAndCommitActions(actions: List<Action>): Result<Unit> {
        if (actions.isEmpty()) {
            return Result.success(Unit)
        }

        val param_data: LocalPlaylistData? = if (playlist is LocalPlaylistData) playlist else null

        val data: LocalPlaylistData = playlist.loadData(context).fold(
            { it },
            {
                return Result.failure(it)
            }
        )
        val items: MutableList<SongData> = (data.items ?: emptyList()).toMutableList()

        for (action in actions) {
            when (action) {
                is Action.Add -> {
                    items.add(SongData(action.song_id))
                }
                is Action.Move -> {
                    items.add(action.to, items.removeAt(action.from))
                }
                is Action.Remove -> {
                    items.removeAt(action.index)
                }

                is Action.SetTitle -> {
                    data.title = action.title
                    param_data?.title = action.title
                }
                is Action.SetImage -> {
                    data.custom_image_url = action.image_url
                    param_data?.custom_image_url = action.image_url
                }
                is Action.SetImageWidth -> {
                    data.image_width = action.image_width
                    param_data?.image_width = action.image_width
                }
            }
        }

        data.items = items
        param_data?.items = items

        val file: PlatformFile = MediaItemLibrary.getLocalPlaylistFile(playlist, context)
        return data.saveToFile(file, context)
    }

    override suspend fun performDeletion(): Result<Unit> {
        val file: PlatformFile = MediaItemLibrary.getLocalPlaylistFile(playlist, context)
        if (file.delete()) {
            return Result.success(Unit)
        }
        else {
            return Result.failure(IOException("Failed to delete file at ${file.absolute_path}"))
        }
    }
}
