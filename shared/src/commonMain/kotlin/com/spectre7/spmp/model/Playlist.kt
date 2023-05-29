package com.spectre7.spmp.model

import androidx.compose.runtime.*
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare

abstract class PlaylistItemData(override val data_item: Playlist): MediaItemData(data_item), MediaItemWithLayoutsData {
    open var items: MutableList<MediaItem>? by mutableStateOf(null)

    open fun supplyItems(value: List<MediaItem>?, certain: Boolean = false, cached: Boolean = false): Playlist {
        println("SUPPLYITEMS $value $certain $cached")
        if (items == null || certain) {
            items = value?.toMutableStateList()
            onChanged(cached)
        }
        return data_item
    }

    override fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean) {
        supplyItems(value?.single()?.items, certain, cached)
    }

    var year: Int? by mutableStateOf(null)
        private set

    fun supplyYear(value: Int?, certain: Boolean = false, cached: Boolean = false): Playlist {
        if (value != year && (year == null || certain)) {
            year = value
            onChanged(cached)
        }
        return data_item
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(year), klaxon.toJsonString(items))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 2)
        data.removeLast()?.also { supplyItems(klaxon.parseFromJsonArray(it as JsonArray<*>), true, cached = true) }
        data.removeLast()?.also { supplyYear(it as Int, cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}

class PlaylistDataRegistryEntry: MediaItemDataRegistry.Entry() {
    var playlist_page_thumb_width: Float? by mutableStateOf(null)
    var image_item_uid: String? by mutableStateOf(null)
}

abstract class Playlist protected constructor (id: String): MediaItem(id), MediaItemWithLayouts {
    enum class PlaylistType {
        PLAYLIST, ALBUM, AUDIOBOOK, RADIO;

        companion object {
            fun fromTypeString(type: String): PlaylistType {
                return when (type) {
                    "MUSIC_PAGE_TYPE_PLAYLIST" -> PLAYLIST
                    "MUSIC_PAGE_TYPE_ALBUM" -> ALBUM
                    "MUSIC_PAGE_TYPE_AUDIOBOOK" -> AUDIOBOOK
                    else -> throw NotImplementedError(type)
                }
            }
        }
    }

    abstract override val data: PlaylistItemData

    override fun isFullyLoaded(): Boolean = data.items != null && super.isFullyLoaded()

    open val items: MutableList<MediaItem>? get() = data.items
    val year: Int? get() = data.year

    @Composable
    fun getLayout(): MediaItemLayout? {
        return remember(data.items) {
            data.items?.let {
                MediaItemLayout(null, null, MediaItemLayout.Type.LIST, it)
            }
        }
    }

    abstract val is_editable: Boolean?
    abstract val playlist_type: PlaylistType?
    abstract val total_duration: Long?
    abstract val item_count: Int?

    override val feed_layouts: List<MediaItemLayout>?
        @Composable
        get() = getLayout()?.let { listOf(it) }
    override fun getFeedLayouts(): List<MediaItemLayout>? = data.items?.let { listOf(MediaItemLayout(null, null, MediaItemLayout.Type.LIST, it)) }

    val playlist_reg_entry: PlaylistDataRegistryEntry = registry_entry as PlaylistDataRegistryEntry
    override fun getDefaultRegistryEntry(): PlaylistDataRegistryEntry = PlaylistDataRegistryEntry()

    open fun addItem(item: MediaItem) {
        check(is_editable == true)
        data.items!!.add(item)
    }

    open fun removeItem(index: Int) {
        check(is_editable == true)
        data.items!!.removeAt(index)
    }

    open fun moveItem(from: Int, to: Int) {
        check(is_editable == true)
        data.items!!.add(to, data.items!!.removeAt(from))
    }

    abstract suspend fun deletePlaylist(): Result<Unit>
    abstract suspend fun saveItems(): Result<Unit>

    protected open suspend fun onDeleted() {
        if (pinned_to_home) {
            setPinnedToHome(false)
        }
    }

    @Composable
    override fun getThumbnailHolder(): MediaItem {
        var item: MediaItem by remember { mutableStateOf(this) }
        LaunchedEffect(playlist_reg_entry.image_item_uid) {
            val uid = playlist_reg_entry.image_item_uid
            item = if (uid != null) fromUid(uid)
                    else this@Playlist
        }
        return item
    }

    @Composable
    override fun PreviewSquare(params: PreviewParams) {
        PlaylistPreviewSquare(this, params)
    }

    @Composable
    override fun PreviewLong(params: PreviewParams) {
        PlaylistPreviewLong(this, params)
    }
}

fun Playlist.PlaylistType?.getReadable(plural: Boolean): String {
    return getString(when (this) {
        Playlist.PlaylistType.PLAYLIST, null -> if (plural) "playlists" else "playlist"
        Playlist.PlaylistType.ALBUM -> if (plural) "albums" else "album"
        Playlist.PlaylistType.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        Playlist.PlaylistType.RADIO -> if (plural) "radios" else "radio"
    })
}
