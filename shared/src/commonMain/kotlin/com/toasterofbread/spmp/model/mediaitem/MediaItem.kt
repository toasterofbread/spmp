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
import com.toasterofbread.spmp.api.DEFAULT_CONNECT_TIMEOUT
import com.toasterofbread.spmp.api.model.TextRun
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType as PlaylistTypeEnum
import com.toasterofbread.spmp.platform.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

val MEDIA_ITEM_RELATED_CONTENT_ICON: ImageVector get() = Icons.Default.GridView

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

    suspend fun loadData(db: Database, populate_data: Boolean = true): Result<MediaItemData> {
        val data = getEmptyData()
        if (Loaded.get(db)) {
            populateData(data, db)
            return Result.success(data)
        }
        return MediaItemLoader.loadUnknown(getEmptyData(), db)
    }

    suspend fun downloadThumbnailData(url: String): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val connection = URL(url).openConnection()
            connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

            val stream = connection.getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            return@runCatching bytes.toImageBitmap()
        }
    }

    val Loaded: Property<Boolean> get() = singleProperty(
        { mediaItemQueries.loadedById(id) }, { loaded.fromSQLBoolean() }, { mediaItemQueries.updateLoadedById(it.toSQLBoolean(), id) }, { false }
    )
    val Title: Property<String?> get() = singleProperty(
        { mediaItemQueries.titleById(id) }, { title }, { mediaItemQueries.updateTitleById(it, id) }
    )
    val OriginalTitle: Property<String?> get() = singleProperty(
        { mediaItemQueries.originalTitleById(id) }, { original_title }, { mediaItemQueries.updateOriginalTitleById(it, id) }
    )
    val Description: Property<String?> get() = singleProperty(
        { mediaItemQueries.descriptionById(id) }, { description }, { mediaItemQueries.updateDescriptionById(it, id) }
    )
    val ThumbnailProvider: Property<MediaItemThumbnailProvider?> get() = singleProperty(
        { mediaItemQueries.thumbnailProviderById(id) },
        { this.toThumbnailProvider() },
        {
            require(it is MediaItemThumbnailProviderImpl?)
            mediaItemQueries.updateThumbnailProviderById(it?.url_a, it?.url_b, id)
        }
    )

    val ThemeColour: Property<Color?> get() = singleProperty(
        { mediaItemQueries.themeColourById(id) }, { theme_colour?.let { Color(it) } }, { mediaItemQueries.updateThemeColourById(it?.toArgb()?.toLong(), id) }
    )
    val PinnedToHome: Property<Boolean> get() = singleProperty(
        { mediaItemQueries.pinnedToHomeById(id) }, { pinned_to_home.fromSQLBoolean() }, { mediaItemQueries.updatePinnedToHomeById(it.toSQLBoolean(), id) }, { false }
    )
    val Hidden: Property<Boolean> get() = singleProperty(
        { mediaItemQueries.isHiddenById(id) }, { hidden.fromSQLBoolean() }, { mediaItemQueries.updateIsHiddenById(it.toSQLBoolean(), id) }, { false }
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
            if (this !is ArtistData && value == "MIMI") {
                TODO(this.toString())
            }
            field = value
        }

    var original_title: String? = null
    var description: String? = null
    var thumbnail_provider: MediaItemThumbnailProvider? = null

    companion object {
        fun fromBrowseEndpointType(page_type: String, id: String): MediaItemData {
            return when (page_type) {
                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" ->
                    PlaylistData(id).apply { playlist_type = PlaylistTypeEnum.fromTypeString(page_type) }
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" ->
                    ArtistData(id)
                else -> throw NotImplementedError(page_type)
            }
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
                            assert(playlist.playlist_type == PlaylistTypeEnum.ALBUM)
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
