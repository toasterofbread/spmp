package com.spectre7.spmp.ui.layout.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.theme.Theme

enum class LibrarySubPage { SONGS }

fun LibrarySubPage?.getReadable(): String =
    getString(when (this) {
        null -> "library_page_title"
        LibrarySubPage.SONGS -> "library_songs_page_title"
    })

fun LibrarySubPage?.getIcon(): ImageVector =
    when (this) {
        null -> Icons.Default.LibraryMusic
        LibrarySubPage.SONGS -> Icons.Default.MusicNote
    }

@Composable
fun LibraryPage(
    pill_menu: PillMenu,
    bottom_padding: Dp,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    outer_multiselect_context: MediaItemMultiSelectContext? = null,
    close: () -> Unit
) {
    require(inline == (outer_multiselect_context != null))

    val multiselect_context = remember(outer_multiselect_context) { outer_multiselect_context ?: MediaItemMultiSelectContext {} }

    val downloads: MutableList<DownloadStatus> = remember { mutableStateListOf() }
    var subpage: LibrarySubPage? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        PlayerServiceHost.download_manager.getDownloads {
            downloads.addAll(it)
        }

        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: DownloadStatus) {
                downloads.add(status)
            }
            override fun onDownloadRemoved(id: String) {
                downloads.removeIf { it.id == id }
            }
            override fun onDownloadChanged(status: DownloadStatus) {
                for (i in downloads.indices) {
                    if (downloads[i].id == status.id) {
                        downloads[i] = status
                    }
                }
            }
        }
        PlayerServiceHost.download_manager.addDownloadStatusListener(listener)

        onDispose {
            PlayerServiceHost.download_manager.removeDownloadStatusListener(listener)
        }
    }

    BackHandler(subpage != null) {
        subpage = null
    }

    Crossfade(subpage) { page ->
        Column(
            modifier.run {
                if (!inline) padding(horizontal = 20.dp, vertical = 10.dp)
                else this
            },
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            // Title bar
            if (!inline) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(page.getIcon(), null)

                    Text(
                        page.getReadable(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Theme.current.on_background
                        )
                    )

                    Spacer(Modifier.width(24.dp))
                }
            }

            when (page) {
                null -> LibraryMainPage(downloads, multiselect_context, bottom_padding, inline) { subpage = it }
                LibrarySubPage.SONGS -> LibrarySongsPage(downloads, multiselect_context, bottom_padding, inline) { subpage = it }
            }
        }
    }
}
