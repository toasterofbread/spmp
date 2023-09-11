package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer

class ArtistData(
    override var id: String,
    var subscribe_channel_id: String? = null,
    var layouts: MutableList<ArtistLayoutData>? = null,
    var subscriber_count: Int? = null,

    var subscribed: Boolean? = null
): MediaItemData(), Artist {
    override fun getDataValues(): Map<String, Any?> =
        super.getDataValues() + mapOf(
            "subscribe_channel_id" to subscribe_channel_id,
            "layouts" to layouts,
            "subscriber_count" to subscriber_count
        )

    override fun toString(): String = "ArtistData($id)"

    override val property_rememberer: PropertyRememberer = PropertyRememberer()

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem, uncertain: Boolean) {
        db.transaction { with(apply_to_item as Artist) {
            super.saveToDatabase(db, apply_to_item, uncertain)

            layouts?.also { layouts ->
                if (uncertain && Layouts.get(db).isNullOrEmpty() && Loaded.get(db)) {
                    return@also
                }

                Layouts.overwriteItems(layouts, db)
                for (layout in layouts) {
                    layout.saveToDatabase(db)
                }
            }

            SubscribeChannelId.setNotNull(subscribe_channel_id, db, uncertain)
            SubscriberCount.setNotNull(subscriber_count, db, uncertain)
        }}
    }
}
