package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.rememberLocalPlaylists
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.launch

private const val LOCAL_SONGS_PREVIEW_AMOUNT = 5

@Composable
private fun PlaylistsRow(heading_text_style: TextStyle, multiselect_context: MediaItemMultiSelectContext) {
    val db = SpMp.context.database
    val player = LocalPlayerState.current
    val ytm_auth = Api.ytm_auth
    val coroutine_scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth().animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(getString("library_row_playlists"), style = heading_text_style)

            Spacer(Modifier.fillMaxWidth().weight(1f))

            if (ytm_auth.is_initialised) {
                var loading by remember { mutableStateOf(false) }

                fun loadPlaylists(report: Boolean = false) {
                    coroutine_scope.launch {
                        check(!loading)
                        loading = true

                        val result = ytm_auth.loadOwnPlaylists()
                        if (result.isFailure && report) {
                            SpMp.reportActionError(result.exceptionOrNull())
                        }

                        loading = false
                    }
                }

                LaunchedEffect(Unit) {
                    loadPlaylists()
                }

                IconButton({
                    if (!loading) {
                        loadPlaylists(true)
                    }
                }) {
                    Crossfade(loading) { loading ->
                        if (loading) {
                            SubtleLoadingIndicator(Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Refresh, null)
                        }
                    }
                }
            }

            IconButton({
                coroutine_scope.launch {
                    val playlist = createLocalPlaylist(db)
                    player.openMediaItem(playlist)
                }
            }) {
                Icon(Icons.Default.Add, null)
            }
        }

        val show_likes: Boolean by Settings.KEY_SHOW_LIKES_PLAYLIST.rememberMutableState()

        val local_playlists: List<LocalPlaylistRef> = rememberLocalPlaylists(db)
        val account_playlists: List<AccountPlaylistRef> = ytm_auth.own_playlists.mapNotNull { playlist_id ->
            if (!show_likes && Playlist.formatYoutubeId(playlist_id) == "LM") {
                return@mapNotNull null
            }
            return@mapNotNull AccountPlaylistRef(playlist_id)
        }

        val playlists: List<Playlist> = remember(local_playlists, account_playlists) {
            local_playlists + account_playlists
        }

        if (playlists.isNotEmpty()) {
            MediaItemGrid(
                playlists,
                Modifier.fillMaxWidth(),
                rows = 1,
                multiselect_context = multiselect_context
            )
        } else {
            Text(getString("library_playlists_empty"), Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
internal fun ArtistsRow(
    heading_text_style: TextStyle,
    multiselect_context: MediaItemMultiSelectContext,
    downloads: List<PlayerDownloadManager.DownloadStatus>,
    openPage: (LibrarySubPage?) -> Unit,
    onSongClicked: (songs: List<Song>, song: Song, index: Int) -> Unit
) {
    val player = LocalPlayerState.current

    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                getString("library_row_local_songs"),
                style = heading_text_style
            )

            Spacer(Modifier.fillMaxWidth().weight(1f))

            IconButton({ openPage(LibrarySubPage.SONGS) }) {
                Icon(Icons.Default.MoreHoriz, null)
            }
        }


        if (downloads.isNotEmpty()) {
            CompositionLocalProvider(LocalPlayerState provides remember { player.copy(onClickedOverride = { item, index ->
                onSongClicked(
                    downloads.mapNotNull { if (it.progress < 1f) null else it.song },
                    item as Song,
                    index!!
                )
            }) }) {
                var shown_songs = 0

                for (download in downloads) {
                    if (download.progress < 1f) {
                        continue
                    }

                    val song = download.song
                    MediaItemPreviewLong(song, multiselect_context = multiselect_context, multiselect_key = shown_songs)

                    if (++shown_songs == LOCAL_SONGS_PREVIEW_AMOUNT) {
                        break
                    }
                }

                if (shown_songs == LOCAL_SONGS_PREVIEW_AMOUNT) {
                    val total_songs = downloads.count { it.progress >= 1f }
                    Text(
                        getString("library_x_more_songs").replace("\$x", (total_songs - shown_songs).toString()),
                        Modifier
                            .padding(top = 5.dp)
                            .align(Alignment.CenterHorizontally)
                            .clickable { openPage(LibrarySubPage.SONGS) }
                    )
                }
            }
        } else {
            Text("No songs downloaded", Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun LibraryMainPage(
    downloads: List<PlayerDownloadManager.DownloadStatus>,
    multiselect_context: MediaItemMultiSelectContext,
    bottom_padding: Dp,
    inline: Boolean,
    openPage: (LibrarySubPage?) -> Unit,
    topContent: (@Composable () -> Unit)? = null,
    onSongClicked: (songs: List<Song>, song: Song, index: Int) -> Unit
) {
    val spacing = 20.dp
    val heading_text_style = MaterialTheme.typography.headlineSmall

    AnimatedVisibility(!inline && multiselect_context.is_active) {
        multiselect_context.InfoDisplay()
    }

    LazyColumn(contentPadding = PaddingValues(bottom = bottom_padding)) {
        if (topContent != null) {
            item {
                topContent.invoke()
            }
        }

        // Playlists
        item {
            PlaylistsRow(heading_text_style, multiselect_context)
        }

        item {
            Spacer(Modifier.height(spacing))
        }

        // Songs
        item {
            ArtistsRow(heading_text_style, multiselect_context, downloads, openPage, onSongClicked)
        }
    }
}
