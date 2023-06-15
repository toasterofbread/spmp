package com.spectre7.spmp.model.mediaitem

class BrowseParamsPlaylist(val playlist_id: String, val params: String, id: String): Playlist(id) {
    override val data = object : PlaylistItemData(this)
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
        return super.loadGeneralData(playlist_id, params)
    }

    override suspend fun deletePlaylist(): Result<Unit> { throw NotImplementedError() }
    override fun saveItems(): Result<Unit> { throw NotImplementedError() }

    companion object {
        private val browse_params_playlists: MutableMap<String, BrowseParamsPlaylist> = mutableMapOf()

        @Synchronized
        fun fromId(playlist_id: String, browse_params: String): BrowseParamsPlaylist {
            val id = playlist_id + browse_params
            return browse_params_playlists.getOrPut(id) {
                val playlist = BrowseParamsPlaylist(playlist_id, browse_params, id)
                playlist.loadFromCache()
                return@getOrPut playlist
            }
        }
    }
}

class BrowseParamsPlaylistItemData(item: BrowseParamsPlaylist): PlaylistItemData(item) {}
