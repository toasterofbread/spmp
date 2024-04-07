package com.toasterofbread.spmp.model.mediaitem.artist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.deserialise
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.SingleProperty
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.layout.ContinuableMediaItemLayout
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.YoutubePageType
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.toMediaItemData
import com.toasterofbread.spmp.model.serialise
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtistLayout
import dev.toastbits.ytmkt.uistrings.UiString

data class ArtistLayoutData(
    override var layout_index: Long?,
    override val artist_id: String,

    var items: List<MediaItemData>? = null,
    var title: UiString? = null,
    var subtitle: UiString? = null,
    var type: ItemLayoutType? = null,
    var view_more: YoutubePage? = null,
    var playlist: RemotePlaylist? = null
): ArtistLayout {
    fun saveToDatabase(db: Database, uncertain: Boolean = false, subitems_uncertain: Boolean = uncertain) {
        db.transaction {
            items?.also { items ->
                Items.clearItems(db, 0)
                for (item in items) {
                    if (item is MediaItem.DataWithArtists) {
                        item.artists = item.artists?.map { it.getReference() }
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

    fun loadIntoYtmLayout(db: Database): YtmArtistLayout =
        YtmArtistLayout(
            items = Items.get(db)?.map {
                val data: MediaItemData = it.getEmptyData()
                it.populateData(data, db)
                return@map data
            },
            title = Title.get(db),
            subtitle = Subtitle.get(db),
            type = Type.get(db),
            view_more = ViewMore.get(db),
            playlist_id = Playlist.get(db)?.id
        )

    @Composable
    fun rememberMediaItemLayout(db: Database): ContinuableMediaItemLayout {
        val items: List<MediaItem>? by Items.observe(db)
        var item_data: List<MediaItemData>? = remember(items) { items?.map { it.getEmptyData() } }

        val title: UiString? by Title.observe(db)
        val subtitle: UiString? by Subtitle.observe(db)
        val type: ItemLayoutType? by Type.observe(db)
        val view_more: YoutubePage? by ViewMore.observe(db)
        val playlist: RemotePlaylist? by Playlist.observe(db)

        val layout: AppMediaItemLayout =
            remember(item_data, title, subtitle, type, view_more) {
                AppMediaItemLayout(
                    item_data ?: emptyList(),
                    title,
                    subtitle,
                    type,
                    view_more,
                )
            }

        return remember(layout, playlist) {
            ContinuableMediaItemLayout(
                layout,
                playlist?.id?.let {
                    ContinuableMediaItemLayout.Continuation(it)
                }
            )
        }
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

    val Title: Property<UiString?>
        get() = SingleProperty(
        { artistLayoutQueries.titleByIndex(artist_id, layout_index!!) },
        { title_data?.let { UiString.deserialise(it) } },
        { artistLayoutQueries.updateTitleByIndex(it?.serialise(), artist_id, layout_index!!) }
    )
    val Subtitle: Property<UiString?>
        get() = SingleProperty(
        { artistLayoutQueries.subtitleByIndex(artist_id, layout_index!!) },
        { subtitle_data?.let { UiString.deserialise(it) } },
        { artistLayoutQueries.updateSubtitleByIndex(it?.serialise(), artist_id, layout_index!!) }
    )
    val Type: Property<ItemLayoutType?>
        get() = SingleProperty(
        { artistLayoutQueries.typeByIndex(artist_id, layout_index!!) },
        { type?.let { ItemLayoutType.entries[it.toInt()] } },
        { artistLayoutQueries.updateTypeByIndex(it?.ordinal?.toLong(), artist_id, layout_index!!) }
    )
    val ViewMore: Property<YoutubePage?>
        get() = SingleProperty(
        { artistLayoutQueries.viewMoreByIndex(artist_id, layout_index!!) },
        { view_more_type?.let { YoutubePageType.entries[it.toInt()].getPage(view_more_data!!) } },
        { view_more ->
            val serialised = view_more?.let { YoutubePageType.fromPage(view_more) }
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

fun YtmArtistLayout.toArtistLayoutData(artist_id: String): ArtistLayoutData =
    ArtistLayoutData(
        artist_id = artist_id,
        layout_index = null,
        items = items?.map { it.toMediaItemData() },
        title = title,
        subtitle = subtitle,
        type = type,
        view_more = view_more,
        playlist = playlist_id?.let { RemotePlaylistRef(it) }
    )
