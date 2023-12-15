package com.toasterofbread.spmp.model.mediaitem.artist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.SingleProperty
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMoreType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString

data class ArtistLayoutData(
    override var layout_index: Long?,
    override val artist_id: String,

    var items: List<MediaItemData>? = null,
    var title: LocalisedString? = null,
    var subtitle: LocalisedString? = null,
    var type: MediaItemLayout.Type? = null,
    var view_more: ViewMore? = null,
    var playlist: RemotePlaylist? = null
): ArtistLayout {
    fun saveToDatabase(db: Database, uncertain: Boolean = false, subitems_uncertain: Boolean = uncertain) {
        db.transaction {
            items?.also { items ->
                Items.clearItems(db, 0)
                for (item in items) {
                    if (item is MediaItem.DataWithArtist) {
                        item.artist = item.artist?.getReference()
                    }
                    item.saveToDatabase(db, uncertain = subitems_uncertain)
                    Items.addItem(item, null, db)
                }
            }

            Title.setNotNull(title, db, uncertain)
            Subtitle.setNotNull(subtitle, db, uncertain)
            Type.setNotNull(type, db, uncertain)
            ViewMore.setNotNull(view_more, db, uncertain)
            Playlist.setNotNull(playlist, db, uncertain)
        }
    }
}

data class ArtistLayoutRef(override var layout_index: Long?, override val artist_id: String): ArtistLayout

sealed interface ArtistLayout {
    var layout_index: Long?
    val artist_id: String

    @Composable
    fun rememberMediaItemLayout(db: Database): MediaItemLayout {
        val items: List<MediaItem>? by Items.observe(db)
        val title: LocalisedString? by Title.observe(db)
        val subtitle: LocalisedString? by Subtitle.observe(db)
        val type: MediaItemLayout.Type? by Type.observe(db)
        val view_more: ViewMore? by ViewMore.observe(db)
        val playlist: RemotePlaylist? by Playlist.observe(db)

        return MediaItemLayout(
            items ?: emptyList(),
            title,
            subtitle,
            type,
            view_more,
            playlist?.id?.let { playlist_id ->
                MediaItemLayout.Continuation(playlist_id, MediaItemLayout.Continuation.Type.PLAYLIST)
            }
        )
    }

    val Items get() = ListPropertyImpl(
        getValue = {
            this.map { item ->
                MediaItemType.entries[item.item_type.toInt()].referenceFromId(item.item_id)
            }
        },
        getQuery = { artistLayoutItemQueries.byLayoutIndex(artist_id, layout_index!!) },
        getSize = { artistLayoutItemQueries.itemCount(artist_id, layout_index!!).executeAsOne() },
        addItem = { item, index ->
            artistLayoutItemQueries.insertItemAtIndex(artist_id, layout_index!!, item.id, item.getType().ordinal.toLong(), index)
        },
        removeItem = { index ->
            artistLayoutItemQueries.removeItemAtIndex(artist_id, layout_index!!, index)
        },
        setItemIndex = { from, to ->
            artistLayoutItemQueries.updateItemIndex(from = from, to = to, artist_id = artist_id, layout_index = layout_index!!)
        },
        clearItems = { from_index ->
            artistLayoutItemQueries.clearItems(artist_id, layout_index!!, from_index)
        }
    )

    val Title: Property<LocalisedString?>
        get() = SingleProperty(
        { artistLayoutQueries.titleByIndex(artist_id, layout_index!!) },
        { title_data?.let { LocalisedString.deserialise(it) } },
        { artistLayoutQueries.updateTitleByIndex(it?.serialise(), artist_id, layout_index!!) }
    )
    val Subtitle: Property<LocalisedString?>
        get() = SingleProperty(
        { artistLayoutQueries.subtitleByIndex(artist_id, layout_index!!) },
        { subtitle_data?.let { LocalisedString.deserialise(it) } },
        { artistLayoutQueries.updateSubtitleByIndex(it?.serialise(), artist_id, layout_index!!) }
    )
    val Type: Property<MediaItemLayout.Type?>
        get() = SingleProperty(
        { artistLayoutQueries.typeByIndex(artist_id, layout_index!!) },
        { type?.let { MediaItemLayout.Type.entries[it.toInt()] } },
        { artistLayoutQueries.updateTypeByIndex(it?.ordinal?.toLong(), artist_id, layout_index!!) }
    )
    val ViewMore: Property<ViewMore?>
        get() = SingleProperty(
        { artistLayoutQueries.viewMoreByIndex(artist_id, layout_index!!) },
        { view_more_type?.let { ViewMoreType.entries[it.toInt()].getViewMore(view_more_data!!) } },
        { view_more ->
            val serialised = view_more?.let { ViewMoreType.fromViewMore(view_more) }
            artistLayoutQueries.updateViewMoreByIndex(serialised?.first, serialised?.second, artist_id, layout_index!!)
        }
    )
    val Playlist: Property<RemotePlaylist?>
        get() = SingleProperty(
        { artistLayoutQueries.playlistIdByIndex(artist_id, layout_index!!) },
        { playlist_id?.let { RemotePlaylistRef(it) } },
        { artistLayoutQueries.updatePlaylistIdByIndex(it?.id, artist_id, layout_index!!) }
    )

    companion object {
        fun create(artist_id: String): ArtistLayoutData {
            return ArtistLayoutData(null, artist_id)
        }
    }
}
