package com.spectre7.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.*
import com.spectre7.spmp.ui.component.MediaItemLayout

class AccountPlaylistItemData(override val data_item: AccountPlaylist): PlaylistItemData(data_item) {
    var playlist_type: Playlist.PlaylistType? by mutableStateOf(null)
        private set

    fun supplyPlaylistType(value: Playlist.PlaylistType?, certain: Boolean = false, cached: Boolean = false): AccountPlaylist {
        if (value != playlist_type && (playlist_type == null || certain)) {
            playlist_type = value
            onChanged(cached)
        }
        return data_item
    }

    var total_duration: Long? by mutableStateOf(null)
        private set

    fun supplyTotalDuration(value: Long?, certain: Boolean = false, cached: Boolean = false): AccountPlaylist {
        if (value != total_duration && (total_duration == null || certain)) {
            total_duration = value
            onChanged(cached)
        }
        return data_item
    }

    var item_count: Int? by mutableStateOf(null)
        private set

    fun supplyItemCount(value: Int?, certain: Boolean = false, cached: Boolean = false): AccountPlaylist {
        if (value != item_count && (item_count == null || certain)) {
            item_count = value
            onChanged(cached)
        }
        return data_item
    }

    var continuation: MediaItemLayout.Continuation? = null
    override fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean) {
        // TODO
        continuation = value?.single()?.continuation
        super.supplyFeedLayouts(value, certain, cached)
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(playlist_type?.ordinal), klaxon.toJsonString(total_duration), klaxon.toJsonString(item_count))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 4)
        data.removeLast()?.also { supplyItemCount(it as Int, cached = true) }
        data.removeLast()?.also { supplyTotalDuration((it as Int).toLong(), cached = true) }
        data.removeLast()?.also { supplyPlaylistType(Playlist.PlaylistType.values()[it as Int], cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}

class AccountPlaylist private constructor(id: String): Playlist(id) {
    override val data: AccountPlaylistItemData = AccountPlaylistItemData(this)

    var item_set_ids: List<String>? = null

    override var is_editable: Boolean? by mutableStateOf(null)
    override val playlist_type: PlaylistType? get() = checkNotDeleted(data.playlist_type)
    override val total_duration: Long? get() = checkNotDeleted(data.total_duration)
    override val item_count: Int? get() = checkNotDeleted(data.item_count)

    private val pending_edit_actions: MutableList<AccountPlaylistEditAction> = mutableListOf()

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
        DataApi.ytm_auth.onOwnPlaylistDeleted(this)
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

    fun editPlaylistDataManual(action: AccountPlaylistItemData.() -> Unit): AccountPlaylistItemData {
        checkNotDeleted()
        action(data)
        return data
    }

    companion object {
        private val playlists: MutableMap<String, AccountPlaylist> = mutableMapOf()

        fun formatId(id: String): String = id.removePrefix("VL")

        @Synchronized
        fun fromId(id: String): AccountPlaylist {
            val formatted_id = id//formatId(id)
            return playlists.getOrPut(formatted_id) {
                val playlist = AccountPlaylist(formatted_id)
                playlist.loadFromCache()
                return@getOrPut playlist
            }.getOrReplacedWith() as AccountPlaylist
        }
    }

    override val url: String get() = "https://music.youtube.com/playlist?list=$id"
}
