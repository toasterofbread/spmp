package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.spmp.api.getOrThrowHere
import com.toasterofbread.spmp.api.loadMediaItemData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object MediaItemLoader {
    private val song_lock = ReentrantLock()
    private val artist_lock = ReentrantLock()
    private val playlist_lock = ReentrantLock()

    private val loading_songs: MutableMap<String, Pair<SongData, Deferred<SongData>>> = mutableMapOf()
    private val loading_artists: MutableMap<String, Pair<ArtistData, Deferred<ArtistData>>> = mutableMapOf()
    private val loading_playlists: MutableMap<String, Pair<PlaylistData, Deferred<PlaylistData>>> = mutableMapOf()

    suspend fun loadSong(song: SongData): Result<SongData> {
        return loadItem(song, loading_songs, song_lock)
    }
    suspend fun loadArtist(artist: ArtistData): Result<ArtistData> {
        return loadItem(artist, loading_artists, artist_lock)
    }
    suspend fun loadPlaylist(playlist: PlaylistData): Result<PlaylistData> {
        return loadItem(playlist, loading_playlists, playlist_lock)
    }

    inline fun <T> withSongLock(action: () -> T): T = song_lock.withLock(action)
    inline fun <T> withArtistLock(action: () -> T): T = artist_lock.withLock(action)
    inline fun <T> withPlaylistLock(action: () -> T): T = playlist_lock.withLock(action)

    private suspend fun <ItemType: MediaItemData> loadItem(
        item: ItemType,
        loading_items: MutableMap<String, Pair<ItemType, Deferred<ItemType>>>,
        lock: ReentrantLock
    ): Result<ItemType> {
        val end_with_lock = lock.isHeldByCurrentThread
        lock.lock()

        val loading = loading_items[item.id]
        if (loading != null) {
            lock.unlock()

            return runCatching {
                loading.second.await()
            }
        }

        val load_job: Deferred<ItemType> = coroutineScope {
            async {
                loadMediaItemData(item).getOrThrowHere()
                item
            }
        }

        loading_items[item.id] = Pair(item, load_job)

        lock.unlock()

        return runCatching {
            val result = load_job.await()

            lock.lock()
            loading_items.remove(item.id)

            if (!end_with_lock) {
                lock.unlock()
            }

            result
        }
    }
}
