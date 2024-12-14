package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.util.isJa
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.seekToPreviousOrRepeat
import dev.toastbits.composekit.components.utils.composable.RowOrColumn
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.top_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar

@Composable
internal fun NowPlayingMainTabPage.NowPlayingMainTabNarrow(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, vertical: Boolean, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val song: Song? by player.status.song_state

    RowOrColumn(
        row = !vertical,
        alignment = 0,
        modifier = modifier
            .requiredHeight(page_height)
            .padding(content_padding)
            .padding(horizontal = horizontal_padding)
            .padding(top = top_padding, bottom = bottom_padding)
    ) {
        val spacing: Dp = 10.dp

        if (vertical) {
            song?.Thumbnail(ThumbnailProvider.Quality.LOW, Modifier.aspectRatio(1f)) {
                onThumbnailLoaded(song, it)
            }
            Spacer(Modifier.height(spacing))

            player.PlayButton()
            player.NextButton()
            player.PreviousButton()
            Spacer(Modifier.height(spacing))
        }
        else {
            player.PreviousButton()
            player.PlayButton()
            player.NextButton()
            Spacer(Modifier.width(spacing))

            song?.Thumbnail(ThumbnailProvider.Quality.LOW, Modifier.aspectRatio(1f)) {
                onThumbnailLoaded(song, it)
            }
            Spacer(Modifier.width(spacing))
        }

        val active_title: String? by song?.observeActiveTitle()
        active_title?.also { title ->
            for (segment in title.split(' ')) {
                if (vertical && segment.all { it.isJa() }) {
                    var offset: Dp = 0.dp
                    for (c in segment) {
                        Text(
                            c.toString(),
                            Modifier
                                .offset(0.dp, offset)
                                .thenIf(c == 'ー') {
                                    rotate(-90f)
                                },
                            style = MaterialTheme.typography.titleLarge
                        )
                        offset -= 5.dp
                    }
                }
                else {
                    Text(
                        segment,
                        Modifier.run {
                            if (vertical) rotate(90f).vertical()
                            else this
                        },
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        Spacer(Modifier.fillMaxSize().weight(1f))
    }
}

fun Modifier.vertical() = layout { measurable, constraints ->
    val placeable: Placeable = measurable.measure(constraints)
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2)
        )
    }
}

@Composable
private fun PlayerState.PlayButton() {
    PlayerButton(
        if (status.m_playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
        enabled = status.m_song != null,
        size = 75.dp
    ) {
        controller?.playPause()
    }
}

@Composable
private fun PlayerState.NextButton() {
    PlayerButton(
        Icons.Rounded.SkipNext,
        enabled = status.m_has_next,
        size = 60.dp
    ) {
        controller?.seekToNext()
    }
}

@Composable
private fun PlayerState.PreviousButton() {
    PlayerButton(
        Icons.Rounded.SkipPrevious,
        enabled = status.m_has_previous,
        size = 60.dp
    ) {
        controller?.seekToPreviousOrRepeat()
    }
}
