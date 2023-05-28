package com.spectre7.spmp.model

import androidx.compose.runtime.*
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare

abstract class PlaylistItemData(override val data_item: Playlist): MediaItemWithLayoutsData(data_item) {
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
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(year))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 4)
        data.removeLast()?.also { supplyYear(it as Int, cached = true) }
        return super.supplyFromSerialisedData(data, klaxon)
    }
}

class PlaylistDataRegistryEntry: MediaItemDataRegistry.Entry() {
    var playlist_page_thumb_width: Float? by mutableStateOf(null)
    var image_item_uid: String? by mutableStateOf(null)
}

abstract class Playlist protected constructor (id: String): MediaItemWithLayouts(id) {
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

    open val layout: MediaItemLayout? get() = feed_layouts?.single()

    abstract val is_editable: Boolean?
    abstract val playlist_type: PlaylistType?
    abstract val total_duration: Long?
    abstract val item_count: Int?
    abstract val year: Int?

    val playlist_reg_entry: PlaylistDataRegistryEntry = registry_entry as PlaylistDataRegistryEntry
    override fun getDefaultRegistryEntry(): PlaylistDataRegistryEntry = PlaylistDataRegistryEntry()

    open fun getItems(): List<MediaItem>? = layout?.items

    open suspend fun addItem(item: MediaItem, index: Int = -1): Result<Unit> {
        check(is_editable == true)
        try {
            val items = layout?.items!!
            items.add(
                if (index == -1) items.size else index,
                item
            )
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }

        return saveItems()
    }

    open suspend fun removeItem(index: Int): Result<Unit> {
        check(is_editable == true)
        try {
            layout?.items!!.removeAt(index)
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }

        return saveItems()
    }

    open suspend fun moveItem(from: Int, to: Int): Result<Unit> {
        check(is_editable == true)
        try {
            layout?.items!!.add(to, layout?.items!!.removeAt(from))
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }

        return saveItems()
    }

    protected open suspend fun onDeleted() {
        if (pinned_to_home) {
            setPinnedToHome(false)
        }
    }

    open suspend fun deletePlaylist(): Result<Unit> {
        check(is_editable == true)
        TODO()
        onDeleted()
    }

    open suspend fun saveItems(): Result<Unit> {
        check(is_editable == true)
        TODO()
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
