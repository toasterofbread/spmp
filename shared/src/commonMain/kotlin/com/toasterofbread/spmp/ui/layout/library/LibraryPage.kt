package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.rememberSongDownloads
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.YoutubeMusicLoginConfirmation
import com.toasterofbread.spmp.ui.layout.mainpage.MainPage
import com.toasterofbread.spmp.ui.layout.mainpage.MainPageState
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerOverlayPage
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.blendWith
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.contrastAgainst

enum class LibrarySubPage { SONGS }

fun LibrarySubPage?.getReadable(): String =
    getString(when (this) {
        null -> "library_main_page_title"
        LibrarySubPage.SONGS -> "library_songs_page_title"
    })

class LibraryPage(state: MainPageState): MainPage(state) {
    @Composable
    override fun Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier, 
        content_padding: PaddingValues, 
        close: () -> Unit
    ) {
        val ytm_auth by remember { YoutubeMusicAuthInfo.getSettingsValueState() }

        Column(modifier.padding(content_padding)) {
//            Crossfade(ytm_auth, Modifier.padding(bottom = 20.dp)) { auth ->
//                if (auth.is_initialised) {
//                    ProfilePreview(auth, Modifier.fillMaxWidth())
//                }
//                else {
//                    ButtonsRow(ytm_auth, true)
//                }
//            }

            ButtonsRow(ytm_auth, true, Modifier.padding(bottom = 20.dp))

            LibraryPage(PaddingValues(), Modifier, multiselect_context, close)
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
                    ytm_auth.own_channel.title ?: "---",
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
    val player = LocalPlayerState.current
    val multiselect_context = remember(outer_multiselect_context) { outer_multiselect_context ?: MediaItemMultiSelectContext {} }
    val downloads = rememberSongDownloads()

    var subpage: LibrarySubPage? by remember { mutableStateOf(null) }
    BackHandler(subpage != null) {
        subpage = null
    }

    Crossfade(subpage, modifier) { page ->
        when (page) {
            null -> LibraryMainPage(
                content_padding,
                multiselect_context,
                downloads,
                { subpage = it },
            ) { songs, song, index ->
                onSongClicked(songs, player, song, index)
            }

            LibrarySubPage.SONGS -> LibrarySongsPage(
                content_padding,    
                multiselect_context,
                downloads,
                { subpage = it }
            ) { songs, song, index ->
                onSongClicked(songs, player, song, index)
            }
        }
    }
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
