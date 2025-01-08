package com.toasterofbread.spmp.model.mediaitem.playlist

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.radio.BuiltInRadioContinuation
import dev.toastbits.ytmkt.radio.RadioContinuation
import dev.toastbits.ytmkt.model.external.ThumbnailProvider as YtmThumbnailProvider

sealed interface RemotePlaylist: Playlist {
    val Continuation: Property<BuiltInRadioContinuation?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Continuation",
            { playlistQueries.continuationById(id) },
            { continuation_token?.let {
                BuiltInRadioContinuation(
                    it,
                    BuiltInRadioContinuation.Type.entries[continuation_type!!.toInt()]
                )
            }},
            { playlistQueries.updateContinuationById(it?.token, it?.type?.ordinal?.toLong(), id) }
        )
    val PlaylistUrl: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "PlaylistUrl",
            { playlistQueries.playlistUrlById(id) },
            { playlist_url },
            { playlistQueries.updatePlaylistUrlById(it, id) }
        )

    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_REM

    override fun getHolder(): PlaylistHolder = PlaylistHolder(this)
    override suspend fun getUrl(context: AppContext): String = "https://music.youtube.com/playlist?list=${id.removePrefix("VL")}"
    override fun getEmptyData(): RemotePlaylistData

    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)

        if (data is RemotePlaylistData) {
            data.continuation = Continuation.get(db)
        }
    }

    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean, save: Boolean): Result<RemotePlaylistData> {
        return super.loadData(context, populate_data, force, save) as Result<RemotePlaylistData>
    }

    override suspend fun setSortType(sort_type: MediaItemSortType?, context: AppContext): Result<Unit> {
        SortType.set(sort_type, context.database)
        return Result.success(Unit)
    }

    companion object {
        fun formatYoutubeId(id: String): String = id.removePrefix("VL")
    }
}

suspend fun Playlist.uploadAsAccountPlaylist(
    context: AppContext,
    auth_state: ApiAuthenticationState,
    replace: Boolean = false
): Result<RemotePlaylistData> = runCatching {
    val db: Database = context.database

    val create_result: String = auth_state.CreateAccountPlaylist.createAccountPlaylist(
        getActiveTitle(db).orEmpty(),
        Description.get(db).orEmpty()
    ).getOrThrow()

    val created_playlist_id: String =
        if (!create_result.startsWith("VL")) "VL$create_result"
        else create_result

    val account_playlist = RemotePlaylistData(created_playlist_id)

    val items = Items.get(db)
    if (!items.isNullOrEmpty()) {
        auth_state.AccountPlaylistAddSongs.addSongs(
            account_playlist.id,
            items.map { it.id }
        ).getOrThrow()
    }

    populateData(account_playlist, db)

    account_playlist.owner = auth_state.own_channel_id?.let { ArtistRef(it) }

    if (account_playlist.custom_image_url == null) {
        account_playlist.custom_image_url = ThumbnailProvider.get(db)?.getThumbnailUrl(YtmThumbnailProvider.Quality.HIGH)
    }

    account_playlist.saveToDatabase(db, account_playlist)

    if (replace) {
        PlaylistHolder.onPlaylistReplaced(this, account_playlist)
    }

    return@runCatching account_playlist
}
