package com.toasterofbread.spmp.model.mediaitem

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.isSubscribedToArtist
import com.toasterofbread.spmp.api.subscribeOrUnsubscribeArtist
import com.toasterofbread.spmp.api.unit
import com.toasterofbread.spmp.model.mediaitem.data.ArtistItemData
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.amountToString
import com.toasterofbread.spmp.ui.component.mediaitempreview.ArtistPreviewLong
import com.toasterofbread.spmp.ui.component.mediaitempreview.ArtistPreviewSquare
import com.toasterofbread.spmp.ui.component.MediaItemLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Artist private constructor (
    id: String,
    context: PlatformContext,
    var is_for_item: Boolean = false,
): MediaItem(id, context), MediaItemWithLayouts {

    override val url: String get() = "https://music.youtube.com/channel/$id"
    override val data: ArtistItemData = ArtistItemData(this)

    val subscribe_channel_id: String? get() = data.subscribe_channel_id
    val subscriber_count: Int? get() = data.subscriber_count

    var subscribed: Boolean? by mutableStateOf(null)
    var is_own_channel: Boolean by mutableStateOf(false)

    override val feed_layouts: List<MediaItemLayout>?
        @Composable
        get() = data.feed_layouts

    override suspend fun getFeedLayouts(): Result<List<MediaItemLayout>> =
        getGeneralValue { data.feed_layouts }

    fun getReadableSubscriberCount(): String {
        return subscriber_count?.let { subs ->
            getString("artist_x_subscribers").replace("\$x", amountToString(subs, SpMp.ui_language))
        } ?: ""
    }

    suspend fun updateSubscribed(): Result<Unit> {
        if (is_for_item || is_own_channel) {
            return Result.success(Unit)
        }

        val result = isSubscribedToArtist(this)
        subscribed = result.getOrNull()

        return result.unit()
    }

    suspend fun toggleSubscribe(toggle_before_fetch: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        check(!is_for_item)
        check(Api.ytm_authenticated)

        if (subscribed == null) {
            return@withContext Result.failure(IllegalStateException())
        }

        val target = !subscribed!!
        if (toggle_before_fetch) {
            subscribed = target
        }

        val result = subscribeOrUnsubscribeArtist(this@Artist, target)
        return@withContext result.unit()
    }

    fun editArtistData(action: ArtistItemData.() -> Unit): Artist {
        if (is_for_item || is_temp) {
            action(data)
        }
        else {
            editData {
                action(this as ArtistItemData)
            }
        }
        return this
    }

    @Composable
    override fun PreviewSquare(params: MediaItemPreviewParams) {
        ArtistPreviewSquare(this, params)
    }

    @Composable
    override fun PreviewLong(params: MediaItemPreviewParams) {
        ArtistPreviewLong(this, params)
    }

    private val is_temp: Boolean get() = id.isBlank()

    companion object {
        private val artists: MutableMap<String, Artist> = mutableMapOf()

        fun fromId(id: String, context: PlatformContext = SpMp.context): Artist {
            check(id.isNotBlank())

            synchronized(artists) {
                return artists.getOrPut(id) {
                    val artist = Artist(id, context)
                    artist.loadFromCache()
                    return@getOrPut artist
                }
            }
        }

        fun createForItem(item: MediaItem, context: PlatformContext = SpMp.context): Artist {
            synchronized(artists) {
                val id = "FS" + item.id
                return artists.getOrPut(id) {
                    val artist = Artist(id, context, true)
                    artist.loadFromCache()
                    return@getOrPut artist
                }
            }
        }

        fun createTemp(id: String = ""): Artist {
            return Artist(id, SpMp.context)
        }
    }
}
