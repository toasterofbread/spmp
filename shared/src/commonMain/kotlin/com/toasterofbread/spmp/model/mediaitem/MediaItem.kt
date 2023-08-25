package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.SingleProperty
import com.toasterofbread.spmp.model.mediaitem.db.asMediaItemProperty
import com.toasterofbread.spmp.model.mediaitem.db.fromSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.db.toSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.spmp.youtubeapi.model.TextRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType as PlaylistTypeEnum

val MEDIA_ITEM_RELATED_CONTENT_ICON: ImageVector get() = Icons.Default.GridView

class PropertyRememberer {
    private val properties: MutableMap<String, Property<*>> = mutableMapOf()
    private val list_properties: MutableMap<String, ListProperty<*, *>> = mutableMapOf()

    fun <T, Q: Any> rememberSingleProperty(
        key: String,
        getQuery: Database.() -> Query<Q>,
        getValue: Q.() -> T,
        setValue: Database.(T) -> Unit,
        getDefault: () -> T = { null as T }
    ): Property<T> {
        return properties.getOrPut(key) {
            SingleProperty(getQuery, getValue, setValue, getDefault)
        } as Property<T>
    }

    fun <T, Q: Any> rememberListProperty(
        key: String,
        getQuery: Database.() -> Query<Q>,
        getValue: List<Q>.() -> List<T>,
        getSize: Database.() -> Long,
        addItem: Database.(item: T, index: Long) -> Unit,
        removeItem: Database.(index: Long) -> Unit,
        setItemIndex: Database.(from: Long, to: Long) -> Unit,
        clearItems: Database.(from_index: Long) -> Unit,
        prerequisite: Property<Boolean>? = null
    ): ListProperty<T, Q> {
        return list_properties.getOrPut(key) {
            ListProperty(getQuery, getValue, getSize, addItem, removeItem, setItemIndex, clearItems, prerequisite)
        } as ListProperty<T, Q>
    }
}

sealed interface MediaItem: MediaItemHolder {
    val id: String
    override val item: MediaItem get() = this

    override fun toString(): String
    fun getHolder(): MediaItemHolder = this
    fun getType(): MediaItemType
    fun getURL(): String

    fun createDbEntry(db: Database)
    fun getEmptyData(): MediaItemData
    fun populateData(data: MediaItemData, db: Database) {
        data.apply {
            loaded = Loaded.get(db)
            title = Title.get(db)
            original_title = OriginalTitle.get(db)
            description = Description.get(db)
            thumbnail_provider = ThumbnailProvider.get(db)
        }
    }

    suspend fun loadData(context: PlatformContext, populate_data: Boolean = true): Result<MediaItemData> {
        val data = getEmptyData()
        if (Loaded.get(context.database)) {
            populateData(data, context.database)
            return Result.success(data)
        }
        return MediaItemLoader.loadUnknown(getEmptyData(), context)
    }

    suspend fun downloadThumbnailData(url: String): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val connection = URL(url).openConnection()
            connection.connectTimeout = com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DEFAULT_CONNECT_TIMEOUT

            val stream = connection.getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            return@runCatching bytes.toImageBitmap()
        }
    }

    val property_rememberer: PropertyRememberer

    val Loaded: Property<Boolean>
        get() = property_rememberer.rememberSingleProperty(
        "Loaded", { mediaItemQueries.loadedById(id) }, { loaded.fromSQLBoolean() }, { mediaItemQueries.updateLoadedById(it.toSQLBoolean(), id) }, { false }
    )
    val Title: Property<String?>
        get() = property_rememberer.rememberSingleProperty(
        "Title", { mediaItemQueries.titleById(id) }, { title }, { mediaItemQueries.updateTitleById(it, id) }
    )
    val OriginalTitle: Property<String?>
        get() = property_rememberer.rememberSingleProperty(
        "OriginalTitle", { mediaItemQueries.originalTitleById(id) }, { original_title }, { mediaItemQueries.updateOriginalTitleById(it, id) }
    )
    val Description: Property<String?>
        get() = property_rememberer.rememberSingleProperty(
        "Description", { mediaItemQueries.descriptionById(id) }, { description }, { mediaItemQueries.updateDescriptionById(it, id) }
    )
    val ThumbnailProvider: Property<MediaItemThumbnailProvider?>
        get() = property_rememberer.rememberSingleProperty(
        "ThumbnailProvider",
        { mediaItemQueries.thumbnailProviderById(id) },
        { this.toThumbnailProvider() },
        {
            require(it is MediaItemThumbnailProviderImpl?)
            mediaItemQueries.updateThumbnailProviderById(it?.url_a, it?.url_b, id)
        }
    )

    val ThemeColour: Property<Color?>
        get() = property_rememberer.rememberSingleProperty(
        "ThemeColour", { mediaItemQueries.themeColourById(id) }, { theme_colour?.let { Color(it) } }, { mediaItemQueries.updateThemeColourById(it?.toArgb()?.toLong(), id) }
    )
    val PinnedToHome: Property<Boolean>
        get() = property_rememberer.rememberSingleProperty(
        "PinnedToHome", { mediaItemQueries.pinnedToHomeById(id) }, { pinned_to_home.fromSQLBoolean() }, { mediaItemQueries.updatePinnedToHomeById(it.toSQLBoolean(), id) }, { false }
    )
    val Hidden: Property<Boolean>
        get() = property_rememberer.rememberSingleProperty(
        "Hidden", { mediaItemQueries.isHiddenById(id) }, { hidden.fromSQLBoolean() }, { mediaItemQueries.updateIsHiddenById(it.toSQLBoolean(), id) }, { false }
    )

    interface WithArtist: MediaItem {
        val Artist: Property<Artist?>

        override fun populateData(data: MediaItemData, db: Database) {
            super.populateData(data, db)
            (data as DataWithArtist).apply {
                artist = Artist.get(db)
            }
        }
    }

    abstract class DataWithArtist: MediaItemData(), WithArtist {
        abstract var artist: Artist?

        override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
            db.transaction { with(apply_to_item as WithArtist) {
                super.saveToDatabase(db, apply_to_item)

                val artist_data = artist
                if (artist_data is ArtistData) {
                    artist_data.saveToDatabase(db)
                }

                Artist.setNotNull(artist, db)
            }}
        }
    }
}

