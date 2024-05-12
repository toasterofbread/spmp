package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItemRef
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer

class RemotePlaylistRef(override val id: String): RemotePlaylist, MediaItemRef() {
    override fun toString(): String = "RemotePlaylistRef($id)"
    override fun getReference(): RemotePlaylistRef = this

    override fun getEmptyData(): RemotePlaylistData = RemotePlaylistData(id)
    override fun createDbEntry(db: Database) {
        db.playlistQueries.insertById(id, null)
    }

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
}
