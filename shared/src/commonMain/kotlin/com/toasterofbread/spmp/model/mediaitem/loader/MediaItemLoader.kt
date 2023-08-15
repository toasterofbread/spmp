package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.loadMediaItemData
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.SongData
import kotlinx.coroutines.Deferred
import java.util.concurrent.locks.ReentrantLock

internal object MediaItemLoader {
    private val song_lock = ReentrantLock()
    private val artist_lock = ReentrantLock()
    private val playlist_lock = ReentrantLock()

    private val loading_songs: MutableMap<String, Deferred<Result<SongData>>> = mutableMapOf()
    private val loading_artists: MutableMap<String, Deferred<Result<ArtistData>>> = mutableMapOf()
    private val loading_playlists: MutableMap<String, Deferred<Result<PlaylistData>>> = mutableMapOf()

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

    private suspend fun <ItemType: MediaItemData> loadItem(
        item: ItemType,
        loading_items: MutableMap<String, Deferred<Result<ItemType>>>,
        lock: ReentrantLock,
        db: Database
    ): Result<ItemType> {
        return performSafeLoad(
            item.id,
            lock,
            loading_items
        ) {
            loadMediaItemData(item, db).fold(
                { Result.success(item) },
                { Result.failure(it) }
            )
        }
    }
}

@Composable
fun MediaItem.loadDataOnChange(db: Database): State<Boolean> {
    val loading_state = remember(this) { mutableStateOf(false) }
    LaunchedEffect(this) {
        loading_state.value = true
        loadData(db)
        loading_state.value = false
    }
    return loading_state
}
