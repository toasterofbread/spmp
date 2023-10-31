package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

sealed interface RemotePlaylist: Playlist {
    val Continuation: Property<MediaItemLayout.Continuation?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Continuation",
            { playlistQueries.continuationById(id) },
            { continuation_token?.let {
                MediaItemLayout.Continuation(
                    it,
                    MediaItemLayout.Continuation.Type.values()[continuation_type!!.toInt()]
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
    override fun getURL(context: AppContext): String = "https://music.youtube.com/playlist?list=$id"
    override fun getEmptyData(): RemotePlaylistData

    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)

        if (data is RemotePlaylistData) {
            data.continuation = Continuation.get(db)
        }
    }

    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean): Result<RemotePlaylistData> {
        return super.loadData(context, populate_data, force) as Result<RemotePlaylistData>
    }

    override suspend fun setSortType(sort_type: MediaItemSortType?, context: AppContext): Result<Unit> {
        SortType.set(sort_type, context.database)
        return Result.success(Unit)
    }

    companion object {
        fun formatYoutubeId(id: String): String = id.removePrefix("VL")
    }
}

suspend fun Playlist.uploadAsAccountPlaylist(auth_state: YoutubeApi.UserAuthState, replace: Boolean = false): Result<RemotePlaylistData> {
    val create_endpoint = auth_state.CreateAccountPlaylist
    if (!create_endpoint.isImplemented()) {
        return Result.failure(EndpointNotImplementedException(create_endpoint))
    }

    val add_endpoint = auth_state.AccountPlaylistAddSongs
    if (!add_endpoint.isImplemented()) {
        return Result.failure(EndpointNotImplementedException(add_endpoint))
    }

    val db = auth_state.api.context.database

    val create_result = create_endpoint.createAccountPlaylist(
        getActiveTitle(db).orEmpty(),
        Description.get(db).orEmpty()
    )

    val created_playlist_id = create_result.fold(
        {
            if (!it.startsWith("VL")) "VL$it"
            else it
        },
        {
            return Result.failure(it)
        }
    )

    val account_playlist = RemotePlaylistData(created_playlist_id)

    val items = Items.get(db)
    if (!items.isNullOrEmpty()) {
        add_endpoint.addSongs(
            account_playlist,
            items.map { it.id }
        ).onFailure {
            return Result.failure(it)
        }
    }

    populateData(account_playlist, db)

    account_playlist.owner = auth_state.own_channel

    if (account_playlist.custom_image_url == null) {
        account_playlist.custom_image_url = ThumbnailProvider.get(db)?.getThumbnailUrl(MediaItemThumbnailProvider.Quality.HIGH)
    }

    account_playlist.saveToDatabase(db, account_playlist)

    if (replace) {
        PlaylistHolder.onPlaylistReplaced(this, account_playlist)
    }

    return Result.success(account_playlist)
}
