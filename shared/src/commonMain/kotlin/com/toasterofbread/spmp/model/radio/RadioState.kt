package com.toasterofbread.spmp.model.radio

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.toSongData
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.endpoint.ArtistShuffleEndpoint
import dev.toastbits.ytmkt.endpoint.RadioBuilderModifier
import dev.toastbits.ytmkt.endpoint.SongRadioEndpoint
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.radio.BuiltInRadioContinuation
import dev.toastbits.ytmkt.radio.RadioContinuation
import kotlinx.serialization.Serializable

@Serializable
data class RadioState(
    val source: RadioStateSource? = null,
    val item_queue_index: Int? = null,
    val shuffle: Boolean = false,
    @Serializable(with = RadioContinuationSerializer::class)
    val continuation: RadioContinuation? = null,
    val initial_songs_loaded: Boolean = false,

    val filters: List<RadioFilter>? = null,
    val current_filter_index: Int? = null
) {
    interface RadioStateSource {
        fun getDesiredMinimumItemCount(): Int = 0

        fun isItem(item: MediaItem): Boolean
        fun getMediaItem(): MediaItem

        data class ItemUid(val item_uid: String): RadioStateSource {
            override fun isItem(item: MediaItem): Boolean =
                item.getUid() == item_uid
            override fun getMediaItem(): MediaItem =
                getMediaItemFromUid(item_uid)
        }
    }

    fun isContinuationAvailable(): Boolean =
        continuation != null || (source != null && !initial_songs_loaded)

    internal suspend fun loadContinuation(context: AppContext): Result<RadioLoadResult?> = runCatching {
        if (source == null) {
            return@runCatching null
        }

        val item: MediaItem = source.getMediaItem()

        val result: RadioLoadResult = (
            if (continuation == null) loadInitialSongs(
                context = context,
                item = item,
                desired_minimum_item_count = source.getDesiredMinimumItemCount()
            )
            else loadContinuationSongs(context, item, continuation)
        )

        if (shuffle) {
            return@runCatching result.copy(songs = result.songs.shuffled())
        }

        return@runCatching result
    }

    private suspend fun loadInitialSongs(
        context: AppContext,
        item: MediaItem,
        desired_minimum_item_count: Int
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
                        BuiltInRadioContinuation(token, type = BuiltInRadioContinuation.Type.SONG, item_id = item.id)
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
                val items: List<SongData>? = playlist_data.items
                checkNotNull(items) { "playlist_data.items is null (${item.id})" }
                return loadInitialPlaylistRadio(items, playlist_data.continuation, desired_minimum_item_count = desired_minimum_item_count)
            }
            is LocalPlaylist -> {
                val items: List<SongData>? = item.loadData(context).getOrThrow().items
                checkNotNull(items) { "playlist_data.items is null (${item.id})" }
                return loadInitialPlaylistRadio(items, desired_minimum_item_count = desired_minimum_item_count)
            }
            else -> throw NotImplementedError(item::class.toString())
        }
    }

    private fun loadInitialPlaylistRadio(
        items: List<SongData>,
        next_continuation: RadioContinuation? = null,
        desired_minimum_item_count: Int = 0
    ): RadioLoadResult {
        val step_size: Int =
            maxOf(desired_minimum_item_count, PlaylistItemsRadioContinuation.PLAYLIST_RADIO_LOAD_STEP_SIZE)
        val continuation: RadioContinuation? =
            if (items.size > step_size)
                PlaylistItemsRadioContinuation(
                    song_ids = items.map { it.id },
                    head = step_size,
                    next_continuation = next_continuation
                )
            else next_continuation

        return RadioLoadResult(
            songs = items.take(step_size),
            continuation = continuation
        )
    }

    private suspend fun loadContinuationSongs(
        context: AppContext,
        item: MediaItem,
        continuation: RadioContinuation
    ): RadioLoadResult {
        val (songs, new_continuation) = continuation.loadContinuation(context.ytapi, getCurrentFilter()).getOrThrow()
        return RadioLoadResult(
            songs = songs.map { song ->
                when (song) {
                    is Song -> song
                    is YtmSong -> song.toSongData()
                    else -> throw IllegalStateException("Not a song $song (from $continuation)")
                }
            },
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
