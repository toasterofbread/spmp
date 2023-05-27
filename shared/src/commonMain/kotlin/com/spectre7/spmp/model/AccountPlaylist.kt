package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare

class AccountPlaylistItemData(override val data_item: AccountPlaylist): MediaItemWithLayoutsData(data_item) {
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

    var year: Int? by mutableStateOf(null)
        private set

    fun supplyYear(value: Int?, certain: Boolean = false, cached: Boolean = false): AccountPlaylist {
        if (value != year && (year == null || certain)) {
            year = value
            onChanged(cached)
        }
        return data_item
    }

    // TODO
    var is_editable: Boolean? by mutableStateOf(null)
        private set

    fun supplyIsEditable(value: Boolean?, certain: Boolean = false, cached: Boolean = false): AccountPlaylist {
        if (value != is_editable && (is_editable == null || certain)) {
            is_editable = value
            onChanged(cached)
        }
        return data_item
    }
}

class AccountPlaylist private constructor(id: String): Playlist(id) {
    override val data: AccountPlaylistItemData = AccountPlaylistItemData(this)

    override val is_editable: Boolean? get() = data.is_editable
    override val playlist_type: PlaylistType? get() = data.playlist_type
    override val total_duration: Long? get() = data.total_duration
    override val item_count: Int? get() = data.item_count
    override val year: Int? get() = data.year

    fun editPlaylistData(action: AccountPlaylistItemData.() -> Unit): AccountPlaylist {
        editData {
            action(this as AccountPlaylistItemData)
        }
        return this
    }

    fun editPlaylistDataManual(action: AccountPlaylistItemData.() -> Unit): AccountPlaylistItemData {
        action(data)
        return data
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(playlist_type?.ordinal), klaxon.toJsonString(total_duration), klaxon.toJsonString(item_count), klaxon.toJsonString(year))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 4)
        with(this@AccountPlaylist.data) {
            data.removeLast()?.also { supplyYear(it as Int, cached = true) }
            data.removeLast()?.also { supplyItemCount(it as Int, cached = true) }
            data.removeLast()?.also { supplyTotalDuration((it as Int).toLong(), cached = true) }
            data.removeLast()?.also { supplyPlaylistType(PlaylistType.values()[it as Int], cached = true) }
        }
        return super.supplyFromSerialisedData(data, klaxon)
    }

    companion object {
        private val playlists: MutableMap<String, AccountPlaylist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): AccountPlaylist {
            return playlists.getOrPut(id) {
                val playlist = AccountPlaylist(id)
                playlist.loadFromCache()
                return@getOrPut playlist
            }.getOrReplacedWith() as AccountPlaylist
        }

        fun clearStoredItems(): Int {
            val amount = playlists.size
            playlists.clear()
            return amount
        }
    }

    override val url: String get() = "https://music.youtube.com/playlist?list=$id"
}
