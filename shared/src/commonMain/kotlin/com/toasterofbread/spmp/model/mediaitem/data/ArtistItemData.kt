package com.toasterofbread.spmp.model.mediaitem.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItemWithLayoutsData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

class ArtistItemData(override val data_item: Artist): MediaItemData(data_item), MediaItemWithLayoutsData {
    var subscribe_channel_id: String? by mutableStateOf(null)
        private set

    fun supplySubscribeChannelId(value: String?, certain: Boolean = false, cached: Boolean = false): Artist {
        if (value != subscribe_channel_id && (subscribe_channel_id == null || certain)) {
            subscribe_channel_id = value
            onChanged(cached)
        }
        return data_item
    }

    var subscriber_count: Int? by mutableStateOf(null)
        private set

    fun supplySubscriberCount(value: Int?, certain: Boolean = false, cached: Boolean = false): Artist {
        if (value != subscriber_count && (subscriber_count == null || certain)) {
            subscriber_count = value
            onChanged(cached)
        }
        return data_item
    }

    var feed_layouts: List<MediaItemLayout>? by mutableStateOf(null)
        private set

    override fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean) {
        if (value != feed_layouts && (feed_layouts == null || certain)) {
            feed_layouts = value
            onChanged(cached)
        }
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(subscribe_channel_id), klaxon.toJsonString(data_item.is_for_item), klaxon.toJsonString(subscriber_count), klaxon.toJsonString(feed_layouts))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 4)
        data.removeLast()?.also { supplyFeedLayouts(klaxon.parseFromJsonArray(it as JsonArray<*>), true, cached = true) }
        data.removeLast()?.also { supplySubscriberCount(it as Int, cached = true) }
        data_item.is_for_item = data.removeLast() as Boolean
        data.removeLast()?.also { supplySubscribeChannelId(it as String, cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}