sealed class MediaItemData: MediaItem {
    var loaded: Boolean = false
    var title: String? = null
        set(value) {
            field = value
            original_title = value
        }
    var original_title: String? = null
    var description: String? = null
    var thumbnail_provider: MediaItemThumbnailProvider? = null

    override val Loaded: Property<Boolean>
        get() = loaded.asMediaItemProperty(super.Loaded) { loaded = it }
    override val Title: Property<String?>
        get() = title.asMediaItemProperty(super.Title) { title = it }
    override val ThumbnailProvider: Property<MediaItemThumbnailProvider?>
        get() = thumbnail_provider.asMediaItemProperty(super.ThumbnailProvider) { thumbnail_provider = it }

    companion object {
        fun fromBrowseEndpointType(page_type: String, id: String): MediaItemData {
            val data = MediaItemType.fromBrowseEndpointType(page_type).referenceFromId(id).getEmptyData()
            if (data is PlaylistData) {
                data.playlist_type = PlaylistType.fromBrowseEndpointType(page_type)
            }
            return data
        }
    }

    open fun saveToDatabase(db: Database, apply_to_item: MediaItem = this) {
        db.transaction { with(apply_to_item) {
            createDbEntry(db)

            if (loaded) {
                Loaded.set(true, db)
            }
            Title.setNotNull(title, db)
            OriginalTitle.setNotNull(original_title, db)
            Description.setNotNull(description, db)
            ThumbnailProvider.setNotNull(thumbnail_provider, db)
        }}
    }

    open fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (MediaItemType.fromBrowseEndpointType(type)) {
                MediaItemType.ARTIST -> {
                    if (this is MediaItem.DataWithArtist) {
                        val item = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                        if (item is Artist) {
                            artist = item
                        }
                    }
                    artist_found = true
                }
                MediaItemType.PLAYLIST_ACC -> {
                    if (this is SongData) {
                        val playlist = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                        if (playlist is PlaylistData) {
                            assert(playlist.playlist_type == PlaylistTypeEnum.ALBUM, { "$playlist (${playlist.playlist_type}) | ${run.navigationEndpoint}" })
                            playlist.title = run.text
                            album = playlist
                        }
                    }
                }
                else -> {}
            }
        }

        if (!artist_found && this is MediaItem.DataWithArtist) {
            artist = ArtistData.createForItem(this).also {
                it.title = runs.getOrNull(1)?.text
            }
        }
    }
}

@Composable
fun <T, Q: Query<*>> Q.observeAsState(
    mapValue: (Q) -> T = { it as T },
    onExternalChange: (suspend (T) -> Unit)?
): MutableState<T> {
    val state = remember(this) { mutableStateOf(mapValue(this)) }
    var current_value by remember(state) { mutableStateOf(state.value) }

    DisposableEffect(state) {
        val listener = Query.Listener {
            current_value = mapValue(this@observeAsState)
            state.value = current_value
        }

        addListener(listener)
        onDispose {
            removeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (state.value != current_value) {
            current_value = state.value

            if (onExternalChange != null) {
                onExternalChange(current_value)
            }
            else {
                throw IllegalStateException("onExternalChange has not been defined ($this, ${state.value}, $current_value)")
            }
        }
    }

    return state
}
