package com.toasterofbread.spmp.model.mediaitem.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

class AccountPlaylistItemData(override val data_item: PlaylistData): PlaylistItemData(data_item) {
    var playlist_type: PlaylistType? by mutableStateOf(null)
        private set

    fun supplyPlaylistType(value: PlaylistType?, certain: Boolean = false, cached: Boolean = false): PlaylistData {
        if (value != playlist_type && (playlist_type == null || certain)) {
            playlist_type = value
            onChanged(cached)
        }
        return data_item
    }

    var total_duration: Long? by mutableStateOf(null)
        private set

    fun supplyTotalDuration(value: Long?, certain: Boolean = false, cached: Boolean = false): PlaylistData {
        if (value != total_duration && (total_duration == null || certain)) {
            total_duration = value
            onChanged(cached)
        }
        return data_item
    }

    var item_count: Int? by mutableStateOf(null)
        private set

    fun supplyItemCount(value: Int?, certain: Boolean = false, cached: Boolean = false): PlaylistData {
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
        data.removeLast()?.also { supplyPlaylistType(PlaylistType.values()[it as Int], cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}