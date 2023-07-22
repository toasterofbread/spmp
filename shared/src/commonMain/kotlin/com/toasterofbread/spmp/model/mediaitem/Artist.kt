package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

interface Artist: MediaItem {
    val subscribe_channel_id: String?
    val layouts: List<MediaItemLayout>?

    val is_for_item: Boolean
    val subscriber_count: Int?
    val subscribed: Boolean?

    override fun getType(): MediaItemType = MediaItemType.ARTIST
    override fun getURL(): String = "https://music.youtube.com/channel/$id"
}

fun Artist.isOwnChannel(): Boolean {
    TODO()
}

class ArtistData(
    override var id: String,
    override var subscribe_channel_id: String? = null,
    override var layouts: MutableList<MediaItemLayout> = mutableListOf(),

    override var is_for_item: Boolean = false,
    override var subscriber_count: Int? = null,
    override var subscribed: Boolean? = null
): MediaItemData(), Artist {
    companion object {
        fun fromId(database: Database, artist_id: String): ArtistData = database.transactionWithResult {
            val data = database.artistQueries.byId(artist_id).executeAsOne()
            ArtistData(
                data.id,
                data.subscribe_channel_id,
                database.artistLayoutQueries.byArtistId(artist_id).executeAsList().let { layouts_data ->
                    val layouts: MutableList<MediaItemLayout> = mutableListOf()
                    for (layout in layouts_data) {
                        layouts.add(MediaItemLayout.fromArtistLayoutData(database, layout))
                    }
                    return@let layouts
                }
            )
        }

        fun createForItem(item: MediaItem): ArtistData =
            ArtistData("", is_for_item = true)
    }
}

class ObservableArtist(
    id: String,
    db: Database,
    base: MediaItemObservableState,
    subscribe_channel_id_state: MutableState<String?>,
    layouts_state: State<List<MediaItemLayout>>
): ObservableMediaItem(id, db, base), Artist {
    override var subscribe_channel_id: String? by subscribe_channel_id_state
    override val layouts: List<MediaItemLayout> by layouts_state
    override val is_for_item: Boolean = false

    fun addLayout(type: MediaItemLayout.Type, title: LocalisedYoutubeString?, subtitle: LocalisedYoutubeString?, items: List<MediaItem>? = null): Int {
        return db.transactionWithResult {
            val layout_count = db.artistLayoutQueries.layoutCountByArtistId(id).executeAsOne().expr ?: 0
            db.artistLayoutQueries.addLayout(
                id,
                layout_count,
                type.ordinal.toLong(),
                title?.type?.ordinal?.toLong(),
                title?.key,
                subtitle?.type?.ordinal?.toLong(),
                subtitle?.key
            )

            if (items?.isNotEmpty() == true) {
                addItemsToLayout(layout_count.toInt(), items)
            }

            layout_count.toInt()
        }
    }

    fun addItemsToLayout(layout_index: Int, items: List<MediaItem>): Int {
        return db.transactionWithResult {
            val item_count = db.artistLayoutQueries.itemCountOfLayout(id, layout_index.toLong()).executeAsOne().expr ?: 0

            for (item in items.withIndex()) {
                db.artistLayoutQueries.addLayoutItem(item_count + item.index, item.value.id, id, layout_index.toLong())
            }

            (item_count + items.size).toInt()
        }
    }

    fun addLayout(layout: MediaItemLayout) {
        db.transaction {
            val layout_index = addLayout(layout.type!!, layout.title, layout.subtitle)
            addItemsToLayout(layout_index, layout.items)
        }
    }

    companion object {
        @Composable
        fun create(id: String, db: Database) =
            with(db.artistQueries) {
                ObservableArtist(
                    id,
                    db,
                    MediaItemObservableState.create(id, db),

                    subscribeChannelIdById(id).observeAsState(
                        { it.executeAsOne().subscribe_channel_id },
                        { updateSubscribeChannelIdById(it, id) }
                    ),
                    db.artistLayoutQueries.byArtistId(id).observeAsState(
                        { it.executeAsList() },
                        { throw IllegalStateException() }
                    )
                )
            }
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
