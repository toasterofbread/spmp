package com.toasterofbread.spmp.ui.layout.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortOption
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.MainPage
import com.toasterofbread.spmp.ui.layout.mainpage.MainPageState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.ResizableOutlinedTextField

interface LibrarySubPage {
    fun getIcon(): ImageVector
    fun getTitle(): String

    @Composable
    fun Page(
        library_page: LibraryPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier
    )
}

class LibraryPage(state: MainPageState): MainPage(state) {
    val tabs: List<LibrarySubPage> = listOf(
        LibraryPlaylistsPage(), LibrarySongsPage()
    )
    var current_tab: LibrarySubPage by mutableStateOf(tabs.first())

//    enum class Tab {
//        PLAYLISTS, ALBUMS, ARTISTS, HISTORY;
//
//        fun getIcon(): ImageVector =
//            when (this) {
//                ALBUMS -> Icons.Default.Album
//                ARTISTS -> MediaItemType.ARTIST.getIcon()
//                HISTORY -> Icons.Default.History
//            }
//
//        fun getReadable(): String =
//            getString(when (this) {
//                ALBUMS -> "library_tab_albums"
//                ARTISTS -> "library_tab_artists"
//                HISTORY -> "library_tab_history"
//            })
//    }

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
        current_tab = tabs.first()
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
                    for (tab in tabs.withIndex()) {
                        Crossfade(tab.value == current_tab) { selected ->
                            Box(
                                Modifier
                                    .fillMaxWidth(
                                        1f / (tabs.size - tab.index)
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
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        Crossfade(current_tab, modifier) { tab ->
            tab.Page(
                this@LibraryPage,
                content_padding,
                multiselect_context,
                Modifier
            )
        }
    }

//    @Composable
//    private fun ButtonsRow(ytm_auth: YoutubeMusicAuthInfo, large: Boolean, modifier: Modifier = Modifier) {
//        val player = LocalPlayerState.current
//        var show_login_confirmation by remember { mutableStateOf(false) }
//        if (show_login_confirmation) {
//            YoutubeMusicLoginConfirmation { manual ->
//                show_login_confirmation = false
//                if (manual != null) {
//                    player.setOverlayPage(PlayerOverlayPage.YtmLoginPage(manual))
//                }
//            }
//        }
//
//        Row(
//            modifier,
//            horizontalArrangement = Arrangement.spacedBy(10.dp)
//        ) {
//            LibraryButton(Icons.Default.Settings, large) {
//                player.setOverlayPage(PlayerOverlayPage.SettingsPage)
//            }
//
//            LibraryButton(Icons.Default.PlaylistPlay, large) {
//                player.setOverlayPage(PlayerOverlayPage.PlaylistsPage)
//            }
//
//            LibraryButton(Icons.Default.MusicNote, large) {
////                    player.setOverlayPage(PlayerOverlayPage.SettingsPage)
//            }
//
//            LibraryButton(Icons.Default.Person, large) {
//                val channel = ytm_auth.getOwnChannelOrNull()
//                if (channel != null) {
//                    player.openMediaItem(channel)
//                }
//                else {
//                    show_login_confirmation = true
//                }
//            }
//
//            AnimatedVisibility(ytm_auth.is_initialised) {
//                LibraryButton(Icons.Default.PersonRemove, large) {
//                    Settings.KEY_YTM_AUTH.set(YoutubeMusicAuthInfo())
//                }
//            }
//        }
//    }
//
//    @Composable
//    private fun ProfilePreview(ytm_auth: YoutubeMusicAuthInfo, modifier: Modifier = Modifier) {
//        require(ytm_auth.is_initialised)
//
//        Row(
//            modifier.height(70.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(10.dp)
//        ) {
//            ytm_auth.own_channel.Thumbnail(
//                MediaItemThumbnailProvider.Quality.LOW,
//                Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape)
//            )
//
//            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
//                WidthShrinkText(
//                    ytm_auth.own_channel.Title.get(SpMp.context.database) ?: "---",
//                    Modifier.fillMaxHeight().weight(1f),
//                    style = MaterialTheme.typography.headlineSmall
//                )
//
//                ButtonsRow(ytm_auth, false)
//            }
//        }
//    }
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    private fun LibraryButton(icon: ImageVector, large: Boolean = false, onClick: () -> Unit) {
//        ElevatedCard(
//            onClick,
//            Modifier.size(if (large) 80.dp else 35.dp)
//        ) {
//            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Icon(icon, null)
//            }
//        }
//    }
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
