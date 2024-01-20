package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.layout.LambdaViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.ListPageBrowseIdViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.PlainViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMore
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeLocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistRadioEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class YTMArtistRadioEndpoint(override val api: YoutubeMusicApi): ArtistRadioEndpoint() {
    override suspend fun getArtistRadio(artist: Artist, continuation: String?): Result<RadioData> = withContext(Dispatchers.IO) {
        var layouts: List<ArtistLayout>? = artist.Layouts.get(api.database)
        if (layouts == null) {
            artist.loadData(api.context, populate_data = false).onFailure {
                return@withContext Result.failure(IOException(it))
            }
            layouts = artist.Layouts.get(api.database)

            if (layouts == null) {
                return@withContext Result.failure(NullPointerException("$artist layouts is null"))
            }
        }

        for (string_id in listOf(YoutubeUILocalisation.StringID.ARTIST_ROW_SONGS, YoutubeUILocalisation.StringID.ARTIST_ROW_VIDEOS)) {
            for (layout in layouts) {
                val title: YoutubeLocalisedString = (layout.Title.get(api.database) as? YoutubeLocalisedString) ?: continue
                if (title.getYoutubeStringId() != string_id) {
                    continue
                }

                val view_more: ViewMore = layout.ViewMore.get(api.database) ?: continue
                when (view_more) {
                    is MediaItemViewMore -> {
                        val songs_playlist: RemotePlaylist = (view_more.browse_media_item as? RemotePlaylist) ?: continue
                        val items = songs_playlist.loadData(api.context).getOrNull()?.items ?: continue

                        return@withContext Result.success(
                            RadioData(items, null)
                        )
                    }
                    is ListPageBrowseIdViewMore -> {
                        val artist_endpoint = api.ArtistWithParams.implementedOrNull() ?: continue
                        val rows = artist_endpoint.loadArtistWithParams(view_more.getBrowseParamsData(title, api.context)).getOrNull() ?: continue

                        return@withContext Result.success(
                            RadioData(rows.flatMap { it.items.filterIsInstance<SongData>() }, null)
                        )
                    }
                    is PlainViewMore, is LambdaViewMore -> continue
                }
            }
        }

        return@withContext Result.failure(RuntimeException("Could not find items layout for $artist"))
    }
}
