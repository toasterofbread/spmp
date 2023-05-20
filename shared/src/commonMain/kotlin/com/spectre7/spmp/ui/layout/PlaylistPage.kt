@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.spectre7.spmp.api.durationToString
import com.spectre7.spmp.model.*
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.component.SONG_THUMB_CORNER_ROUNDING
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread

@Composable
fun PlaylistPage(
    pill_menu: PillMenu,
    playlist: Playlist,
    previous_item: MediaItem? = null,
    close: () -> Unit
) {
    val status_bar_height = SpMp.context.getStatusBarHeight()
    var accent_colour: Color? by remember { mutableStateOf(null) }
    val player = LocalPlayerState.current

    LaunchedEffect(playlist) {
        accent_colour = null

        if (playlist.feed_layouts == null) {
            thread {
                val result = playlist.loadData()
                result.fold(
                    { playlist ->
                        if (playlist == null) {
                            SpMp.error_manager.onError("PlaylistPageLoad", Exception("loadData result is null"))
                        }
                    },
                    { error ->
                        SpMp.error_manager.onError("PlaylistPageLoad", error)
                    }
                )
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(top = status_bar_height), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (previous_item != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(close) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))
                previous_item.title!!.also { Text(it) }
                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ player.showLongPressMenu(previous_item) }) {
                    Icon(Icons.Default.MoreVert, null)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                PlaylistTopInfo(playlist, accent_colour) {
                    if (accent_colour == null) {
                        accent_colour = playlist.getDefaultThemeColour() ?: Theme.current.accent
                    }
                }
            }

            playlist.feed_layouts?.also { layouts ->
                val layout = layouts.single()

                item {
                    Row(Modifier.fillMaxWidth().padding(top = 15.dp), verticalAlignment = Alignment.Bottom) {
                        val total_duration_text = remember(playlist.total_duration) {
                            if (playlist.total_duration == null) ""
                            else durationToString(playlist.total_duration!!, SpMp.ui_language, false)
                        }

                        Text(
                            "${(playlist.item_count ?: layout.items.size) + 1}曲 $total_duration_text",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.width(50.dp))
                        Spacer(Modifier.fillMaxWidth().weight(1f))

                        playlist.artist?.title?.also { artist ->
                            Marquee(arrangement = Arrangement.End) {
                                Text(
                                    artist,
                                    Modifier.clickable { player.onMediaItemClicked(playlist.artist!!) },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                items(layout.items.size) { i ->
                    val item = layout.items[i]
                    check(item is Song)

                    Row(
                        Modifier.fillMaxWidth().clickable { player.onMediaItemClicked(item) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, Modifier.size(50.dp).clip(RoundedCornerShape(SONG_THUMB_CORNER_ROUNDING)))
                        Text(
                            item.title!!,
                            Modifier.fillMaxWidth().weight(1f),
                            style = MaterialTheme.typography.titleSmall
                        )

                        val duration_text = remember(item.duration!!) {
                            durationToString(item.duration!!, SpMp.ui_language, true)
                        }

                        Text(duration_text, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTopInfo(playlist: Playlist, accent_colour: Color?, onThumbLoaded: (ImageBitmap) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val player = LocalPlayerState.current

    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {

        var thumb_size by remember { mutableStateOf(IntSize.Zero) }
        playlist.Thumbnail(
            MediaItemThumbnailProvider.Quality.HIGH,
            Modifier.fillMaxWidth(0.5f).aspectRatio(1f).clip(shape).onSizeChanged {
                thumb_size = it
            },
            onLoaded = onThumbLoaded
        )

        Column(Modifier.height(with(LocalDensity.current) { thumb_size.height.toDp() })) {
            Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                Text(
                    playlist.title!!,
                    style = MaterialTheme.typography.headlineSmall,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton({ TODO() }) {
                    Icon(Icons.Default.Radio, null)
                }
                IconButton({ TODO() }) {
                    Icon(Icons.Default.Shuffle, null)
                }
                Crossfade(playlist.pinned_to_home) { pinned ->
                    IconButton({ playlist.setPinnedToHome(!pinned) }) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }
                if (SpMp.context.canShare()) {
                    IconButton({ SpMp.context.shareText(playlist.url, playlist.title!!) }) {
                        Icon(Icons.Default.Share, null)
                    }
                }
            }

            Button(
                { player.playMediaItem(playlist) },
                Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour ?: Theme.current.accent,
                    contentColor = accent_colour?.getContrasted() ?: Theme.current.on_accent
                ),
                shape = shape
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Text(getString("playlist_chip_play"))
            }
        }
    }
}
