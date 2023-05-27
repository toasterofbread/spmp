package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare

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

    open fun getItems(): List<MediaItem>? = layout?.items

    open suspend fun addItem(item: MediaItem, index: Int): Result<Unit> {
        check(is_editable == true)
        try {
            layout?.items!!.add(index, item)
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

    open suspend fun saveItems(): Result<Unit> {
        TODO()
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
