package com.toasterofbread.spmp.model.radio

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.toSongData
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.endpoint.RadioBuilderModifier
import dev.toastbits.ytmkt.endpoint.ArtistShuffleEndpoint
import dev.toastbits.ytmkt.endpoint.SongRadioEndpoint
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.radio.RadioContinuation
import kotlinx.serialization.Serializable

@Serializable
data class RadioState(
    val item_uid: String? = null,
    val item_queue_index: Int? = null,
    val shuffle: Boolean = false,
    val continuation: RadioContinuation? = null,
    val initial_songs_loaded: Boolean = false,

    val filters: List<RadioFilter>? = null,
    val current_filter_index: Int? = null
) {
    fun isContinuationAvailable(): Boolean =
        continuation != null || (item_uid != null && !initial_songs_loaded)

    internal suspend fun loadContinuation(context: AppContext): Result<RadioLoadResult?> = runCatching {
        if (item_uid == null) {
            return@runCatching null
        }

        val item: MediaItem = getMediaItemFromUid(item_uid)

        val result: RadioLoadResult = (
            if (continuation == null) loadInitialSongs(context, item)
            else loadContinuationSongs(context, item, continuation)
        )

        if (shuffle) {
            return@runCatching result.copy(songs = result.songs.shuffled())
        }

        return@runCatching result
    }

    private suspend fun loadInitialSongs(
        context: AppContext,
        item: MediaItem
    ): RadioLoadResult {
        if (initial_songs_loaded) {
            throw RuntimeException("Initial songs already loaded $this")
        }

        when (item) {
            is Song -> {
                val radio: SongRadioEndpoint.RadioData =
                    context.ytapi.SongRadio.getSongRadio(
                        song_id = item.id,
                        continuation = null,
                        filters = getCurrentFilter()
                    ).getOrThrow()

                return RadioLoadResult(
                    songs = radio.items.map { it.toSongData() },
                    continuation = radio.continuation?.let { token ->
                        RadioContinuation(token, type = RadioContinuation.Type.SONG, item_id = item.id)
                    },
                    filters = radio.filters
                )
            }
            is Artist -> {
                val shufflePlaylistId: String =
                    item.ShufflePlaylistId.get(context.database)
                    ?: throw NullPointerException("Artist ${item.id} has no shuffle playlist ID")

                val radio: ArtistShuffleEndpoint.RadioData =
                    context.ytapi.ArtistShuffle.getArtistShuffle(
                        artist_shuffle_playlist_id = shufflePlaylistId,
                        continuation = null
                    ).getOrThrow()

                return RadioLoadResult(
                    songs = radio.items.map { it.toSongData() },
                    continuation = radio.continuation
                )
            }
            is RemotePlaylist -> {
                val playlist_data: RemotePlaylistData = item.loadData(context).getOrThrow()

                val continuation: RadioContinuation? =
                    if (playlist_data.items == null)
                        playlist_data.continuation
                        ?: RadioContinuation(item.id, type = RadioContinuation.Type.PLAYLIST_INITIAL)
                    else playlist_data.continuation

                if (playlist_data.items == null) {
                    throw NullPointerException("playlist_data.items is null (${item.id})")
                }

                return RadioLoadResult(
                    songs = playlist_data.items!!,
                    continuation = continuation
                )
            }
            is LocalPlaylist -> {
                val playlist_data: LocalPlaylistData = item.loadData(context).getOrThrow()

                if (playlist_data.items == null) {
                    throw NullPointerException("playlist_data.items is null (${item.id})")
                }

                return RadioLoadResult(songs = playlist_data.items!!)
            }
            else -> throw NotImplementedError(item::class.toString())
        }
    }

    private suspend fun loadContinuationSongs(
        context: AppContext,
        item: MediaItem,
        continuation: RadioContinuation
    ): RadioLoadResult {
        val (songs, new_continuation) = continuation.loadContinuation(context.ytapi, getCurrentFilter()).getOrThrow()
        return RadioLoadResult(
            songs = songs.filterIsInstance<YtmSong>().map { it.toSongData() },
            continuation = new_continuation,
            filters = filters
        )
    }

    private fun getCurrentFilter(): RadioFilter {
        if (current_filter_index == null) {
            return emptyList()
        }
        if (current_filter_index == -1) {
            return listOf(RadioBuilderModifier.Internal.ARTIST)
        }
        return filters?.getOrNull(current_filter_index) ?: emptyList()
    }
}
