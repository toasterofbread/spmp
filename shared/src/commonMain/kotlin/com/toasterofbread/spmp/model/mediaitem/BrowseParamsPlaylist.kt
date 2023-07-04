package com.toasterofbread.spmp.model.mediaitem

import SpMp
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.data.PlaylistItemData
import com.toasterofbread.spmp.platform.PlatformContext

class BrowseParamsPlaylist(
    val artist_id: String,
    val params: String,
    id: String,
    context: PlatformContext,
): Playlist(id, context) {
    override val data = BrowseParamsPlaylistItemData(this)
    override val is_editable = false
    override val playlist_type = null
    override val total_duration: Long? get() {
        val items = data.items ?: return null

        var sum = 0L
        for (item in items) {
            if (item is Song) {
                sum += item.duration ?: 0
            }
        }
        return sum
    }
    override val item_count: Int? = data.items?.size

    override suspend fun loadGeneralData(item_id: String, browse_params: String?): Result<Unit> {
        return super.loadGeneralData(artist_id, params)
    }

    override suspend fun deletePlaylist(): Result<Unit> { throw NotImplementedError() }
    override suspend fun saveItems(): Result<Unit> { throw NotImplementedError() }

    override val url: String
        get() = "https://music.youtube.com/channel/$artist_id"

    @Composable
    override fun getThumbnailHolder(): MediaItem =
        Artist.fromId(artist_id)
    override fun getDefaultRegistryEntry(): PlaylistDataRegistryEntry = PlaylistDataRegistryEntry()

    companion object {
        private val browse_params_playlists: MutableMap<String, BrowseParamsPlaylist> = mutableMapOf()

        @Synchronized
        fun fromId(item_id: String, browse_params: String, context: PlatformContext = SpMp.context): BrowseParamsPlaylist {
            return browse_params_playlists.getOrPut(item_id) {
                val playlist = BrowseParamsPlaylist(item_id, browse_params, item_id, context)
                playlist.loadFromCache()
                return@getOrPut playlist
            }
        }
    }
}

class BrowseParamsPlaylistItemData(item: BrowseParamsPlaylist): PlaylistItemData(item)
