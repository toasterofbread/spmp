package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.api.*
import com.toasterofbread.spmp.model.mediaitem.data.AccountPlaylistItemData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.platform.PlatformContext

class AccountPlaylist private constructor(id: String, context: PlatformContext): Playlist(id, context) {
    override val url: String get() = "https://music.youtube.com/playlist?list=$id"

    override val data: AccountPlaylistItemData = AccountPlaylistItemData(this)

    var item_set_ids: List<String>? = null

    override var is_editable: Boolean? by mutableStateOf(null)
    override val playlist_type: PlaylistType? get() = checkNotDeleted(data.playlist_type)
    override val total_duration: Long? get() = checkNotDeleted(data.total_duration)
    override val item_count: Int? get() = checkNotDeleted(data.item_count)

    override fun addItem(item: MediaItem) {
        super.addItem(item)
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
        checkNotDeleted()
        val result = deleteAccountPlaylist(id)
        if (result.isSuccess) {
            onDeleted()
        }
        Api.ytm_auth.onOwnPlaylistDeleted(this)
        return result
    }

    override suspend fun saveItems(): Result<Unit> {
        checkNotDeleted()
        val actions: List<AccountPlaylistEditAction>
        synchronized(pending_edit_actions) {
            actions = pending_edit_actions.toList()
            pending_edit_actions.clear()
        }
        return editAccountPlaylist(this, actions)
    }

    fun editPlaylistData(action: AccountPlaylistItemData.() -> Unit): AccountPlaylist {
        checkNotDeleted()
        editData {
            action(this as AccountPlaylistItemData)
        }
        return this
    }

    private val pending_edit_actions: MutableList<AccountPlaylistEditAction> = mutableListOf()

    companion object {
        private val playlists: MutableMap<String, AccountPlaylist> = mutableMapOf()

        fun formatId(id: String): String = id.removePrefix("VL")

        @Synchronized
        fun fromId(id: String, context: PlatformContext = SpMp.context): AccountPlaylist {
            return playlists.getOrPut(id) {
                val playlist = AccountPlaylist(id, context)
                playlist.loadFromCache()
                return@getOrPut playlist
            }
        }
    }
}
