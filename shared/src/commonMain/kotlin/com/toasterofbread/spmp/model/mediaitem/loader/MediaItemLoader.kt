package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.Database
import com.toasterofbread.spmp.api.getOrThrowHere
import com.toasterofbread.spmp.api.loadMediaItemData
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.SongData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object MediaItemLoader {
    private val song_lock = ReentrantLock()
    private val artist_lock = ReentrantLock()
    private val playlist_lock = ReentrantLock()

    private val loading_songs: MutableMap<String, Deferred<SongData>> = mutableMapOf()
    private val loading_artists: MutableMap<String, Deferred<ArtistData>> = mutableMapOf()
    private val loading_playlists: MutableMap<String, Deferred<PlaylistData>> = mutableMapOf()

    suspend fun <ItemType: MediaItemData> loadUnknown(item: ItemType, db: Database): Result<ItemType> =
        when (item) {
            is SongData -> loadSong(item, db) as Result<ItemType>
            is ArtistData -> loadArtist(item, db) as Result<ItemType>
            is PlaylistData -> loadPlaylist(item, db) as Result<ItemType>
            else -> throw NotImplementedError(item::class.toString())
        }

    suspend fun loadSong(song: SongData, db: Database): Result<SongData> {
        return loadItem(song, loading_songs, song_lock, db)
    }
    suspend fun loadArtist(artist: ArtistData, db: Database): Result<ArtistData> {
        return loadItem(artist, loading_artists, artist_lock, db)
    }
    suspend fun loadPlaylist(playlist: PlaylistData, db: Database): Result<PlaylistData> {
        return loadItem(playlist, loading_playlists, playlist_lock, db)
    }

    inline fun <T> withSongLock(action: () -> T): T = song_lock.withLock(action)
    inline fun <T> withArtistLock(action: () -> T): T = artist_lock.withLock(action)
    inline fun <T> withPlaylistLock(action: () -> T): T = playlist_lock.withLock(action)

    private suspend fun <ItemType: MediaItemData> loadItem(
        item: ItemType,
        loading_items: MutableMap<String, Deferred<ItemType>>,
        lock: ReentrantLock,
        db: Database
    ): Result<ItemType> {
        return performSafeLoad(
            item.id,
            lock,
            loading_items
        ) {
            loadMediaItemData(item, db).getOrThrowHere()
            item
        }
    }
}
