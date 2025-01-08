package com.toasterofbread.spmp.model.radio

import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import dev.toastbits.ytmkt.endpoint.RadioBuilderModifier
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.radio.RadioContinuation
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaylistItemsRadioContinuation(
    val song_ids: List<String>,
    val head: Int,
    @Serializable(with = RadioContinuationSerializer::class)
    val next_continuation: RadioContinuation?
): RadioContinuation {
    override suspend fun loadContinuation(
        api: YtmApi,
        filters: List<RadioBuilderModifier>
    ): Result<Pair<List<YtmMediaItem>, RadioContinuation?>> = runCatching {
        val continuation: RadioContinuation? =
            if (song_ids.size - head > PLAYLIST_RADIO_LOAD_STEP_SIZE) copy(head = head + PLAYLIST_RADIO_LOAD_STEP_SIZE)
            else next_continuation

        return@runCatching (
            song_ids.subList(
                fromIndex = head,
                toIndex = (head + PLAYLIST_RADIO_LOAD_STEP_SIZE).coerceAtMost(song_ids.size)
            )
                .map { SongRef(it) }
            to continuation
        )
    }

    companion object {
        const val PLAYLIST_RADIO_LOAD_STEP_SIZE: Int = 30
    }
}
