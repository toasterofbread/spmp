package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import java.util.concurrent.locks.ReentrantLock

internal object MediaItemLoader: ListenerLoader<String, MediaItemData>() {
    private val song_lock = ReentrantLock()
    private val artist_lock = ReentrantLock()
    private val playlist_lock = ReentrantLock()

    private val loading_songs: MutableMap<String, LoadJob<Result<SongData>>> = mutableMapOf()
    private val loading_artists: MutableMap<String, LoadJob<Result<ArtistData>>> = mutableMapOf()
    private val loading_remote_playlists: MutableMap<String, LoadJob<Result<RemotePlaylistData>>> = mutableMapOf()
    private val loading_local_playlists: MutableMap<String, LoadJob<Result<LocalPlaylistData>>> = mutableMapOf()

    suspend fun <ItemType: MediaItemData> loadUnknown(item: ItemType, context: PlatformContext): Result<ItemType> =
        when (item) {
            is SongData -> loadSong(item, context) as Result<ItemType>
            is ArtistData -> loadArtist(item, context) as Result<ItemType>
            is RemotePlaylistData -> loadRemotePlaylist(item, context) as Result<ItemType>
            is LocalPlaylistData -> loadLocalPlaylist(item, context) as Result<ItemType>
            else -> throw NotImplementedError(item::class.toString())
        }

    suspend fun loadSong(song: SongData, context: PlatformContext): Result<SongData> {
        return loadItem(song, loading_songs, song_lock, context)
    }
    suspend fun loadArtist(artist: ArtistData, context: PlatformContext): Result<ArtistData> {
        return loadItem(artist, loading_artists, artist_lock, context)
    }
    suspend fun loadRemotePlaylist(playlist: RemotePlaylistData, context: PlatformContext, continuation: MediaItemLayout.Continuation? = null): Result<RemotePlaylistData> {
        return loadItem(playlist, loading_remote_playlists, playlist_lock, context, continuation)
    }
    suspend fun loadLocalPlaylist(playlist: LocalPlaylistData, context: PlatformContext): Result<LocalPlaylistData> {
        return loadItem(playlist, loading_local_playlists, playlist_lock, context)
    }

    fun isUnknownLoading(item: MediaItem): Boolean {
        val loading: Map<String, *> = when (item) {
            is Song -> loading_songs
            is Artist -> loading_artists
            is RemotePlaylist -> loading_remote_playlists
            is LocalPlaylist -> loading_local_playlists
            else -> throw NotImplementedError(item::class.toString())
        }

        return loading.contains(item.id)
    }

    override val listeners: MutableList<Listener<String, MediaItemData>> = mutableListOf()

    private suspend fun <ItemType: MediaItemData> loadItem(
        item: ItemType,
        loading_items: MutableMap<String, LoadJob<Result<ItemType>>>,
        lock: ReentrantLock,
        context: PlatformContext,
        continuation: MediaItemLayout.Continuation? = null
    ): Result<ItemType> {
        val result = performSafeLoad(
            item.id,
            lock,
            loading_items,
            listeners = listeners
        ) {
            val result: Result<ItemType> = when (item) {
                is SongData -> {
                    with(context.ytapi.LoadSong) {
                        if (!isImplemented()) {
                            return@performSafeLoad Result.failure(EndpointNotImplementedException(this))
                        }
                        loadSong(item) as Result<ItemType>
                    }
                }
                is ArtistData -> {
                    with(context.ytapi.LoadArtist) {
                        if (!isImplemented()) {
                            return@performSafeLoad Result.failure(EndpointNotImplementedException(this))
                        }
                        loadArtist(item) as Result<ItemType>
                    }
                }
                is RemotePlaylistData -> {
                    with(context.ytapi.LoadPlaylist) {
                        if (!isImplemented()) {
                            return@performSafeLoad Result.failure(EndpointNotImplementedException(this))
                        }
                        loadPlaylist(item, continuation) as Result<ItemType>
                    }
                }
                is LocalPlaylistData -> {
                    kotlin.runCatching {
                        val file = MediaItemLibrary.getLocalPlaylistFile(item, context)
                        PlaylistFileConverter.loadFromFile(file, context)
                    } as Result<ItemType>
                }
                else -> throw NotImplementedError(item::class.toString())
            }

            return@performSafeLoad result.fold(
                { Result.success(item) },
                { Result.failure(it) }
            )
        }
        return result
    }

}

@Composable
fun MediaItem.loadDataOnChange(
    context: PlatformContext,
    load: Boolean = true,
    force: Boolean = false,
    onLoadSucceeded: ((MediaItemData) -> Unit)? = null,
    onLoadFailed: ((Throwable?) -> Unit)? = null,
): State<Boolean> {
    val loading_state = remember(this) {
        mutableStateOf(MediaItemLoader.isUnknownLoading(this))
    }

    DisposableEffect(this) {
        val listener = object : ListenerLoader.Listener<String, MediaItemData> {
            override fun onLoadStarted(key: String) {
                if (key == id) {
                    loading_state.value = true
                }
            }
            override fun onLoadFinished(key: String, value: MediaItemData) {
                if (key == id) {
                    loading_state.value = false
                    onLoadSucceeded?.invoke(value)
                }
            }
            override fun onLoadFailed(key: String, error: Throwable) {
                if (key == id) {
                    loading_state.value = false
                    onLoadFailed?.invoke(error)
                }
            }
        }

        MediaItemLoader.addListener(listener)

        onDispose {
            MediaItemLoader.removeListener(listener)
        }
    }

    LaunchedEffect(this, load) {
        if (load) {
            onLoadFailed?.invoke(null)
            loadData(context, force = force)
        }
    }

    return loading_state
}
