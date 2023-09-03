package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.utils.lazyAssert

class RemotePlaylistData(id: String): PlaylistData(id), RemotePlaylist {
    var continuation: MediaItemLayout.Continuation? = null

    override fun toString(): String = "RemotePlaylistData($id, type=$playlist_type)"
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_REM

    override fun getDataValues(): Map<String, Any?> =
        super.getDataValues() + mapOf(
            "continuation" to continuation
        )

    override val property_rememberer: PropertyRememberer = PropertyRememberer()

    override fun createDbEntry(db: Database) {
        if (playlist_type == PlaylistType.LOCAL) {
            throw IllegalStateException(id)
        }
        db.playlistQueries.insertById(id, playlist_type?.ordinal?.toLong())
    }
    override fun getEmptyData(): RemotePlaylistData = RemotePlaylistData(id)

    override suspend fun savePlaylist(context: PlatformContext) {
        saveToDatabase(context.database)
    }

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem, uncertain: Boolean) {
        db.transaction { with(apply_to_item as RemotePlaylist) {
            super.saveToDatabase(db, apply_to_item, uncertain)

            items?.also { items ->
                for (item in items) {
                    item.saveToDatabase(db)
                }
                Items.overwriteItems(items, db)
            }

            ItemCount.setNotNull(item_count, db, uncertain)
            TypeOfPlaylist.setNotNull(playlist_type, db, uncertain)
            TotalDuration.setNotNull(total_duration, db, uncertain)
            Year.setNotNull(year, db, uncertain)
            Owner.setNotNull(owner, db, uncertain)
            Continuation.setNotNull(continuation, db, uncertain)
            CustomImageUrl.setNotNull(custom_image_url, db, uncertain)
            ImageWidth.setNotNull(image_width, db, uncertain)
            SortType.setNotNull(sort_type, db, uncertain)
        }}
    }
}
