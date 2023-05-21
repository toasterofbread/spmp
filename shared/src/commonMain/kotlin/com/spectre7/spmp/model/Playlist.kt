package com.spectre7.spmp.model

import androidx.compose.runtime.*
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare

class PlaylistItemData(override val data_item: Playlist): MediaItemWithLayoutsData(data_item) {
    var playlist_type: Playlist.PlaylistType? by mutableStateOf(null)
        private set

    fun supplyPlaylistType(value: Playlist.PlaylistType?, certain: Boolean = false, cached: Boolean = false): Playlist {
        if (value != playlist_type && (playlist_type == null || certain)) {
            playlist_type = value
            onChanged(cached)
        }
        return data_item
    }

    var total_duration: Long? by mutableStateOf(null)
        private set

    fun supplyTotalDuration(value: Long?, certain: Boolean = false, cached: Boolean = false): Playlist {
        if (value != total_duration && (total_duration == null || certain)) {
            total_duration = value
            onChanged(cached)
        }
        return data_item
    }

    var item_count: Int? by mutableStateOf(null)
        private set

    fun supplyItemCount(value: Int?, certain: Boolean = false, cached: Boolean = false): Playlist {
        if (value != item_count && (item_count == null || certain)) {
            item_count = value
            onChanged(cached)
        }
        return data_item
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
}

class LocalPlaylist(id: String): Playlist(id) {
    val items: MutableList<MediaItem> = mutableStateListOf()

    override val feed_layouts: List<MediaItemLayout> = listOf(
        MediaItemLayout(null, null, items = items, view_more = MediaItemLayout.ViewMore(media_item = this))
    )

    override val playlist_type: PlaylistType = PlaylistType.PLAYLIST
    override val total_duration: Long? get() {
        var sum = 0L
        for (item in items) {
            if (item !is Song) {
                continue
            }
            if (item.duration == null) {
                return null
            }
            sum += item.duration!!
        }
        return sum
    }
    override val item_count: Int get() = items.size
    override val year: Int? get() = null

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(year))
    }
}

open class Playlist protected constructor (
    id: String
): MediaItemWithLayouts(id) {

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

    override val data: PlaylistItemData = PlaylistItemData(this)

    open val playlist_type: PlaylistType? get() = data.playlist_type
    open val total_duration: Long? get() = data.total_duration
    open val item_count: Int? get() = data.item_count
    open val year: Int? get() = data.year

    fun editPlaylistData(action: PlaylistItemData.() -> Unit): Playlist {
        editData {
            action(this as PlaylistItemData)
        }
        return this
    }

    fun editPlaylistDataManual(action: PlaylistItemData.() -> Unit): PlaylistItemData {
        action(data)
        return data
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(playlist_type?.ordinal), klaxon.toJsonString(total_duration), klaxon.toJsonString(item_count), klaxon.toJsonString(year))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 4)
        with(this@Playlist.data) {
            data.removeLast()?.also { supplyYear(it as Int, cached = true) }
            data.removeLast()?.also { supplyItemCount(it as Int, cached = true) }
            data.removeLast()?.also { supplyTotalDuration((it as Int).toLong(), cached = true) }
            data.removeLast()?.also { supplyPlaylistType(PlaylistType.values()[it as Int], cached = true) }
        }
        return super.supplyFromSerialisedData(data, klaxon)
    }

    companion object {
        private val playlists: MutableMap<String, Playlist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Playlist {
            return playlists.getOrPut(id) {
                val playlist = Playlist(id)
                playlist.loadFromCache()
                return@getOrPut playlist
            }.getOrReplacedWith() as Playlist
        }

        fun clearStoredItems(): Int {
            val amount = playlists.size
            playlists.clear()
            return amount
        }
    }

    @Composable
    override fun PreviewSquare(params: PreviewParams) {
        PlaylistPreviewSquare(this, params)
    }

    @Composable
    override fun PreviewLong(params: PreviewParams) {
        PlaylistPreviewLong(this, params)
    }

    override val url: String get() = "https://music.youtube.com/playlist?list=$id"
}

fun Playlist.PlaylistType?.getReadable(plural: Boolean): String {
    return getString(when (this) {
        Playlist.PlaylistType.PLAYLIST, null -> if (plural) "playlists" else "playlist"
        Playlist.PlaylistType.ALBUM -> if (plural) "albums" else "album"
        Playlist.PlaylistType.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        Playlist.PlaylistType.RADIO -> if (plural) "radios" else "radio"
    })
}
