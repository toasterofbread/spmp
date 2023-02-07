package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.BrowseData
import com.spectre7.spmp.api.isSubscribedToArtist
import com.spectre7.spmp.api.subscribeOrUnsubscribeArtist
import com.spectre7.spmp.ui.component.ArtistPreviewLong
import com.spectre7.spmp.ui.component.ArtistPreviewSquare
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.sendToast
import kotlin.concurrent.thread

class Artist private constructor (
    id: String
): MediaItem(id) {

    // Data
    override lateinit var title: String
    var description: String? = null
    lateinit var feed_rows: List<MediaItemLayout>
    lateinit var subscribe_channel_id: String

    var subscribed: Boolean? by mutableStateOf(null)

    companion object {
        private val artists: MutableMap<String, Artist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Artist {
            return artists.getOrElse(id) {
                val ret = Artist(id)
                artists[id] = ret
                return ret
            }.getOrReplacedWith() as Artist
        }

        fun serialisable(id: String): Serialisable {
            return Serialisable(Type.ARTIST.ordinal, id)
        }
    }

    @Composable
    override fun PreviewSquare(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        ArtistPreviewSquare(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    @Composable
    override fun PreviewLong(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        ArtistPreviewLong(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    override fun getAssociatedArtist(): Artist {
        return this
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/channel/$id"
    }

    override fun subInitWithData(data: Any) {
        if (data !is BrowseData) {
            throw ClassCastException(data.javaClass.name)
        }

        title = data.name!!
        description = data.description
        feed_rows = data.item_layouts
        subscribe_channel_id = data.subscribe_channel_id ?: id
    }

    fun getFormattedSubscriberCount(): String {
        return "Unknown"
//        val subs = subscriber_count.toInt()
//        if (subs >= 1000000) {
//            return "${subs / 1000000}M"
//        }
//        else if (subs >= 1000) {
//            return "${subs / 1000}K"
//        }
//        else {
//            return "$subs"
//        }
    }

    fun toggleSubscribe(toggle_before_fetch: Boolean = false, notify_failure: Boolean = false) {
        thread {
            if (subscribed == null) {
                throw IllegalStateException()
            }

            val target = !subscribed!!

            if (toggle_before_fetch) {
                subscribed = target
            }

            subscribeOrUnsubscribeArtist(this, target).getDataOrThrow()
            subscribed = isSubscribedToArtist(this).getNullableDataOrThrow()

            if (notify_failure && subscribed != target) {
                sendToast(
                    if (target) "Subscribing to $title failed"
                    else        "Unsubscribed from $title failed"
                )
            }
        }
    }

}