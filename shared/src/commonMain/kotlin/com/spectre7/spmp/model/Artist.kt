package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.*
import com.spectre7.spmp.ui.component.ArtistPreviewLong
import com.spectre7.spmp.ui.component.ArtistPreviewSquare
import com.spectre7.utils.getString
import kotlin.concurrent.thread

class Artist private constructor (
    id: String,
    is_for_item: Boolean = false
): MediaItemWithLayouts(id) {

    init {
        supplyArtist(this, true)
    }

    var is_for_item: Boolean = is_for_item
        private set

    private var _subscribe_channel_id: String? by mutableStateOf(null)
    val subscribe_channel_id: String?
        get() = _subscribe_channel_id

    fun supplySubscribeChannelId(value: String?, certain: Boolean): MediaItem {
        if (value != null && (_subscribe_channel_id == null || certain)) {
            _subscribe_channel_id = value
        }
        return this
    }

    var subscriber_count: Int? by mutableStateOf(null)
        private set

    fun supplySubscriberCount(value: Int?, certain: Boolean): MediaItem {
        if (value != null && (subscriber_count == null || certain)) {
            subscriber_count = value
        }
        return this
    }

    var subscribed: Boolean? by mutableStateOf(null)
    var is_own_channel: Boolean by mutableStateOf(false)

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(stringToJson(subscribe_channel_id), klaxon.toJsonString(is_for_item), klaxon.toJsonString(subscriber_count))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 3)
        data.removeLast()?.also { subscriber_count = it as Int }
        is_for_item = data.removeLast() as Boolean
        data.removeLast()?.also { _subscribe_channel_id = it as String }
        return super.supplyFromSerialisedData(data, klaxon)
    }

    override fun isFullyLoaded(): Boolean {
        return super.isFullyLoaded() && _subscribe_channel_id != null
    }

    companion object {
        private val artists: MutableMap<String, Artist> = mutableMapOf()

        fun fromId(id: String): Artist {
            synchronized(artists) {
                return artists.getOrPut(id) {
                    val artist = Artist(id)
                    artist.loadFromCache()
                    return@getOrPut artist
                }.getOrReplacedWith() as Artist
            }
        }

        fun clearStoredItems(): Int {
            val amount = artists.size
            artists.clear()
            return amount
        }

        fun createForItem(item: MediaItem): Artist {
            synchronized(artists) {
                val id = "FS" + item.id
                return artists.getOrPut(id) {
                    val artist = Artist(id, true)
                    artist.loadFromCache()
                    return@getOrPut artist
                }.getOrReplacedWith() as Artist
            }
        }

        fun createTemp(id: String = "TEMP"): Artist {
            return Artist(id)
        }
    }

    @Composable
    override fun PreviewSquare(params: PreviewParams) {
        ArtistPreviewSquare(this, params)
    }

    @Composable
    override fun PreviewLong(params: PreviewParams) {
        ArtistPreviewLong(this, params)
    }

    override val url: String get() = "https://music.youtube.com/channel/$id"

    fun getFormattedSubscriberCount(): String {
        return getString("artist_x_subscribers").replace("\$x", subscriber_count?.let { amountToString(it, SpMp.ui_language) } ?: "0")
    }

    fun updateSubscribed() {
        check(!is_for_item)

        if (is_own_channel) {
            return
        }
        subscribed = isSubscribedToArtist(this).getOrThrowHere()
    }

    fun toggleSubscribe(toggle_before_fetch: Boolean = false, onFinished: ((success: Boolean, subscribing: Boolean) -> Unit)? = null) {
        check(!is_for_item)
        check(DataApi.ytm_authenticated)

        thread {
            if (subscribed == null) {
                throw IllegalStateException()
            }

            val target = !subscribed!!

            if (toggle_before_fetch) {
                subscribed = target
            }

            subscribeOrUnsubscribeArtist(this, target).getOrThrowHere()
            updateSubscribed()

            onFinished?.invoke(subscribed == target, target)
        }
    }
}
