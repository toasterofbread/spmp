package com.toasterofbread.spmp.api.radio

import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.DataParseException
import com.toasterofbread.spmp.api.RadioModifier
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private const val RADIO_ID_PREFIX = "RDAMVM"
private const val MODIFIED_RADIO_ID_PREFIX = "RDAT"

data class RadioData(val items: List<SongData>, var continuation: String?, val filters: List<List<RadioModifier>>?)

suspend fun getSongRadio(
    video_id: String, 
    continuation: String?, 
    filters: List<RadioModifier> = emptyList()
): Result<RadioData> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/next")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf(
            "enablePersistentPlaylistPanel" to true,
            "tunerSettingValue" to "AUTOMIX_SETTING_NORMAL",
            "playlistId" to videoIdToRadio(video_id, filters),
            "watchEndpointMusicSupportedConfigs" to mapOf(
                "watchEndpointMusicConfig" to mapOf(
                    "hasPersistentPlaylistPanel" to true,
                    "musicVideoType" to "MUSIC_VIDEO_TYPE_ATV"
                )
            ),
            "isAudioOnly" to true
        ).let {
            if (continuation == null) it
            else it + mapOf("continuation" to continuation)
        }))
        .build()

    val result = Api.request(request)
    val stream = result.getOrNull()?.getStream() ?: return@withContext result.cast()

    val radio: YoutubeiNextResponse.PlaylistPanelRenderer
    val out_filters: List<List<RadioModifier>>?

    try {
        if (continuation == null) {
            val renderer = Api.klaxon.parse<YoutubeiNextResponse>(stream)!!
                .contents
                .singleColumnMusicWatchNextResultsRenderer
                .tabbedRenderer
                .watchNextTabbedResultsRenderer
                .tabs
                .first()
                .tabRenderer
                .content!!
                .musicQueueRenderer
            radio = renderer.content.playlistPanelRenderer

            out_filters = renderer.subHeaderChipCloud?.chipCloudRenderer?.chips?.mapNotNull { chip ->
                radioToFilters(chip.getPlaylistId(), video_id)
            }
        }
        else {
            radio = Api.klaxon.parse<YoutubeiNextContinuationResponse>(stream)!!
                .continuationContents
                .playlistPanelContinuation
            out_filters = null
        }

        return@withContext Result.success(
            RadioData(
                radio.contents.map { item ->
                    val renderer = item.getRenderer()
                    val song = SongData(renderer.videoId)

                    song.title = renderer.title.first_text

                    renderer.getArtist(song, SpMp.context.database).fold(
                        { artist ->
                            if (artist != null) {
                                song.artist = artist
                            }
                        },
                        { return@withContext Result.failure(it) }
                    )

                    return@map song
                },
                radio.continuations?.firstOrNull()?.data?.continuation,
                out_filters
            )
        )
    }
    catch (error: Throwable) {
        return@withContext Result.failure(
            DataParseException.ofYoutubeJsonRequest(
                request,
                cause = error
            )
        )
    }
    finally {
        stream.close()
    }
}

private fun radioToFilters(radio: String, video_id: String): List<RadioModifier>? {
    if (!radio.startsWith(MODIFIED_RADIO_ID_PREFIX)) {
        return null
    }

    val ret: MutableList<RadioModifier> = mutableListOf()
    val modifier_string = radio.substring(MODIFIED_RADIO_ID_PREFIX.length, radio.length - video_id.length)

    var c = 0
    while (c + 1 < modifier_string.length) {
        val modifier = RadioModifier.fromString(modifier_string.substring(c++, ++c))
        if (modifier != null) {
            ret.add(modifier)
        }
    }

    if (ret.isEmpty()) {
        return null
    }

    return ret
}

private fun videoIdToRadio(video_id: String, filters: List<RadioModifier>): String {
    if (filters.isEmpty()) {
        return RADIO_ID_PREFIX + video_id
    }

    val ret = StringBuilder(MODIFIED_RADIO_ID_PREFIX)
    for (filter in filters) {
        filter.string?.also { ret.append(it) }
    }
    ret.append('v')
    ret.append(video_id)
    return ret.toString()
}

