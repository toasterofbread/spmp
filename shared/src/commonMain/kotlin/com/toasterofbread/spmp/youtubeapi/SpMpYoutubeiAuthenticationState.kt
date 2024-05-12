package com.toasterofbread.spmp.youtubeapi

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.impl.youtubei.endpoint.*
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import io.ktor.http.Headers
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.playlist.toRemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.artist.toArtistData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef

class SpMpYoutubeiAuthenticationState(
    val database: Database,
    api: YoutubeiApi,
    own_channel_id: String?,
    headers: Headers
): YoutubeiAuthenticationState(api, headers, own_channel_id) {

    private suspend fun performTransaction(action: () -> Unit) {
        database.transaction {
            action()
        }
    }

    override val AccountPlaylists = object : YTMAccountPlaylistsEndpoint(this) {
        override suspend fun getAccountPlaylists(): Result<List<YtmPlaylist>> = runCatching {
            val playlists: List<YtmPlaylist> = super.getAccountPlaylists().getOrThrow()

            performTransaction {
                database.playlistQueries.clearOwners()
                for (playlist in playlists.asReversed()) {
                    val playlist_data: RemotePlaylistData = playlist.toRemotePlaylistData()
                    if (own_channel_id != null) {
                        playlist_data.owner = ArtistRef(own_channel_id)
                    }
                    else {
                        playlist_data.owned_by_user = true
                    }

                    playlist_data.saveToDatabase(database)
                }
            }

            return@runCatching playlists
        }
    }

    // override val CreateAccountPlaylist = YTMCreateAccountPlaylistEndpoint(this)
    // override val DeleteAccountPlaylist = YTMDeleteAccountPlaylistEndpoint(this)
    // override val SubscribedToArtist = YTMSubscribedToArtistEndpoint(this)
    // override val SetSubscribedToArtist = YTMSetSubscribedToArtistEndpoint(this)
    // override val SongLiked = YTMSongLikedEndpoint(this)
    // override val SetSongLiked = YTMSetSongLikedEndpoint(this)
    // override val MarkSongAsWatched = YTMMarkSongAsWatchedEndpoint(this)
    // override val AccountPlaylistEditor = YTMAccountPlaylistEditorEndpoint(this)
    // override val AccountPlaylistAddSongs = YTMAccountPlaylistAddSongsEndpoint(this)
    override val LikedAlbums = object : YTMLikedAlbumsEndpoint(this) {
        override suspend fun getLikedAlbums(): Result<List<YtmPlaylist>> = runCatching {
            val albums: List<YtmPlaylist> = super.getLikedAlbums().getOrThrow()
            performTransaction {
                for (album in albums) {
                    album.toRemotePlaylistData().saveToDatabase(database)
                }
            }
            return@runCatching albums
        }
    }
    override val LikedArtists = object : YTMLikedArtistsEndpoint(this) {
        override suspend fun getLikedArtists(): Result<List<YtmArtist>> = runCatching {
            val artists: List<YtmArtist> = super.getLikedArtists().getOrThrow()
            performTransaction {
                for (artist in artists) {
                    artist.toArtistData().saveToDatabase(database)
                }
            }
            return@runCatching artists
        }
    }
    override val LikedPlaylists = object : YTMLikedPlaylistsEndpoint(this) {
        override suspend fun getLikedPlaylists(): Result<List<YtmPlaylist>> = runCatching {
            val playlists: List<YtmPlaylist> = super.getLikedPlaylists().getOrThrow()
            performTransaction {
                for (playlist in playlists) {
                    playlist.toRemotePlaylistData().saveToDatabase(database)
                }
            }
            return@runCatching playlists
        }
    }
}
