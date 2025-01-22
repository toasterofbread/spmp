package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.rememberLocalLikedSongs
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.radio.RadioState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.rememberSongDownloads
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import dev.toastbits.composekit.components.platform.composable.ScrollBarLazyColumn
import dev.toastbits.composekit.components.utils.composable.LoadActionIconButton
import dev.toastbits.composekit.components.utils.composable.RowOrColumnScope
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.components.utils.composable.crossfade.EmptyListCrossfade
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.util.model.Locale
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.ytmkt.endpoint.LoadPlaylistEndpoint
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import dev.toastbits.ytmkt.model.implementedOrNull
import dev.toastbits.ytmkt.uistrings.durationToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.`library_$x_songs`
import spmp.shared.generated.resources.library_no_items_match_filter
import spmp.shared.generated.resources.library_no_liked_songs
import spmp.shared.generated.resources.library_no_local_songs
import spmp.shared.generated.resources.library_songs_downloaded
import spmp.shared.generated.resources.library_songs_downloaded_title
import spmp.shared.generated.resources.library_songs_liked
import spmp.shared.generated.resources.library_songs_liked_title
import spmp.shared.generated.resources.local_songs_playlist_name

class LibrarySongsPage(context: AppContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        MediaItemType.SONG.getIcon()

    private var sorted_songs: List<Song> by mutableStateOf(emptyList())
    private var load_error: Throwable? by mutableStateOf(null)

    override fun canShowAltContent(): Boolean = true
    override fun getAltContentButtons(): Pair<Pair<StringResource, ImageVector>, Pair<StringResource, ImageVector>> =
        Pair(
            Pair(Res.string.library_songs_downloaded, Icons.Default.Download),
            Pair(Res.string.library_songs_liked, Icons.Default.Favorite)
        )

    @Composable
    override fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        showing_alt_content: Boolean,
        modifier: Modifier
    ) {
        val player: PlayerState = LocalPlayerState.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()
        val auth_state: ApiAuthenticationState? = player.context.ytapi.user_auth_state

        val downloads: List<DownloadStatus> by rememberSongDownloads()
        val local_liked_songs: List<Song>? by rememberLocalLikedSongs()

        val remote_likes_playlist: RemotePlaylistRef? = remember(auth_state) { if (auth_state != null) RemotePlaylistRef("LM") else null }
        val remote_liked_songs: List<Song>? by remote_likes_playlist?.Items?.observe(player.database)

        LaunchedEffect(Unit) {
            sorted_songs = emptyList()
            load_error = null
        }

        with(library_page) {
            LaunchedEffect(showing_alt_content, remote_likes_playlist) {
                if (showing_alt_content) {
                    remote_likes_playlist?.loadData(player.context, populate_data = false, force = true)
                }
            }

            LaunchedEffect(downloads, search_filter, sort_type, reverse_sort, showing_alt_content, local_liked_songs, remote_liked_songs) {
                val songs: List<Song>

                if (showing_alt_content) {
                    songs = (remote_liked_songs ?: local_liked_songs ?: emptyList()).distinctBy { it.id }
                }
                else {
                    songs = downloads.mapNotNull { download -> if (download.progress < 1f) null else download.song }
                }

                val filter: String? = if (search_filter?.isNotEmpty() == true) search_filter else null
                val filtered_songs: List<Song> = songs.filter { song ->
                    filter == null || song.getActiveTitle(player.database)?.contains(filter, true) == true
                }

                sorted_songs = sort_type.sortItems(filtered_songs, player.database, reverse_sort)
            }
        }


        Column(modifier) {
            EmptyListCrossfade(sorted_songs) { current_songs ->
                ScrollBarLazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = content_padding,
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    item {
                        LibraryPageTitle(
                            if (showing_alt_content) stringResource(Res.string.library_songs_liked_title)
                            else stringResource(Res.string.library_songs_downloaded_title)
                        )
                    }

                    load_error?.also { error ->
                        item {
                            ErrorInfoDisplay(error, isDebugBuild(), Modifier.fillMaxWidth()) {
                                load_error = null
                            }
                        }
                    }

                    if (current_songs == null) {
                        item {
                            Text(
                                if (library_page.search_filter != null) stringResource(Res.string.library_no_items_match_filter)
                                else if (showing_alt_content) stringResource(Res.string.library_no_liked_songs)
                                else stringResource(Res.string.library_no_local_songs),
                                Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InfoRow(current_songs, Modifier.fillMaxWidth().weight(1f), !showing_alt_content)

                                multiselect_context.CollectionToggleButton(
                                    remember(current_songs) {
                                        current_songs.map { Pair(it, null) }
                                    }
                                )
                            }
                        }

                        itemsIndexed(current_songs, { _, item -> item.id }) { index, song ->
                            CompositionLocalProvider(LocalPlayerClickOverrides provides LocalPlayerClickOverrides.current.copy(
                                onClickOverride = { _, _ ->
                                    coroutine_scope.launchSingle {
                                        onSongClicked(sorted_songs, player, song, index)
                                    }
                                }
                            )) {
                                MediaItemPreviewLong(
                                    song,
                                    Modifier.fillMaxWidth(),
                                    multiselect_context = multiselect_context,
                                    show_type = false,
                                    show_play_count = true,
                                    show_download_indicator = false,
                                    getExtraInfo = {
                                        val ui_language: Locale by player.context.observeUiLanguage()
                                        val duration_string: String? = remember(song.id, ui_language) {
                                            song.Duration.get(player.database)?.let { duration ->
                                                durationToString(duration, ui_language.toTag(), true)
                                            }
                                        }

                                        listOfNotNull(duration_string)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }



    @Composable
    override fun RowOrColumnScope.SideContent(showing_alt_content: Boolean) {
        val player: PlayerState = LocalPlayerState.current
        val auth_state: ApiAuthenticationState? = player.context.ytapi.user_auth_state

        val load_endpoint: LoadPlaylistEndpoint? = player.context.ytapi.LoadPlaylist.implementedOrNull()

        AnimatedVisibility(showing_alt_content && auth_state != null && load_endpoint != null) {
            LoadActionIconButton(
                {
                    val result: Result<RemotePlaylistData> = RemotePlaylistRef("LM").loadData(player.context, populate_data = false, force = true)
                    load_error = result.exceptionOrNull()
                }
            ) {
                Icon(Icons.Default.Refresh, null)
            }
        }

        if (!showing_alt_content && sorted_songs.isEmpty()) {
            LibrarySyncButton()
        }
    }
}

@Composable
private fun InfoRow(songs: List<Song>, modifier: Modifier = Modifier, show_sync_button: Boolean = true) {
    if (songs.isEmpty()) {
        return
    }

    val player: PlayerState = LocalPlayerState.current

    var total_duration_string: String? by remember { mutableStateOf(null) }
    LaunchedEffect(songs) {
        total_duration_string = null

        val duration: Long = songs.sumOf { song ->
            song.Duration.get(player.database) ?: 0
        }
        if (duration == 0L) {
            return@LaunchedEffect
        }

        total_duration_string = durationToString(duration, hl = player.context.getUiLanguage().toTag())
    }

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(stringResource(Res.string.`library_$x_songs`).replace("\$x", songs.size.toString()))

        total_duration_string?.also { duration ->
            Text("\u2022")
            Text(duration)
        }

        Spacer(Modifier.fillMaxWidth().weight(1f))

        if (show_sync_button) {
            LibrarySyncButton()
        }
    }
}

@Composable
private fun LibrarySyncButton() {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    IconButton({
        if (MediaItemLibrary.song_sync_in_progress) {
            return@IconButton
        }

        coroutine_scope.launch {
            MediaItemLibrary.syncLocalSongs(player.context)
        }
    }) {
        Crossfade(MediaItemLibrary.song_sync_in_progress) { syncing ->
            if (syncing) {
                SubtleLoadingIndicator()
            }
            else {
                Icon(Icons.Default.Sync, null)
            }
        }
    }
}

private suspend fun onSongClicked(songs: List<Song>, player: PlayerState, clicked_song: Song, index: Int) = withContext(Dispatchers.Default) {
    val playlist_name: String = getString(Res.string.local_songs_playlist_name)
    player.withPlayer {
        startRadioAtIndex(
            index = 0,
            object : RadioState.RadioStateSource {
                private val playlist: PlaylistData =
                    LocalPlaylistData("!LOCALSONGS").apply {
                        name = playlist_name
                        loaded = true
                        items = songs.map {
                            SongData(it.id)
                        }
                    }
                override fun isItem(item: MediaItem): Boolean = false
                override fun getMediaItem(): MediaItem = playlist
                override fun getDesiredMinimumItemCount(): Int = index + 1
            },
            onSuccessfulLoad = {
                seekToSong(index)
            }
        )
    }
}
