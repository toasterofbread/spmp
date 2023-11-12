package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemRef
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.composekit.utils.common.lazyAssert

class SongData(
    override var id: String,
    override var artist: Artist? = null,

    var song_type: SongType? = null,
    var duration: Long? = null,
    var album: RemotePlaylist? = null,
    var related_browse_id: String? = null,
    var lyrics_browse_id: String? = null
): MediaItem.DataWithArtist(), Song {
    override fun toString(): String = "SongData($id)"
    override fun getDataValues(): Map<String, Any?> =
        super.getDataValues() + mapOf(
            "song_type" to song_type,
            "duration" to duration,
            "album" to album,
            "related_browse_id" to related_browse_id,
            "lyrics_browse_id" to lyrics_browse_id
        )
    override fun getReference(): SongRef = SongRef(id)

    override val ThumbnailProvider: Property<MediaItemThumbnailProvider?>
        get() = super<Song>.ThumbnailProvider

    override val property_rememberer: PropertyRememberer = PropertyRememberer()

    init {
        lazyAssert { id.isNotBlank() }
    }

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem, uncertain: Boolean, subitems_uncertain: Boolean) {
        db.transaction { with(apply_to_item as Song) {
            super.saveToDatabase(db, apply_to_item, uncertain, subitems_uncertain)

            album?.also { album ->
                if (album is RemotePlaylistData) {
                    album.saveToDatabase(db, uncertain = uncertain)
                }
                else {
                    album.createDbEntry(db)
                }
            }

            TypeOfSong.setNotNull(song_type, db, uncertain)
            Duration.setNotNull(duration, db, uncertain)
            Album.setNotNull(album, db, uncertain)
            RelatedBrowseId.setNotNull(related_browse_id, db, uncertain)
            LyricsBrowseId.setNotNull(lyrics_browse_id, db, uncertain)
        }}
    }
}
