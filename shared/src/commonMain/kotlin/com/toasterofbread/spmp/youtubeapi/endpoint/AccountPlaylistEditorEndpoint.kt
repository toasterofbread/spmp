package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class AccountPlaylistEditorEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract fun getEditor(playlist: Playlist): PlaylistEditor
}
