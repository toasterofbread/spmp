package com.toasterofbread.spmp.model.mediaitem.db

import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import PlatformIO

suspend fun MediaItem.removeFromDatabase(db: Database) = withContext(Dispatchers.PlatformIO) {
    db.transaction {
        db.pinnedItemQueries.remove(id, getType().ordinal.toLong())

        when (getType()) {
            MediaItemType.SONG -> db.songQueries.removeById(id)
            MediaItemType.ARTIST -> db.artistQueries.removeById(id)
            MediaItemType.PLAYLIST_REM -> {
                db.songQueries.dereferenceAlbumById(id)
                db.playlistItemQueries.removeByPlaylistId(id)
                db.playlistQueries.removeById(id)
            }
            MediaItemType.PLAYLIST_LOC -> throw IllegalStateException("Local playlists are not stored in database")
        }
    }
}
