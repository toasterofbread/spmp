package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class LoadSongEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun loadSong(song_data: SongData): Result<SongData>
}

abstract class LoadArtistEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun loadArtist(artist_data: ArtistData): Result<ArtistData>
}

abstract class LoadPlaylistEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun loadPlaylist(playlist_data: RemotePlaylistData, continuation: MediaItemLayout.Continuation? = null): Result<RemotePlaylistData>
}
