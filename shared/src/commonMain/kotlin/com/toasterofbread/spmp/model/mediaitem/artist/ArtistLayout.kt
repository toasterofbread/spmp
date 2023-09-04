package com.toasterofbread.spmp.model.mediaitem.artist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.SingleProperty
import com.toasterofbread.spmp.model.mediaitem.db.toLocalisedYoutubeString
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

data class ArtistLayoutData(
    override var layout_index: Long?,
    override val artist_id: String,

    var items: List<MediaItemData>? = null,
    var title: LocalisedYoutubeString? = null,
    var subtitle: LocalisedYoutubeString? = null,
    var type: MediaItemLayout.Type? = null,
    var view_more: MediaItemLayout.ViewMore? = null,
    var playlist: RemotePlaylist? = null
): ArtistLayout {
    fun saveToDatabase(db: Database) {
        db.transaction {
            items?.also { items ->
                Items.clearItems(db, 0)
                for (item in items) {
                    item.saveToDatabase(db)
                    Items.addItem(item, null, db)
                }
            }

            Title.setNotNull(title, db)
            Subtitle.setNotNull(subtitle, db)
            Type.setNotNull(type, db)
            ViewMore.setNotNull(view_more, db)
            Playlist.setNotNull(playlist, db)
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
        val title: LocalisedYoutubeString? by Title.observe(db)
        val subtitle: LocalisedYoutubeString? by Subtitle.observe(db)
        val type: MediaItemLayout.Type? by Type.observe(db)
        val view_more: MediaItemLayout.ViewMore? by ViewMore.observe(db)
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
                MediaItemType.values()[item.item_type.toInt()].referenceFromId(item.item_id)
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

    val Title: Property<LocalisedYoutubeString?>
        get() = SingleProperty(
        { artistLayoutQueries.titleByIndex(artist_id, layout_index!!) },
        { title_type.toLocalisedYoutubeString(title_key, title_lang) },
        { artistLayoutQueries.updateTitleByIndex(it?.type?.ordinal?.toLong(), it?.key, it?.source_language, artist_id, layout_index!!) }
    )
    val Subtitle: Property<LocalisedYoutubeString?>
        get() = SingleProperty(
        { artistLayoutQueries.subtitleByIndex(artist_id, layout_index!!) },
        { subtitle_type.toLocalisedYoutubeString(subtitle_key, subtitle_lang) },
        { artistLayoutQueries.updateSubtitleByIndex(it?.type?.ordinal?.toLong(), it?.key, it?.source_language, artist_id, layout_index!!) }
    )
    val Type: Property<MediaItemLayout.Type?>
        get() = SingleProperty(
        { artistLayoutQueries.typeByIndex(artist_id, layout_index!!) },
        { type?.let { MediaItemLayout.Type.values()[it.toInt()] } },
        { artistLayoutQueries.updateTypeByIndex(it?.ordinal?.toLong(), artist_id, layout_index!!) }
    )
    val ViewMore: Property<MediaItemLayout.ViewMore?>
        get() = SingleProperty(
        { artistLayoutQueries.viewMoreByIndex(artist_id, layout_index!!) },
        { view_more_type?.let { ViewMoreType.values()[it.toInt()].getViewMore(view_more_data!!) } },
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

    enum class ViewMoreType {
        MediaItem, ListPage, Plain;

        fun getViewMore(data: String): MediaItemLayout.ViewMore =
            when (this) {
                MediaItem -> {
                    val split = data.split(VIEW_MORE_SPLIT_CHAR, limit = 2)
                    MediaItemLayout.MediaItemViewMore(getMediaItemFromUid(split[0]), split.getOrNull(1))
                }
                ListPage -> {
                    val split = data.split(VIEW_MORE_SPLIT_CHAR, limit = 3)
                    MediaItemLayout.ListPageBrowseIdViewMore(split[0], split[1], split[2])
                }
                Plain -> {
                    MediaItemLayout.PlainViewMore(data)
                }
            }

        companion object {
            private const val VIEW_MORE_SPLIT_CHAR = '|'
            fun fromViewMore(view_more: MediaItemLayout.ViewMore): Pair<Long, String> =
                when (view_more) {
                    is MediaItemLayout.MediaItemViewMore ->
                        Pair(
                            MediaItem.ordinal.toLong(),
                            view_more.media_item.getUid() + VIEW_MORE_SPLIT_CHAR + (view_more.browse_params ?: "")
                        )
                    is MediaItemLayout.ListPageBrowseIdViewMore ->
                        Pair(
                            ListPage.ordinal.toLong(),
                            view_more.item_id + VIEW_MORE_SPLIT_CHAR + view_more.list_page_browse_id + VIEW_MORE_SPLIT_CHAR + (view_more.browse_params ?: "")
                        )
                    is MediaItemLayout.PlainViewMore ->
                        Pair(
                            Plain.ordinal.toLong(),
                            view_more.browse_id
                        )
                    is MediaItemLayout.LambdaViewMore -> throw NotImplementedError(view_more.toString())
                }
        }
    }

    companion object {
        fun create(artist_id: String): ArtistLayoutData {
            return ArtistLayoutData(null, artist_id)
        }
    }
}
