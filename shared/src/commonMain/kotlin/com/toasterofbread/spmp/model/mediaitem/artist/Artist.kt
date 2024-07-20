package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemRef
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.fromNullableSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.db.toNullableSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.AppContext

class ArtistRef(override val id: String): Artist, MediaItemRef() {
    override fun toString(): String = "ArtistRef($id)"
    override val property_rememberer: PropertyRememberer = PropertyRememberer()
    override fun getReference(): ArtistRef = this
}

sealed interface Artist: MediaItem {
    override fun getType(): MediaItemType = MediaItemType.ARTIST
    override suspend fun getUrl(context: AppContext): String = "https://music.youtube.com/channel/$id"
    override fun getReference(): ArtistRef

    val SubscribeChannelId: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "SubscribeChannelId", { artistQueries.subscribeChannelIdById(id) }, { subscribe_channel_id }, { artistQueries.updateSubscribeChannelIdById(it, id) }
        )
    val ShufflePlaylistId: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "ShufflePlaylistId", { artistQueries.shufflePlaylistIdById(id) }, { shuffle_playlist_id }, { artistQueries.updateShufflePlaylistIdById(it, id) }
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
            prerequisite = Loaded
        )
    val SubscriberCount: Property<Int?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "SubscriberCount", { artistQueries.subscriberCountById(id) }, { subscriber_count?.toInt() }, { artistQueries.updateSubscriberCountById(it?.toLong(), id) }
        )

    val Subscribed: Property<Boolean?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Subscribed",
            { artistQueries.subscribedById(id) },
            { subscribed.fromNullableSQLBoolean() },
            { artistQueries.updateSubscribedById(it.toNullableSQLBoolean(), id) }
        )

    override fun createDbEntry(db: Database) {
        db.artistQueries.insertById(id)
    }
    override fun getEmptyData(): ArtistData = ArtistData(id)
    override fun populateData(data: MediaItemData, db: Database) {
        require(data is ArtistData)

        super.populateData(data, db)

        data.subscribe_channel_id = SubscribeChannelId.get(db)
        data.shuffle_playlist_id = ShufflePlaylistId.get(db)

        data.layouts =
            Layouts.get(db).orEmpty().map { layout ->
                ArtistLayoutData(layout.layout_index, layout.artist_id)
            }.toMutableList()
        data.subscriber_count = SubscriberCount.get(db)
    }
    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean, save: Boolean): Result<ArtistData> {
        return super.loadData(context, populate_data, force, save) as Result<ArtistData>
    }

    fun isForItem(): Boolean =
        id.startsWith("FORITEM")

    companion object {
        fun getForItemId(item: MediaItem): String {
            return "FORITEM${item.id}"
        }
    }
}
