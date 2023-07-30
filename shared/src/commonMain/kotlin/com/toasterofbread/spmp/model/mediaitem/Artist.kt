package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType

class ArtistRef(override val id: String): Artist

interface Artist: MediaItem {
    override fun getType(): MediaItemType = MediaItemType.ARTIST
    override fun getURL(): String = "https://music.youtube.com/channel/$id"

    override fun getEmptyData(): ArtistData = ArtistData(id)
    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)
        (data as ArtistData).apply {
            subscribe_channel_id = SubscribeChannelId.get(db)
            layouts = Layouts.get(db)?.toMutableList()
            subscriber_count = SubscriberCount.get(db)
            is_for_item = IsForItem.get(db)
        }
    }

    override suspend fun loadData(db: Database): Result<ArtistData> {
        return super.loadData(db) as Result<ArtistData>
    }

    // Properties

    val SubscribeChannelId: Property<String?> get() = SingleProperty(
        { artistQueries.subscribeChannelIdById(id) }, { subscribe_channel_id }, { artistQueries.updateSubscribeChannelIdById(it, id) }
    )
    val Layouts get() = ListProperty(
        getValue = {
            this.map { layout_index ->
                ArtistLayout(layout_index, id)
            }
        },
        getQuery = { artistLayoutQueries.byArtistId(id) },
        getSize = { artistLayoutQueries.layoutCount(id).executeAsOne() },
        addItem = { item, index ->
            artistLayoutQueries.insertLayoutAtIndex(id, index)
            item.layout_index = index
        },
        removeItem = { index ->
            artistLayoutQueries.removeLayoutAtIndex(id, index)
        },
        setItemIndex = { from, to ->
            artistLayoutQueries.updateLayoutIndex(from = from, to = to, artist_id = id)
        },
        clearItems = { from_index ->
            artistLayoutQueries.clearLayouts(id, from_index)
        },
        prerequisite = Loaded
    )

    val SubscriberCount: Property<Int?> get() = SingleProperty(
        { artistQueries.subscriberCountById(id) }, { subscriber_count?.toInt() }, { artistQueries.updateSubscriberCountById(it?.toLong(), id) }
    )
    val IsForItem: Property<Boolean> get() = SingleProperty(
        { artistQueries.isForItemById(id) },
        { is_for_item.fromSQLBoolean() },
        { artistQueries.updateIsForItemById(it.toSQLBoolean(), id) }
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
    var layouts: MutableList<ArtistLayout>? = null,
    var subscriber_count: Int? = null,
    var is_for_item: Boolean = false,

    var subscribed: Boolean? = null
): MediaItemData(), Artist {
    override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
        db.transaction { with(apply_to_item as Artist) {
            super.saveToDatabase(db, apply_to_item)

            SubscribeChannelId.set(subscribe_channel_id, db)
            layouts?.also {
                Layouts.overwriteItems(it, db)
            }
            SubscriberCount.set(subscriber_count, db)
        }}
    }

    companion object {
        fun createForItem(item: MediaItem): ArtistData =
            ArtistData("", is_for_item = true)
    }
}
