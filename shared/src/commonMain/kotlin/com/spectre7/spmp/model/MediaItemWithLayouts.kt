package com.spectre7.spmp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.ui.component.MediaItemLayout

open class MediaItemWithLayoutsData(item: MediaItem): MediaItemData(item) {
    open var feed_layouts: List<MediaItemLayout>? by mutableStateOf(null)
        protected set

    open fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean = false): MediaItemWithLayoutsData {
        if (value != feed_layouts && (feed_layouts == null || certain)) {
            feed_layouts = value
            onChanged(cached)
        }
        return this
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(feed_layouts))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 1)
        data.removeLast()?.also { supplyFeedLayouts(klaxon.parseFromJsonArray(it as JsonArray<*>), true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}

abstract class MediaItemWithLayouts(id: String): MediaItem(id) {
    abstract override val data: MediaItemWithLayoutsData

    open val feed_layouts: List<MediaItemLayout>? get() = data.feed_layouts

    override fun isFullyLoaded(): Boolean {
        return super.isFullyLoaded() && feed_layouts != null
    }
}
