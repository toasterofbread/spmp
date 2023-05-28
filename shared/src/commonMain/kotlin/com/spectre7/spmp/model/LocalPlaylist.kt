package com.spectre7.spmp.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.addUnique
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Reader

private fun getPlaylistsDirectory(context: PlatformContext): File =
    context.getFilesDir().resolve("localPlaylists")

private fun getPlaylistFileFromId(context: PlatformContext, id: String): File = getPlaylistsDirectory(context).resolve(id)

private class LocalPlaylistItemData(
    item: LocalPlaylist,
    private val playlist_items: MutableList<MediaItem>
): PlaylistItemData(item) {
    override var feed_layouts: List<MediaItemLayout>? = listOf(
        MediaItemLayout(
            null,
            null,
            MediaItemLayout.Type.LIST,
            playlist_items
        )
    )
        set(_) { throw IllegalStateException() }

    override fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean): MediaItemWithLayoutsData {
        if (value?.isNotEmpty() != true) {
            playlist_items.clear()
        }
        else {
            playlist_items.addAll(value.single().items)
        }
        return this
    }

    override fun saveData(data: String) {
        val file = getPlaylistFileFromId(SpMp.context, data_item.id)
        file.parentFile.mkdirs()
        file.writeText(data)
    }

    override fun getDataReader(): Reader? {
        val file = getPlaylistFileFromId(SpMp.context, data_item.id)
        if (!file.isFile) {
            return null
        }
        return file.reader()
    }
}

class LocalPlaylist(id: String): Playlist(id) {
    private val items: MutableList<MediaItem> = mutableStateListOf()
    override val data: MediaItemWithLayoutsData = LocalPlaylistItemData(this, items)

    override val is_editable: Boolean = true
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
    override val year: Int? get() = null // TODO

    override fun getItems(): List<MediaItem> = items

    override suspend fun addItem(item: MediaItem, index: Int): Result<Unit> {
        try {
            items.add(if (index == -1) items.size else index, item)
            data.onChanged()
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
        return Result.success(Unit)
    }

    override suspend fun removeItem(index: Int): Result<Unit> {
        try {
            items.removeAt(index)
            data.onChanged()
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
        return Result.success(Unit)
    }

    override suspend fun moveItem(from: Int, to: Int): Result<Unit> {
        try {
            items.add(to, items.removeAt(from))
            data.onChanged()
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
        return Result.success(Unit)
    }

    override suspend fun deletePlaylist(): Result<Unit> {
        deleteLocalPlaylist(SpMp.context, id)
        onDeleted()
        return Result.success(Unit)
    }

    override suspend fun saveItems(): Result<Unit> {
        data.save()
        return Result.success(Unit)
    }

    override fun isFullyLoaded(): Boolean = true
    override suspend fun loadData(force: Boolean): Result<MediaItem?> = Result.success(this)

    override val url: String? = null

    override fun canLoadThumbnail(): Boolean = true
    override fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> = Result.failure(NotImplementedError())

    override fun canGetThemeColour(): Boolean = true
    override fun getThemeColour(): Color = super.getThemeColour() ?: Theme.current.accent

    @Composable
    override fun Thumbnail(
        quality: MediaItemThumbnailProvider.Quality,
        modifier: Modifier,
        contentColourProvider: (() -> Color)?,
        onLoaded: ((ImageBitmap) -> Unit)?
    ) {
        var image_item: MediaItem? by remember { mutableStateOf(null) }
        LaunchedEffect(playlist_reg_entry.image_item_uid) {
            image_item = playlist_reg_entry.image_item_uid?.let { uid ->
                fromUid(uid)
            }
        }

        image_item?.also { item ->
            item.Thumbnail(quality, modifier, contentColourProvider, onLoaded)
            return
        }

        Box(modifier.background(Theme.current.accent), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.MusicNote,
                null,
                tint = Theme.current.on_accent
            )
        }
    }

    interface Listener {
        fun onAdded(playlist: LocalPlaylist)
        fun onRemoved(index: Int, playlist: LocalPlaylist)
    }

    companion object {
        private var local_playlists: MutableList<LocalPlaylist>? = null
        private val load_mutex = Mutex()
        private val playlists_listeners: MutableList<Listener> = mutableListOf()

        fun addPlaylistsListener(listener: Listener) {
            playlists_listeners.addUnique(listener)
        }
        fun removePlaylistsListener(listener: Listener) {
            playlists_listeners.remove(listener)
        }

        @Composable
        fun rememberLocalPlaylistsListener(): List<LocalPlaylist> {
            val playlists: MutableList<LocalPlaylist> = remember { mutableStateListOf() }

            LaunchedEffect(Unit) {
                playlists.addAll(getLocalPlaylists(SpMp.context).toMutableStateList())
            }
            DisposableEffect(Unit) {
                val listener = object : Listener {
                    override fun onAdded(playlist: LocalPlaylist) {
                        playlists.add(playlist)
                    }
                    override fun onRemoved(index: Int, playlist: LocalPlaylist) {
                        playlists.removeAt(index)
                    }
                }
                addPlaylistsListener(listener)
                onDispose {
                    removePlaylistsListener(listener)
                }
            }

            return playlists
        }

        suspend fun fromId(id: String, context: PlatformContext = SpMp.context): LocalPlaylist {
            getLocalPlaylists(context)
            load_mutex.withLock {
                return local_playlists!!.first { it.id == id }
            }
        }

        suspend fun getLocalPlaylists(context: PlatformContext): List<LocalPlaylist> {
            load_mutex.withLock {
                local_playlists?.also {
                    return it.toList()
                }

                return withContext(Dispatchers.IO) {
                    val dir = getPlaylistsDirectory(context)
                    if (dir.isDirectory) {
                        val files = dir.listFiles() ?: emptyArray()
                        local_playlists = files.map { file ->
                            LocalPlaylist(file.name).apply { loadFromCache() }
                        }.toMutableList()
                    }
                    else {
                        local_playlists = mutableListOf()
                    }

                    return@withContext local_playlists!!.toList()
                }
            }
        }

        suspend fun createLocalPlaylist(context: PlatformContext): LocalPlaylist = withContext(Dispatchers.IO) {
            getLocalPlaylists(context)

            load_mutex.withLock {
                var id: Int = 0
                for (playlist in local_playlists ?: emptyList()) {
                    id = maxOf(id, playlist.id.toInt() + 1)
                }

                val file = getPlaylistFileFromId(context, id.toString())
                check(!file.exists())

                val playlist = LocalPlaylist(id.toString())
                playlist.editData {
                    supplyTitle(getString("new_playlist_title"))
                }
                local_playlists!!.add(playlist)

                for (listener in playlists_listeners) {
                    listener.onAdded(playlist)
                }

                return@withContext playlist
            }
        }

        private suspend fun deleteLocalPlaylist(context: PlatformContext, id: String) = withContext(Dispatchers.IO) {
            getLocalPlaylists(context)

            load_mutex.withLock {
                val file = getPlaylistFileFromId(context, id)
                check(file.exists())

                val index = local_playlists!!.indexOfFirst { it.id == id }
                val playlist = local_playlists!!.removeAt(index)

                assert(file.delete())

                for (listener in playlists_listeners) {
                    listener.onRemoved(index, playlist)
                }
            }
        }
    }
}
