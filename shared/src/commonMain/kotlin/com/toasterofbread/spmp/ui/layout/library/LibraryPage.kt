package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortOption
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.rememberSongDownloads
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.YoutubeMusicLoginConfirmation
import com.toasterofbread.spmp.ui.layout.mainpage.MainPage
import com.toasterofbread.spmp.ui.layout.mainpage.MainPageState
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerOverlayPage
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.ResizableOutlinedTextField
import com.toasterofbread.utils.composable.WidthShrinkText

enum class LibrarySubPage { SONGS }

fun LibrarySubPage?.getReadable(): String =
    getString(when (this) {
        null -> "library_main_page_title"
        LibrarySubPage.SONGS -> "library_songs_page_title"
    })

class LibraryPage(state: MainPageState): MainPage(state) {
    enum class Tab {
        PLAYLISTS, SONGS, ALBUMS, ARTISTS, HISTORY;

        fun getIcon(): ImageVector =
            when (this) {
                PLAYLISTS -> MediaItemType.PLAYLIST_ACC.getIcon()
                SONGS -> MediaItemType.SONG.getIcon()
                ALBUMS -> Icons.Default.Album
                ARTISTS -> MediaItemType.ARTIST.getIcon()
                HISTORY -> Icons.Default.History
            }

        fun getReadable(): String =
            getString(when (this) {
                PLAYLISTS -> "library_tab_playlists"
                SONGS -> "library_tab_local_songs"
                ALBUMS -> "library_tab_albums"
                ARTISTS -> "library_tab_artists"
                HISTORY -> "library_tab_history"
            })
    }
    var current_tab: Tab by mutableStateOf(Tab.values().first())

    private var show_search_field: Boolean by mutableStateOf(false)
    var search_filter: String? by mutableStateOf(null)

    private var show_sort_option_menu: Boolean by mutableStateOf(false)
    var sort_option: MediaItemSortOption by mutableStateOf(MediaItemSortOption.PLAY_COUNT)
    var reverse_sort: Boolean by mutableStateOf(false)

    override fun onOpened() {
        show_search_field = false
        search_filter = null
        show_sort_option_menu = false
        sort_option = MediaItemSortOption.PLAY_COUNT
        reverse_sort = false
        current_tab = Tab.values().first()
    }

