package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItemRef
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.UnsupportedPropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.context.PlatformFile

class LocalPlaylistRef(override val id: String): LocalPlaylist, MediaItemRef() {
    override fun toString(): String = "LocalPlaylistRef($id)"
    override fun getReference(): LocalPlaylistRef = this

    override fun createDbEntry(db: Database) {
        throw IllegalStateException(id)
    }

    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean, save: Boolean): Result<LocalPlaylistData> {
        return runCatching {
            val file: PlatformFile =
                MediaItemLibrary.getLocalPlaylistFile(this, context)
                ?: throw RuntimeException("Local file not available")
            PlaylistFileConverter.loadFromFile(file, context)!!
        }
    }

    override val property_rememberer: PropertyRememberer =
        UnsupportedPropertyRememberer { is_read ->
            if (is_read) "Local playlist must be loaded from file into a LocalPlaylistData."
            else "Use a PlaylistEditor instead."
        }
}
