package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.utils.lazyAssert

class RemotePlaylistData(id: String): PlaylistData(id), RemotePlaylist {
    var continuation: MediaItemLayout.Continuation? = null

    override fun toString(): String = "RemotePlaylistData($id, type=$playlist_type)"
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_REM

    override fun createDbEntry(db: Database) {
        if (playlist_type == PlaylistType.LOCAL) {
            throw IllegalStateException(id)
        }
        db.playlistQueries.insertById(id, playlist_type?.ordinal?.toLong())
    }
    override fun getEmptyData(): RemotePlaylistData = RemotePlaylistData(id)

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
        db.transaction { with(apply_to_item as RemotePlaylist) {
            super.saveToDatabase(db, apply_to_item)

            items?.also { items ->
                for (item in items) {
                    item.saveToDatabase(db)
                }
                Items.overwriteItems(items, db)
            }

            ItemCount.setNotNull(item_count, db)
            TypeOfPlaylist.setNotNull(playlist_type, db)
            TotalDuration.setNotNull(total_duration, db)
            Year.setNotNull(year, db)
            Owner.setNotNull(owner, db)
            Continuation.setNotNull(continuation, db)
        }}
    }

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
    init {
        lazyAssert { id.isNotBlank() }
    }
}
