package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import com.toasterofbread.spmp.youtubeapi.endpoint.SongRadioEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextContinuationResponse
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private const val RADIO_ID_PREFIX = "RDAMVM"
private const val MODIFIED_RADIO_ID_PREFIX = "RDAT"

class YTMSongRadioEndpoint(override val api: YoutubeMusicApi): SongRadioEndpoint() {
    override suspend fun getSongRadio(
        video_id: String,
        continuation: String?,
        filters: List<RadioBuilderModifier>
    ): Result<RadioData> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/next")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
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
                }
            )
            .build()

        val result = api.performRequest(request)

        val radio: YoutubeiNextResponse.PlaylistPanelRenderer
        val out_filters: List<List<RadioBuilderModifier>>?

        if (continuation == null) {
            val data: YoutubeiNextResponse = result.parseJsonResponse {
                return@withContext Result.failure(
                    com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException.ofYoutubeJsonRequest(
                        request,
                        api,
                        cause = it
                    )
                )
            }

            val renderer = data
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
            val data: YoutubeiNextContinuationResponse = result.parseJsonResponse {
                return@withContext Result.failure(
                    com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException.ofYoutubeJsonRequest(
                        request,
                        api,
                        cause = it
                    )
                )
            }

            radio = data
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

                    renderer.getArtist(song, api.context).fold(
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
}

private fun radioToFilters(radio: String, video_id: String): List<RadioBuilderModifier>? {
    if (!radio.startsWith(MODIFIED_RADIO_ID_PREFIX)) {
        return null
    }

    val ret: MutableList<RadioBuilderModifier> = mutableListOf()
    val modifier_string = radio.substring(MODIFIED_RADIO_ID_PREFIX.length, radio.length - video_id.length)

    var c = 0
    while (c + 1 < modifier_string.length) {
        val modifier = RadioBuilderModifier.fromString(modifier_string.substring(c++, ++c))
        if (modifier != null) {
            ret.add(modifier)
        }
    }

    if (ret.isEmpty()) {
        return null
    }

    return ret
}

private fun videoIdToRadio(video_id: String, filters: List<RadioBuilderModifier>): String {
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
