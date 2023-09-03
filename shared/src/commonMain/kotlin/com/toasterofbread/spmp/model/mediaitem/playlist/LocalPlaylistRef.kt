package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.UnsupportedPropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.utils.lazyAssert

class LocalPlaylistRef(override val id: String): LocalPlaylist {
    override fun toString(): String = "LocalPlaylistRef($id)"

    override fun createDbEntry(db: Database) {
        throw IllegalStateException(id)
    }

    override suspend fun loadData(context: PlatformContext, populate_data: Boolean, force: Boolean): Result<LocalPlaylistData> {
        return runCatching {
            val file = MediaItemLibrary.getLocalPlaylistFile(this, context)
            PlaylistFileConverter.loadFromFile(file, context)!!
        }
    }

    override val property_rememberer: PropertyRememberer =
        UnsupportedPropertyRememberer { is_read ->
            if (is_read) "Local playlist must be loaded from file into a LocalPlaylistData."
            else "Use a PlaylistEditor instead."
        }
}
