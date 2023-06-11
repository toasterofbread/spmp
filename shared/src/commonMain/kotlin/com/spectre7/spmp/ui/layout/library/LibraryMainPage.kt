package com.spectre7.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.api.Api
import com.spectre7.spmp.api.getOrReport
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val LOCAL_SONGS_PREVIEW_AMOUNT = 5

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
    val heading_text_style = MaterialTheme.typography.headlineSmall
    val coroutine_scope = rememberCoroutineScope()
    val player = LocalPlayerState.current

    AnimatedVisibility(!inline && multiselect_context.is_active) {
        multiselect_context.InfoDisplay()
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(30.dp),
        contentPadding = PaddingValues(bottom = bottom_padding)
    ) {
        if (topContent != null) {
            item {
                topContent.invoke()
            }
        }

        // Playlists
        item {
            val ytm_auth = Api.ytm_auth

            Column(Modifier.fillMaxWidth().animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(getString("library_row_playlists"), style = heading_text_style)

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    if (ytm_auth.initialised) {
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
                            val playlist = LocalPlaylist.createLocalPlaylist(SpMp.context)
                            player.openMediaItem(playlist)
                        }
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                }

                val playlists: MutableList<Playlist> = LocalPlaylist.rememberLocalPlaylistsListener().toMutableList()
                val show_likes: Boolean by Settings.KEY_SHOW_LIKES_PLAYLIST.rememberMutableState()

                for (id in ytm_auth.own_playlists) {
                    if (!show_likes && AccountPlaylist.formatId(id) == "LM") {
                        continue
                    }
                    playlists.add(AccountPlaylist.fromId(id))
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

        // Songs
        item {
            Column(Modifier.fillMaxWidth().animateContentSize()) {
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
                            song.PreviewLong(MediaItemPreviewParams(multiselect_context = multiselect_context), queue_index = shown_songs)

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
    }
}