    @Composable
    override fun showTopBarContent(): Boolean = true

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    override fun TopBarContent(modifier: Modifier, close: () -> Unit) {
        MediaItemSortOption.SelectionMenu(
            show_sort_option_menu,
            sort_option,
            { show_sort_option_menu = false },
            {
                if (it == sort_option) {
                    reverse_sort = !reverse_sort
                }
                else {
                    sort_option = it
                }
            }
        )

        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keyboard_controller = LocalSoftwareKeyboardController.current
            Crossfade(show_search_field) { searching ->
                IconButton({
                    if (searching) {
                        keyboard_controller?.hide()
                    }
                    show_search_field = !searching
                }) {
                    Icon(
                        if (searching) Icons.Default.Close else Icons.Default.Search,
                        null
                    )
                }
            }

            Row(Modifier.fillMaxWidth().weight(1f)) {
                AnimatedVisibility(show_search_field, enter = fadeIn() + expandHorizontally(clip = false)) {
                    ResizableOutlinedTextField(
                        search_filter ?: "",
                        { search_filter = it },
                        Modifier.height(45.dp).fillMaxWidth().weight(1f),
                        singleLine = true
                    )
                }

                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (tab in Tab.values().withIndex()) {
                        Crossfade(tab.value == current_tab) { selected ->
                            Box(
                                Modifier
                                    .fillMaxWidth(
                                        1f / (Tab.values().size - tab.index)
                                    )
                                    .padding(horizontal = 5.dp)
                            ) {
                                ElevatedFilterChip(
                                    selected,
                                    {
                                        current_tab = tab.value
                                    },
                                    {
                                        Box(Modifier.fillMaxWidth().padding(end = 8.dp), contentAlignment = Alignment.Center) {
                                            Icon(tab.value.getIcon(), null, Modifier.requiredSizeIn(minWidth = 20.dp, minHeight = 20.dp))
                                        }
                                    },
                                    colors = with(Theme) {
                                        FilterChipDefaults.elevatedFilterChipColors(
                                            containerColor = background,
                                            labelColor = on_background,
                                            selectedContainerColor = accent,
                                            selectedLabelColor = on_accent
                                        )
                                    },
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Theme.on_background
                                    )
                                )
                            }
                        }
                    }
                }
            }

            IconButton({
                show_sort_option_menu = !show_sort_option_menu
            }) {
                Icon(Icons.Default.Sort, null)
            }
        }
    }

    @Composable
    override fun Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier, 
        content_padding: PaddingValues, 
        close: () -> Unit
    ) {
        val player = LocalPlayerState.current
        val downloads = rememberSongDownloads()

        Crossfade(current_tab, modifier) { tab ->
            when (tab) {
//                null -> LibraryMainPage(
//                    content_padding,
//                    multiselect_context,
//                    downloads,
//                    { subpage = it },
//                ) { songs, song, index ->
//                    onSongClicked(songs, player, song, index)
//                }

                Tab.PLAYLISTS -> LibraryPlaylistsPage(
                    content_padding,
                    multiselect_context,
                    { current_tab = it }
                )

                Tab.SONGS -> LibrarySongsPage(
                    content_padding,
                    multiselect_context,
                    downloads,
                    { current_tab = it }
                ) { songs, song, index ->
                    onSongClicked(songs, player, song, index)
                }

                Tab.ALBUMS -> TODO()
                Tab.ARTISTS -> TODO()
                Tab.HISTORY -> TODO()
            }
        }
    }

    @Composable
    private fun ButtonsRow(ytm_auth: YoutubeMusicAuthInfo, large: Boolean, modifier: Modifier = Modifier) {
        val player = LocalPlayerState.current
        var show_login_confirmation by remember { mutableStateOf(false) }
        if (show_login_confirmation) {
            YoutubeMusicLoginConfirmation { manual ->
                show_login_confirmation = false
                if (manual != null) {
                    player.setOverlayPage(PlayerOverlayPage.YtmLoginPage(manual))
                }
            }
        }

        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LibraryButton(Icons.Default.Settings, large) {
                player.setOverlayPage(PlayerOverlayPage.SettingsPage)
            }

            LibraryButton(Icons.Default.PlaylistPlay, large) {
                player.setOverlayPage(PlayerOverlayPage.PlaylistsPage)
            }

            LibraryButton(Icons.Default.MusicNote, large) {
//                    player.setOverlayPage(PlayerOverlayPage.SettingsPage)
            }

            LibraryButton(Icons.Default.Person, large) {
                val channel = ytm_auth.getOwnChannelOrNull()
                if (channel != null) {
                    player.openMediaItem(channel)
                }
                else {
                    show_login_confirmation = true
                }
            }

            AnimatedVisibility(ytm_auth.is_initialised) {
                LibraryButton(Icons.Default.PersonRemove, large) {
                    Settings.KEY_YTM_AUTH.set(YoutubeMusicAuthInfo())
                }
            }
        }
    }

    @Composable
    private fun ProfilePreview(ytm_auth: YoutubeMusicAuthInfo, modifier: Modifier = Modifier) {
        require(ytm_auth.is_initialised)

        Row(
            modifier.height(70.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ytm_auth.own_channel.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape)
            )

            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WidthShrinkText(
                    ytm_auth.own_channel.Title.get(SpMp.context.database) ?: "---",
                    Modifier.fillMaxHeight().weight(1f),
                    style = MaterialTheme.typography.headlineSmall
                )

                ButtonsRow(ytm_auth, false)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LibraryButton(icon: ImageVector, large: Boolean = false, onClick: () -> Unit) {
        ElevatedCard(
            onClick,
            Modifier.size(if (large) 80.dp else 35.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null)
            }
        }
    }
}

@Composable
fun LibraryPage(
    content_padding: PaddingValues,
    modifier: Modifier = Modifier,
    outer_multiselect_context: MediaItemMultiSelectContext? = null,
    close: () -> Unit
) {
    // TODO Remove
//    val player = LocalPlayerState.current
//    val multiselect_context = remember(outer_multiselect_context) { outer_multiselect_context ?: MediaItemMultiSelectContext {} }
//    val downloads = rememberSongDownloads()
//
//    var subpage: LibrarySubPage? by remember { mutableStateOf(null) }
//    BackHandler(subpage != null) {
//        subpage = null
//    }
//
//    Crossfade(subpage, modifier) { page ->
//        when (page) {
//            null -> LibraryMainPage(
//                content_padding,
//                multiselect_context,
//                downloads,
//                { subpage = it },
//            ) { songs, song, index ->
//                onSongClicked(songs, player, song, index)
//            }
//
//            LibrarySubPage.SONGS -> LibrarySongsPage(
//                content_padding,
//                multiselect_context,
//                downloads,
//                { subpage = it }
//            ) { songs, song, index ->
//                onSongClicked(songs, player, song, index)
//            }
//        }
//    }
}

private fun onSongClicked(songs: List<Song>, player: PlayerState, song: Song, index: Int) {
    player.withPlayer {
        // TODO Config
        val ADD_BEFORE = 2
        val ADD_AFTER = 7

        val add_songs = songs
            .mapIndexedNotNull { song_index, song ->
                if (song_index < index && index - song_index > ADD_BEFORE) {
                    return@mapIndexedNotNull null
                }

                if (song_index > index && song_index - index > ADD_AFTER) {
                    return@mapIndexedNotNull null
                }

                song
            }

        val song_index = minOf(ADD_BEFORE, index)
        assert(add_songs[song_index] == song)

        clearQueue(save = false)
        addMultipleToQueue(add_songs)
        seekToSong(song_index)
    }
}
