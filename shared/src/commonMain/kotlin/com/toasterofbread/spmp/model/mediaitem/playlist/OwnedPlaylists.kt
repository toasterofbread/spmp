package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observeAsState
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.CreateAccountPlaylistEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberOwnedPlaylists(owner: Artist, context: AppContext): List<RemotePlaylistRef> {
    return context.database.playlistQueries.byOwner(owner.id)
        .observeAsState(
            {
                it.executeAsList().map { playlist_id ->
                    RemotePlaylistRef(playlist_id)
                }.asReversed()
            },
            null
        )
        .value
}

suspend fun MediaItemLibrary.createOwnedPlaylist(auth_state: YoutubeApi.UserAuthState, create_endpoint: CreateAccountPlaylistEndpoint): Result<RemotePlaylistData> = withContext(Dispatchers.IO) {
    if (!create_endpoint.isImplemented()) {
        return@withContext Result.failure(EndpointNotImplementedException(create_endpoint))
    }

    val playlist_id: String = create_endpoint.createAccountPlaylist(getString("new_playlist_title"), "")
        .getOrElse {
            return@withContext Result.failure(it)
        }

    val playlist = RemotePlaylistData(playlist_id)
    playlist.title = getString("new_playlist_title")
    playlist.owner = auth_state.own_channel

    playlist.saveToDatabase(auth_state.api.database)

    onPlaylistCreated(playlist)

    return@withContext Result.success(playlist)
}
