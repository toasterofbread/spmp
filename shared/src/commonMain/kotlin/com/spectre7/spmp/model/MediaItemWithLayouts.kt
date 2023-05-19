package com.spectre7.spmp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.ui.component.MediaItemLayout

abstract class MediaItemWithLayoutsData(item: MediaItem): MediaItemData(item) {
    var feed_layouts: List<MediaItemLayout>? by mutableStateOf(null)
        private set

    fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean = false): MediaItemWithLayoutsData {
        if (value != feed_layouts && (feed_layouts == null || certain)) {
            feed_layouts = value
            onChanged(cached)
        }
        return this
    }
}

abstract class MediaItemWithLayouts(id: String): MediaItem(id) {
    abstract override val data: MediaItemWithLayoutsData

    val feed_layouts: List<MediaItemLayout>? get() = data.feed_layouts

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(feed_layouts))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 1)
        with(this@MediaItemWithLayouts.data) {
            data.removeLast()?.also { supplyFeedLayouts(klaxon.parseFromJsonArray(it as JsonArray<*>), true) }
        }
        return super.supplyFromSerialisedData(data, klaxon)
    }

    override fun isFullyLoaded(): Boolean {
        return super.isFullyLoaded() && feed_layouts != null
    }
}
