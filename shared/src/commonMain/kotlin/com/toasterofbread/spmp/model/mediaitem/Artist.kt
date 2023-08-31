package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayoutData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayoutRef
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.fromNullableSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.db.fromSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.db.toNullableSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.db.toSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.PlatformContext

class ArtistRef(override val id: String): Artist {
    override fun toString(): String = "ArtistRef($id)"

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
//    init {
//        lazyAssert { id.isNotBlank() || IsForItem.get(SpMp.context.database) }
//    }
}

sealed interface Artist: MediaItem {
    override fun getType(): MediaItemType = MediaItemType.ARTIST
    override fun getURL(context: PlatformContext): String = "https://music.youtube.com/channel/$id"

    override fun createDbEntry(db: Database) {
        db.artistQueries.insertById(id)
    }
    override fun getEmptyData(): ArtistData = ArtistData(id)
    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)
        (data as ArtistData).apply {
            subscribe_channel_id = SubscribeChannelId.get(db)
            layouts = mutableListOf<ArtistLayoutData>().apply {
                for (layout in Layouts.get(db).orEmpty()) {
                    add(ArtistLayoutData(layout.layout_index, layout.artist_id))
                }
            }
            subscriber_count = SubscriberCount.get(db)
            is_for_item = IsForItem.get(db)
        }
    }

    override suspend fun loadData(context: PlatformContext, populate_data: Boolean): Result<ArtistData> {
        return super.loadData(context, populate_data) as Result<ArtistData>
    }

    val SubscribeChannelId: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "SubscribeChannelId", { artistQueries.subscribeChannelIdById(id) }, { subscribe_channel_id }, { artistQueries.updateSubscribeChannelIdById(it, id) }
    )
    val Layouts: ListPropertyImpl<ArtistLayout, Long>
        get() = property_rememberer.rememberListQueryProperty(
        "Layouts",
        getValue = {
            this.map { layout_index ->
                ArtistLayoutRef(layout_index, id)
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
        prerequisite = null
    )

    val SubscriberCount: Property<Int?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "SubscriberCount", { artistQueries.subscriberCountById(id) }, { subscriber_count?.toInt() }, { artistQueries.updateSubscriberCountById(it?.toLong(), id) }
    )
    val IsForItem: Property<Boolean>
        get() = property_rememberer.rememberSingleQueryProperty(
        "IsForItem",
        { artistQueries.isForItemById(id) },
        { is_for_item.fromSQLBoolean() },
        { artistQueries.updateIsForItemById(it.toSQLBoolean(), id) },
        { false }
    )

    // User properties

    val Subscribed: Property<Boolean?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Subscribed",
        { artistQueries.subscribedById(id) },
        { subscribed.fromNullableSQLBoolean() },
        { artistQueries.updateSubscribedById(it.toNullableSQLBoolean(), id) }
    )
}

class ArtistData(
    override var id: String,
    var subscribe_channel_id: String? = null,
    var layouts: MutableList<ArtistLayoutData>? = null,
    var subscriber_count: Int? = null,
    var is_for_item: Boolean = false,

    var subscribed: Boolean? = null
): MediaItemData(), Artist {
    override fun toString(): String = "ArtistData($id)"

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
        db.transaction { with(apply_to_item as Artist) {
            super.saveToDatabase(db, apply_to_item)

            layouts?.also { layouts ->
                Layouts.overwriteItems(layouts, db)
                for (layout in layouts) {
                    layout.saveToDatabase(db)
                }
            }

            SubscribeChannelId.setNotNull(subscribe_channel_id, db)
            SubscriberCount.setNotNull(subscriber_count, db)
            IsForItem.setNotNull(is_for_item, db)
        }}
    }

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
//    init {
//        lazyAssert { is_for_item || id.isNotBlank() || IsForItem.get(SpMp.context.database) }
//    }

    companion object {
        fun createForItem(item: MediaItem): ArtistData =
            ArtistData("", is_for_item = true)
    }
}
