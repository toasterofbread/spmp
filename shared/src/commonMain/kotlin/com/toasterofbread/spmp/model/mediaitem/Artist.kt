package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayoutData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

class ArtistRef(override val id: String): Artist

interface Artist: MediaItem {
    override fun getType(): MediaItemType = MediaItemType.ARTIST
    override fun getURL(): String = "https://music.youtube.com/channel/$id"

    // Properties

    val SubscribeChannelId: Property<String?> get() = SingleProperty(
        { artistQueries.subscribeChannelIdById(id) }, { subscribe_channel_id }, { artistQueries.updateSubscribeChannelIdById(it, id) }
    )
    val Layouts get() = ListProperty(
        getValue = {
            this.map { layout ->
                ArtistLayout(layout.layout_index, id)
            }
        },
        getQuery = { artistLayoutQueries.byArtistId(id) },
        getSize = { artistLayoutQueries.layoutCount(id).executeAsOne() },
        addItem = { item, index ->
            artistLayoutQueries.insertLayoutAtIndex(id, index)
        },
        removeItem = { index ->
            artistLayoutQueries.removeLayoutAtIndex(id, index)
        },
        setItemIndex = { from, to ->
            artistLayoutQueries.updateLayoutIndex(from = from, to = to, artist_id = id)
        },
        clearItems = { from_index ->
            artistLayoutQueries.clearLayouts(id, from_index)
        }
    )
    val SubscriberCount: Property<Int?> get() = SingleProperty(
        { artistQueries.subscriberCountById(id) }, { subscriber_count?.toInt() }, { artistQueries.updateSubscriberCountById(it?.toLong(), id) }
    )

    // User properties

    val Subscribed: Property<Boolean?> get() = SingleProperty(
        { artistQueries.subscribedById(id) },
        { subscribed.fromNullableSQLBoolean() },
        { artistQueries.updateSubscriberCountById(it.toNullableSQLBoolean(), id) }
    )
}

class ArtistData(
    override var id: String,
    var subscribe_channel_id: String? = null,
    var layouts: MutableList<ArtistLayoutData> = mutableListOf(),
    var subscriber_count: Int? = null,

    var subscribed: Boolean? = null,
    var is_for_item: Boolean = false
): MediaItemData(), Artist {
    override fun saveToDatabase(db: Database) {
        db.transaction {
            super.saveToDatabase(db)

            SubscribeChannelId.set(subscribe_channel_id, db)

            Layouts.clearItems(db, 0)
            for (layout in layouts) {

            }

            Layouts.overwriteItems(layouts, db)

            SubscriberCount.set(subscriber_count, db)
        }
    }

    companion object {
        fun createForItem(item: MediaItem): ArtistData =
            ArtistData("", is_for_item = true)
    }
}

//class Artist private constructor (
//    id: String,
//    context: PlatformContext,
//    var is_for_item: Boolean = false,
//): MediaItem(id, context), MediaItemWithLayouts {
//
//    override val url: String get() = "https://music.youtube.com/channel/$id"
//    override val data: ArtistItemData = ArtistItemData(this)
//
//    val subscribe_channel_id: String? get() = data.subscribe_channel_id
//    val subscriber_count: Int? get() = data.subscriber_count
//
//    var subscribed: Boolean? by mutableStateOf(null)
//    var is_own_channel: Boolean by mutableStateOf(false)
//
//    override val feed_layouts: List<MediaItemLayout>?
//        @Composable
//        get() = data.feed_layouts
//
//    override suspend fun getFeedLayouts(): Result<List<MediaItemLayout>> =
//        getGeneralValue { data.feed_layouts }
//
//    fun getReadableSubscriberCount(): String {
//        return subscriber_count?.let { subs ->
//            getString("artist_x_subscribers").replace("\$x", amountToString(subs, SpMp.ui_language))
//        } ?: ""
//    }
//
//    suspend fun updateSubscribed(): Result<Unit> {
//        if (is_for_item || is_own_channel) {
//            return Result.success(Unit)
//        }
//
//        val result = isSubscribedToArtist(this)
//        subscribed = result.getOrNull()
//
//        return result.unit()
//    }
//
//    suspend fun toggleSubscribe(toggle_before_fetch: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
//        check(!is_for_item)
//        check(Api.ytm_authenticated)
//
//        if (subscribed == null) {
//            return@withContext Result.failure(IllegalStateException())
//        }
//
//        val target = !subscribed!!
//        if (toggle_before_fetch) {
//            subscribed = target
//        }
//
//        val result = subscribeOrUnsubscribeArtist(this@Artist, target)
//        return@withContext result.unit()
//    }
//
//    fun editArtistData(action: ArtistItemData.() -> Unit): Artist {
//        if (is_for_item || is_temp) {
//            action(data)
//        }
//        else {
//            editData {
//                action(this as ArtistItemData)
//            }
//        }
//        return this
//    }
//
//    override fun PreviewSquare(params: MediaItemPreviewParams) {
//    @Composable
//        ArtistPreviewSquare(this, params)
//    }
//
//    @Composable
//    override fun PreviewLong(params: MediaItemPreviewParams) {
//        ArtistPreviewLong(this, params)
//    }
//
//    private val is_temp: Boolean get() = id.isBlank()
//
//    companion object {
//        private val artists: MutableMap<String, Artist> = mutableMapOf()
//
//        fun fromId(id: String, context: PlatformContext = SpMp.context): Artist {
//            check(id.isNotBlank())
//
//            synchronized(artists) {
//                return artists.getOrPut(id) {
//                    val artist = Artist(id, context)
//                    artist.loadFromCache()
//                    return@getOrPut artist
//                }
//            }
//        }
//
//        fun createForItem(item: MediaItem, context: PlatformContext = SpMp.context): Artist {
//            synchronized(artists) {
//                val id = "FS" + item.id
//                return artists.getOrPut(id) {
//                    val artist = Artist(id, context, true)
//                    artist.loadFromCache()
//                    return@getOrPut artist
//                }
//            }
//        }
//
//        fun createTemp(id: String = ""): Artist {
//            return Artist(id, SpMp.context)
//        }
//    }
//}
