package com.toasterofbread.spmp.model.mediaitem.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemWithLayoutsData
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.ui.component.MediaItemLayout

abstract class PlaylistItemData(override val data_item: Playlist): MediaItemData(data_item), MediaItemWithLayoutsData {
    open var items: MutableList<MediaItem>? by mutableStateOf(null)

    open fun supplyItems(value: List<MediaItem>?, certain: Boolean = false, cached: Boolean = false): Playlist {
        if (items == null || certain) {
            items = value?.toMutableStateList()
            onChanged(cached)
        }
        return data_item
    }

    override fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean) {
        supplyItems(value?.single()?.items, certain, cached)
    }

    var year: Int? by mutableStateOf(null)
        private set

    fun supplyYear(value: Int?, certain: Boolean = false, cached: Boolean = false): Playlist {
        if (value != year && (year == null || certain)) {
            year = value
            onChanged(cached)
        }
        return data_item
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(year), klaxon.toJsonString(items))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 2)
        data.removeLast()?.also { supplyItems(klaxon.parseFromJsonArray(it as JsonArray<*>), true, cached = true) }
        data.removeLast()?.also { supplyYear(it as Int, cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}
