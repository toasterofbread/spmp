package com.spectre7.spmp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.VectorPainter
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.DEFAULT_CONNECT_TIMEOUT
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.crop
import com.spectre7.spmp.platform.toImageBitmap
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.utils.addUnique
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.net.URL

class LocalPlaylist(id: String, private val playlist_file: File): Playlist(id) {
    override val data: MediaItemWithLayoutsData = MediaItemWithLayoutsData(this)
    private val items: MutableList<MediaItem> = mutableStateListOf()

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
            items.add(index, item)
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
        return Result.success(Unit)
    }

    override suspend fun removeItem(index: Int): Result<Unit> {
        try {
            items.removeAt(index)
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
        return Result.success(Unit)
    }

    override suspend fun moveItem(from: Int, to: Int): Result<Unit> {
        try {
            items.add(to, items.removeAt(from))
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
        return Result.success(Unit)
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(year))
    }

    override fun isFullyLoaded(): Boolean = true
    override suspend fun loadData(force: Boolean): Result<MediaItem?> = Result.success(this)

    override val url: String? = null

    override fun canLoadThumbnail(): Boolean = true
    override fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> {
        try {
            val connection = URL("https://www.gstatic.com/youtube/media/ytm/images/pbg/attribute-radio-fallback-1@1000.png").openConnection()
            connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

            val stream = connection.getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            return Result.success(bytes.toImageBitmap())
        }
        catch (e: FileNotFoundException) {
            return Result.failure(e)
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

        private fun getPlaylistsDirectory(context: PlatformContext): File =
            context.getFilesDir().resolve("localPlaylists")

        private fun getPlaylistFileFromId(context: PlatformContext, id: String): File = getPlaylistsDirectory(context).resolve("$id.json")

        fun addPlaylistsListener(listener: Listener) {
            playlists_listeners.addUnique(listener)
        }
        fun removePlaylistsListener(listener: Listener) {
            playlists_listeners.remove(listener)
        }

        suspend fun getLocalPlaylists(context: PlatformContext): List<LocalPlaylist> {
            load_mutex.withLock {
                local_playlists?.also {
                    return it.toList()
                }

                return withContext(Dispatchers.IO) {
                    synchronized(this) {
                        val dir = getPlaylistsDirectory(context)
                        if (dir.isDirectory) {
                            for (file in dir.listFiles() ?: emptyArray()) {

                            }
                            TODO()
                        }
                        else {
                            local_playlists = mutableListOf()
                        }

                        return@withContext local_playlists!!.toList()
                    }
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

                val playlist = LocalPlaylist(id.toString(), file)
                playlist.editData {
                    supplyTitle(getStringTODO("New playlist"))
                }
                local_playlists!!.add(playlist)

                withContext(Dispatchers.Main) {
                    for (listener in playlists_listeners) {
                        listener.onAdded(playlist)
                    }
                }

                return@withContext playlist
            }
        }
    }
}
