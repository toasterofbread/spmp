package com.spectre7.spmp.model.mediaitem.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.Api
import com.spectre7.spmp.api.TextRun
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.model.mediaitem.enums.MediaItemType
import com.spectre7.spmp.model.mediaitem.enums.PlaylistType
import com.spectre7.utils.ValueListeners
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.Reader
import kotlin.concurrent.thread

open class MediaItemData(open val data_item: MediaItem) {

    private var changes_made: Boolean = false
    fun onChanged(cached: Boolean = false) {
        if (!cached) {
            changes_made = true
        }
    }

    var original_title: String? by mutableStateOf(null)

    val title_listeners = ValueListeners<String?>()
    fun supplyTitle(value: String?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != original_title && (original_title == null || certain)) {
            original_title = value
            title_listeners.call(data_item.title)
            onChanged(cached)
        }
        return data_item
    }

    var description: String? by mutableStateOf(null)
        private set

    fun supplyDescription(value: String?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != description && (description == null || certain)) {
            description = value
            onChanged(cached)
        }
        return data_item
    }

    var artist: Artist? by mutableStateOf(null)
        private set

    val artist_listeners = ValueListeners<Artist?>()
    fun supplyArtist(value: Artist?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (data_item !is Artist && value != artist && (artist == null || certain)) {
            artist = value
            artist_listeners.call(artist)
            onChanged(cached)
        }
        return data_item
    }

    var thumbnail_provider: MediaItemThumbnailProvider? by mutableStateOf(null)
        private set

    fun supplyThumbnailProvider(value: MediaItemThumbnailProvider?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != thumbnail_provider && (thumbnail_provider == null || certain)) {
            thumbnail_provider = value
            onChanged(cached)
        }
        return data_item
    }

    open fun getSerialisedData(klaxon: Klaxon = Api.klaxon): List<String> {
        return listOf(
            klaxon.toJsonString(original_title),
            klaxon.toJsonString(artist?.id),
            klaxon.toJsonString(thumbnail_provider)
        )
    }

    open fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 3) { data }
        data[data.size - 3]?.also { supplyTitle(it as String, cached = true) }
        data[data.size - 2]?.also { supplyArtist(Artist.fromId(it as String), cached = true) }
        data[data.size - 1]?.also { supplyThumbnailProvider(MediaItemThumbnailProvider.fromJsonObject(it as JsonObject, klaxon), cached = true) }
        return this
    }

    open fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (MediaItemType.fromBrowseEndpointType(type)) {
                MediaItemType.ARTIST -> {
                    val artist = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                    if (artist != null) {
                        supplyArtist(artist as Artist, true)
                        artist_found = true
                    }
                }
                MediaItemType.PLAYLIST_ACC -> {
                    check(this is SongItemData)

                    val playlist = run.navigationEndpoint?.browseEndpoint?.getMediaItem() as Playlist?
                    if (playlist != null) {
                        check(playlist.playlist_type == PlaylistType.ALBUM)

                        playlist.editData {
                            supplyTitle(run.text, true)
                        }
                        supplyAlbum(playlist, true)
                    }
                }
                else -> {}
            }
        }

        if (!artist_found && data_item !is Artist) {
            val artist = Artist.createForItem(data_item)
            artist.editData {
                supplyTitle(runs[1].text)
            }
            supplyArtist(artist)
        }
    }

    fun load() {
        val reader = getDataReader() ?: return
        thread {
            val array = Api.klaxon.parseJsonArray(reader)
            reader.close()

            runBlocking {
                var retries = 5
                while (retries-- > 0) {
                    try {
                        data_item.supplyFromSerialisedData(array.toMutableList(), Api.klaxon)
                        break
                    } catch (e: IllegalStateException) {
                        delay(100)
                        if (retries == 0) {
                            throw e
                        }
                    }
                }
            }
        }
    }

    private fun getItemCacheKey(item: MediaItem): String {
        return "M/${item.type.name}/${item.id}"
    }

    protected open fun getDataReader(): Reader? = Cache.get(getItemCacheKey(data_item))

    fun save() {
        if (!changes_made) {
            return
        }
        saveData(Api.mediaitem_klaxon.toJsonString(data_item))
    }

    protected open fun saveData(data: String) {
        Cache.setString(
            getItemCacheKey(data_item),
            Api.mediaitem_klaxon.toJsonString(data_item),
            MediaItem.CACHE_LIFETIME
        )
    }
}
