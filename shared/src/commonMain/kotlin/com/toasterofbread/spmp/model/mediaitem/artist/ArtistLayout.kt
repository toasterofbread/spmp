package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.ListProperty
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.Property
import com.toasterofbread.spmp.model.mediaitem.SingleProperty
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.toLocalisedYoutubeString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

data class ArtistLayoutData(
    var items: MutableList<MediaItem> = mutableListOf(),
    var title: LocalisedYoutubeString? = null,
    var subtitle: LocalisedYoutubeString? = null,
    var type: MediaItemLayout.Type? = null,
    var view_more: MediaItemLayout.ViewMore? = null,
    var playlist: Playlist? = null
) {
    fun saveToDatabase(layout: ArtistLayout, db: Database) {
        db.transaction {
            with(layout) {
                Items.overwriteItems(items, db)
                Title.set(title, db)
                Subtitle.set(subtitle, db)
                Type.set(type, db)
                ViewMore.set(view_more, db)
                Playlist.set(playlist, db)
            }
        }
    }
}

open class ArtistLayout internal constructor(val layout_index: Long, val artist_id: String) {
    val Items get() = ListProperty(
        getValue = {
            this.map { item ->
                MediaItemType.values()[item.item_type.toInt()].referenceFromId(item.item_id)
            }
        },
        getQuery = { artistLayoutItemQueries.byLayoutIndex(artist_id, layout_index) },
        getSize = { artistLayoutItemQueries.itemCount(artist_id, layout_index).executeAsOne() },
        addItem = { item, index ->
            artistLayoutItemQueries.insertItemAtIndex(artist_id, layout_index, item.id, item.getType().ordinal.toLong(), index)
        },
        removeItem = { index ->
            artistLayoutItemQueries.removeItemAtIndex(artist_id, layout_index, index)
        },
        setItemIndex = { from, to ->
            artistLayoutItemQueries.updateItemIndex(from = from, to = to, artist_id = artist_id, layout_index = layout_index)
        },
        clearItems = { from_index ->
            artistLayoutItemQueries.clearItems(artist_id, layout_index, from_index)
        }
    )

    val Title: Property<LocalisedYoutubeString?> get() = SingleProperty(
        { artistLayoutQueries.titleByIndex(artist_id, layout_index) },
        { title_type.toLocalisedYoutubeString(title_key) },
        { artistLayoutQueries.updateTitleByIndex(it?.type?.ordinal?.toLong(), it?.key, artist_id, layout_index) }
    )
    val Subtitle: Property<LocalisedYoutubeString?> get() = SingleProperty(
        { artistLayoutQueries.subtitleByIndex(artist_id, layout_index) },
        { subtitle_type.toLocalisedYoutubeString(subtitle_key) },
        { artistLayoutQueries.updateSubtitleByIndex(it?.type?.ordinal?.toLong(), it?.key, artist_id, layout_index) }
    )
    val Type: Property<MediaItemLayout.Type?> get() = SingleProperty(
        { artistLayoutQueries.typeByIndex(artist_id, layout_index) },
        { type?.let { MediaItemLayout.Type.values()[it.toInt()] } },
        { artistLayoutQueries.updateTypeByIndex(it?.ordinal?.toLong(), artist_id, layout_index) }
    )
    val ViewMore: Property<MediaItemLayout.ViewMore?> get() = SingleProperty(
        { artistLayoutQueries.viewMoreByIndex(artist_id, layout_index) },
        { view_more_type?.let { ViewMoreType.values()[it.toInt()].getViewMore(view_more_data!!) } },
        { view_more ->
            val serialised = view_more?.let { ViewMoreType.fromViewMore(view_more) }
            artistLayoutQueries.updateViewMoreByIndex(serialised?.first, serialised?.second, artist_id, layout_index)
        }
    )
    val Playlist: Property<Playlist?> get() = SingleProperty(
        { artistLayoutQueries.playlistIdByIndex(artist_id, layout_index) },
        { playlist_id?.let { AccountPlaylistRef(it) } },
        { artistLayoutQueries.updatePlaylistIdByIndex(it?.id, artist_id, layout_index) }
    )

    enum class ViewMoreType {
        MediaItem, ListPage;

        fun getViewMore(data: String): MediaItemLayout.ViewMore =
            when (this) {
                MediaItem -> MediaItemLayout.MediaItemViewMore(getMediaItemFromUid(data))
                ListPage -> {
                    val split = data.split(VIEW_MORE_SPLIT_CHAR, limit = 2)
                    MediaItemLayout.ListPageBrowseIdViewMore(split[0], split.getOrNull(1))
                }
            }

        companion object {
            private const val VIEW_MORE_SPLIT_CHAR = '|'
            fun fromViewMore(view_more: MediaItemLayout.ViewMore): Pair<Long, String> =
                when (view_more) {
                    is MediaItemLayout.MediaItemViewMore -> Pair(MediaItem.ordinal.toLong(), view_more.media_item.getUid())
                    is MediaItemLayout.ListPageBrowseIdViewMore -> Pair(ListPage.ordinal.toLong(), view_more.list_page_browse_id + VIEW_MORE_SPLIT_CHAR + (view_more.browse_params ?: ""))
                    is MediaItemLayout.LambdaViewMore -> throw NotImplementedError(view_more.toString())
                }
        }
    }

    companion object {
        fun create(artist_id: String, db: Database): ArtistLayout {
            return with(db) { db.transactionWithResult {
                val layout_index = artistLayoutQueries.layoutCount(artist_id).executeAsOne()
                artistLayoutQueries.insertLayoutAtIndex(artist_id, layout_index)

                return@transactionWithResult ArtistLayout(layout_index, artist_id)
            }}
        }
    }
}
