package com.toasterofbread.spmp.model.mediaitem.playlist

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.observeAsState
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import dev.toastbits.ytmkt.endpoint.CreateAccountPlaylistEndpoint

@Composable
fun rememberOwnedPlaylists(owner_id: String, context: AppContext): List<RemotePlaylistRef> {
    return context.database.playlistQueries.byOwner(owner_id)
        .observeAsState(
            Unit,
            {
                it.executeAsList().map { playlist_id ->
                    RemotePlaylistRef(playlist_id)
                }.asReversed()
            },
            null
        )
        .value
}

suspend fun MediaItemLibrary.createOwnedPlaylist(
    context: AppContext,
    auth_state: ApiAuthenticationState,
    create_endpoint: CreateAccountPlaylistEndpoint
): Result<RemotePlaylistData> = runCatching {
    val playlist_id: String = create_endpoint.createAccountPlaylist(getString("new_playlist_title"), "").getOrThrow()

    val playlist: RemotePlaylistData = RemotePlaylistData(playlist_id)
    playlist.name = getString("new_playlist_title")
    playlist.owner = auth_state.own_channel_id?.let { ArtistRef(it) }

    playlist.saveToDatabase(context.database)
    onPlaylistCreated(playlist)

    return@runCatching playlist
}
