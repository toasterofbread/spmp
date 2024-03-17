package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong

class ArtistData(
    override var id: String,
    var subscribe_channel_id: String? = null,
    var shuffle_playlist_id: String? = null,
    var layouts: MutableList<ArtistLayoutData>? = null,
    var subscriber_count: Int? = null,

    var subscribed: Boolean? = null
): MediaItemData(), Artist {
    override fun getDataValues(): Map<String, Any?> =
        super.getDataValues() + mapOf(
            "subscribe_channel_id" to subscribe_channel_id,
            "shuffle_playlist_id" to shuffle_playlist_id,
            "layouts" to layouts,
            "subscriber_count" to subscriber_count
        )
    override fun getReference(): ArtistRef = ArtistRef(id)

    override fun toString(): String = "ArtistData($id)"

    override val property_rememberer: PropertyRememberer = PropertyRememberer()

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem, uncertain: Boolean, subitems_uncertain: Boolean) {
        db.transaction { with(apply_to_item as Artist) {
            super.saveToDatabase(db, apply_to_item, uncertain, subitems_uncertain)

            layouts?.also { layouts ->
                if (uncertain && Layouts.get(db).isNullOrEmpty() && Loaded.get(db)) {
                    return@also
                }

                Layouts.overwriteItems(layouts, db)
                for (layout in layouts) {
                    layout.saveToDatabase(db, uncertain = subitems_uncertain)
                }
            }

            SubscribeChannelId.setNotNull(subscribe_channel_id, db, uncertain)
            ShufflePlaylistId.setNotNull(shuffle_playlist_id, db, uncertain)
            SubscriberCount.setNotNull(subscriber_count, db, uncertain)
        }}
    }
}

fun YtmArtist.toArtistData(): ArtistData =
    ArtistData(
        id = id,
        subscribe_channel_id = subscribe_channel_id,
        shuffle_playlist_id = shuffle_playlist_id,
        layouts = layouts?.map { it.toArtistLayoutData(id) }?.toMutableList(),
        subscriber_count = subscriber_count,
        subscribed = subscribed
    ).also { data ->
        data.name = name
        data.description = description
        data.thumbnail_provider = thumbnail_provider
    }