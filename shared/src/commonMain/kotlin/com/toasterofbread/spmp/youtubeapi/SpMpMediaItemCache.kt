package com.toasterofbread.spmp.youtubeapi

import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import dev.toastbits.ytmkt.itemcache.MediaItemCache
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylistBuilder
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtistBuilder
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSongBuilder
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist

internal class SpMpItemCache(val database: Database): MediaItemCache() {
    override fun getSong(song_id: String, keys: Set<SongKey>): YtmSong? =
        database.transactionWithResult {
            val song: Song = SongRef(song_id)
            var filled: Boolean = true

            val builder = YtmSongBuilder(song_id).apply {
                for (key in keys) {
                    when (key) {
                        SongKey.ARTIST_ID -> {
                            artists = song.Artists.get(database)?.map { YtmArtist(it.id) }
                            if (artists == null) {
                                filled = false
                            }
                        }
                        SongKey.RELATED_BROWSE_ID -> {
                            related_browse_id = song.RelatedBrowseId.get(database)
                            if (related_browse_id == null) {
                                filled = false
                            }
                        }
                        SongKey.TYPE -> {
                            type = song.TypeOfSong.get(database)
                            if (type == null) {
                                filled = false
                            }
                        }
                    }
                }
            }

            if (!filled && !song.Loaded.get(database)) {
                return@transactionWithResult null
            }
            return@transactionWithResult builder.build()
        }

    override fun getArtist(artist_id: String, keys: Set<ArtistKey>): YtmArtist? =
        database.transactionWithResult {
            val artist: Artist = ArtistRef(artist_id)
            var filled: Boolean = true

            val builder = YtmArtistBuilder(artist_id).apply {
                for (key in keys) {
                    when (key) {
                        ArtistKey.LAYOUTS -> {
                            layouts = artist.Layouts.get(database)?.map {
                                it.loadIntoYtmLayout(database)
                            }
                            if (layouts == null) {
                                filled = false
                            }
                        }
                    }
                }
            }

            if (!filled && !artist.Loaded.get(database)) {
                return@transactionWithResult null
            }
            return@transactionWithResult builder.build()
        }

    override fun getPlaylist(playlist_id: String, keys: Set<PlaylistKey>): YtmPlaylist? =
        database.transactionWithResult {
            val playlist: RemotePlaylist = RemotePlaylistRef(playlist_id)
            var filled: Boolean = true

            val builder = YtmPlaylistBuilder(playlist_id).apply {
                for (key in keys) {
                    when (key) {
                        PlaylistKey.ARTIST_ID -> {
                            artists = playlist.Artists.get(database)?.map { YtmArtist(it.id) }
                            if (artists == null) {
                                filled = false
                            }
                        }
                        PlaylistKey.ITEMS -> {
                            items = playlist.Items.get(database)?.map { YtmSong(it.id) }
                            if (items == null) {
                                filled = false
                            }
                        }
                        PlaylistKey.CONTINUATION -> {
                            continuation = playlist.Continuation.get(database)
                            if (continuation == null) {
                                filled = false
                            }
                        }
                    }
                }
            }

            if (!filled && !playlist.Loaded.get(database)) {
                return@transactionWithResult null
            }
            return@transactionWithResult builder.build()
        }
}
