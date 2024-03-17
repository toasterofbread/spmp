package com.toasterofbread.spmp.model.mediaitem

import LocalPlayerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.AltSetterProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.fromSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.db.observeAsState
import com.toasterofbread.spmp.model.mediaitem.db.toSQLBoolean
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.ThumbnailProviderImpl
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val DEFAULT_CONNECT_TIMEOUT: Int = 10000
val MEDIA_ITEM_RELATED_CONTENT_ICON: ImageVector get() = Icons.Default.GridView

interface MediaItem: MediaItemHolder, YtmMediaItem {
    override val id: String
    override val item: MediaItem get() = this

    override fun toString(): String
    fun getHolder(): MediaItemHolder = this
    fun getType(): MediaItemType
    fun getURL(context: AppContext): String
    fun getReference(): MediaItemRef

    @Composable
    fun observeActiveTitle(): MutableState<String?> {
        val player: PlayerState = LocalPlayerState.current
        return player.database.mediaItemQueries.activeTitleById(id)
            .observeAsState(
                key = id,
                mapValue = {
                    it.executeAsOneOrNull()?.IFNULL?.let { formatActiveTitle(it) }
                }
            ) { title ->
                setActiveTitle(title, player.context)
            }
    }

    suspend fun setActiveTitle(value: String?, context: AppContext) = withContext(Dispatchers.IO) {
        CustomTitle.set(value, context.database)
    }

    fun getActiveTitle(db: Database): String? {
        return getItemActiveTitle(db)
    }

    fun createDbEntry(db: Database)
    fun getEmptyData(): MediaItemData
    fun populateData(data: MediaItemData, db: Database) {
        data.loaded = Loaded.get(db)
        data.name = Title.get(db)
        data.custom_name = CustomTitle.get(db)
        data.description = Description.get(db)
        data.thumbnail_provider = ThumbnailProvider.get(db)
    }

    suspend fun loadData(
        context: AppContext,
        populate_data: Boolean = true,
        force: Boolean = false,
        save: Boolean = true
    ): Result<MediaItemData> {
        val data: MediaItemData = getEmptyData()
        if (!force && Loaded.get(context.database)) {
            if (populate_data) {
                populateData(data, context.database)
            }
            return Result.success(data)
        }
        return MediaItemLoader.loadUnknown(data, context, save = save)
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

    val property_rememberer: PropertyRememberer

    val Loaded: Property<Boolean>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Loaded", { mediaItemQueries.loadedById(id) }, { loaded.fromSQLBoolean() }, { mediaItemQueries.updateLoadedById(it.toSQLBoolean(), id) }, { false }
    )
    val Title: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Title", { mediaItemQueries.titleById(id) }, { title }, { mediaItemQueries.updateTitleById(it, id) }
    )
    val CustomTitle: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "CustomTitle", { mediaItemQueries.customTitleById(id) }, { custom_title }, { mediaItemQueries.updateCustomTitleById(it, id) }
    )
    val Description: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Description", { mediaItemQueries.descriptionById(id) }, { description }, { mediaItemQueries.updateDescriptionById(it, id) }
    )
    val ThumbnailProvider: Property<ThumbnailProvider?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "ThumbnailProvider",
        { mediaItemQueries.thumbnailProviderById(id) },
        { this.toThumbnailProvider() },
        {
            require(it is ThumbnailProviderImpl?)
            mediaItemQueries.updateThumbnailProviderById(it?.url_a, it?.url_b, id)
        }
    )

    val ThemeColour: Property<Color?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "ThemeColour", { mediaItemQueries.themeColourById(id) }, { theme_colour?.let { Color(it) } }, { mediaItemQueries.updateThemeColourById(it?.toArgb()?.toLong(), id) }
    )
    val Hidden: Property<Boolean>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Hidden", { mediaItemQueries.isHiddenById(id) }, { hidden.fromSQLBoolean() }, { mediaItemQueries.updateIsHiddenById(it.toSQLBoolean(), id) }, { false }
    )

    interface WithArtist: MediaItem {
        val Artist: AltSetterProperty<ArtistRef?, Artist?>

        override fun populateData(data: MediaItemData, db: Database) {
            require(data is DataWithArtist)

            super.populateData(data, db)

            data.artist = Artist.get(db)
        }
    }

    abstract class DataWithArtist: MediaItemData(), WithArtist {
        abstract var artist: Artist?

        override fun getDataValues(): Map<String, Any?> =
            super.getDataValues() + mapOf(
                "artist" to artist
            )

        override fun saveToDatabase(db: Database, apply_to_item: MediaItem, uncertain: Boolean, subitems_uncertain: Boolean) {
            db.transaction { with(apply_to_item as WithArtist) {
                super.saveToDatabase(db, apply_to_item, uncertain, subitems_uncertain)

                val artist_data: Artist? = artist
                if (artist_data is ArtistData) {
                    artist_data.saveToDatabase(db, uncertain = subitems_uncertain)
                }
                else if (artist_data != null) {
                    artist_data.createDbEntry(db)
                }

                Artist.setNotNullAlt(artist, db, uncertain)
            }}
        }
    }
}

abstract class MediaItemRef: MediaItem {
    override val name: String? = null
    override val description: String? = null
    override val thumbnail_provider: ThumbnailProvider? = null

    override fun equals(other: Any?): Boolean {
        if (other is MediaItemRef && other.getType() == getType()) {
            return id == other.id
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

fun YtmMediaItem.toMediaItemRef(): MediaItem =
    when (this) {
        is YtmSong -> SongRef(id)
        is YtmPlaylist -> RemotePlaylistRef(id)
        is YtmArtist -> ArtistRef(id)
        is MediaItem -> this
        else -> throw NotImplementedError(this::class.toString())
    }

fun YtmMediaItem.getItemActiveTitle(db: Database): String? {
    return db.mediaItemQueries.activeTitleById(id).executeAsOneOrNull()?.IFNULL?.let { formatActiveTitle(it) }
}

private fun formatActiveTitle(active_title: String): String {
    Platform.DESKTOP.only {
        return active_title.replace('ㅤ', '\u200b')
    }
    return active_title
}