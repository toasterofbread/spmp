package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.model.mediaitem.artist.*
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitles
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.seekToPreviousOrRepeat
import com.toasterofbread.spmp.service.playercontroller.*
import com.toasterofbread.spmp.ui.component.MediaItemTitleEditDialog
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.nowplaying.*
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.components.utils.composable.Marquee
import dev.toastbits.composekit.components.utils.modifier.bounceOnClick
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_cancel

private const val TITLE_FONT_SIZE_SP: Float = 21f
private const val ARTIST_FONT_SIZE_SP: Float = 12f

@Composable
fun PlayerButton(
    image: ImageVector,
    size: Dp = 60.dp,
    enabled: Boolean = true,
    getBackgroundColour: PlayerState.() -> Color = { getNPBackground() },
    getOnBackgroundColour: PlayerState.() -> Color = { getNPOnBackground() },
    getAccentColour: (PlayerState.() -> Color)? = null,
    onClick: () -> Unit
) {
    val player: PlayerState = LocalPlayerState.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .bounceOnClick()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = enabled
            )
    ) {
        val painter: VectorPainter = rememberVectorPainter(image)
        val np_theme_mode: ThemeMode by player.settings.Theme.NOWPLAYING_THEME_MODE.observe()

        Canvas(
            Modifier
                .requiredSize(size)
                // https://stackoverflow.com/a/67820996
                .graphicsLayer { alpha = 0.99f }
        ) {
            with(painter) {
                draw(this@Canvas.size)
            }

            val gradient_end: Float
            val gradient_colours: List<Color>

            val accent: Color? = if (np_theme_mode != ThemeMode.NONE) getAccentColour?.invoke(player) else null
            if (accent != null) {
                gradient_end = this@Canvas.size.width * 0.95f
                gradient_colours = listOf(getOnBackgroundColour(player), getOnBackgroundColour(player), accent)
            }
            else {
                gradient_end = this@Canvas.size.width * 1.9f
                gradient_colours = listOf(getOnBackgroundColour(player), getBackgroundColour(player))
            }

            drawRect(
                Brush.linearGradient(
                    gradient_colours,
                    end = Offset(gradient_end, gradient_end)
                ),
                blendMode = BlendMode.SrcAtop
            )
        }
    }
}

@Composable
internal fun Controls(
    song: Song?,
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    button_row_arrangement: Arrangement.Horizontal = Arrangement.Center,
    seek_bar_next_to_buttons: Boolean = false,
    disable_text_marquees: Boolean = false,
    vertical_arrangement: Arrangement.Vertical = Arrangement.spacedBy(25.dp),
    title_font_size: TextUnit = TITLE_FONT_SIZE_SP.sp,
    artist_font_size: TextUnit = ARTIST_FONT_SIZE_SP.sp,
    text_align: TextAlign = TextAlign.Center,
    title_text_max_lines: Int = 1,
    button_size: Dp = 60.dp,
    getBackgroundColour: PlayerState.() -> Color = { getNPBackground() },
    getOnBackgroundColour: PlayerState.() -> Color = { getNPOnBackground() },
    getAccentColour: (PlayerState.() -> Color)? = null,
    buttonRowStartContent: @Composable RowScope.() -> Unit = {},
    buttonRowEndContent: @Composable RowScope.() -> Unit = {},
    artistRowStartContent: @Composable RowScope.() -> Unit = {},
    artistRowEndContent: @Composable RowScope.() -> Unit = {},
    textRowStartContent: @Composable RowScope.() -> Unit = {}
) {
    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

    val song_title: String? by song?.observeActiveTitle()
    val song_artist_titles: List<String?>? = song?.Artists?.observePropertyActiveTitles()

    var show_title_edit_dialog: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        show_title_edit_dialog = false
    }

    if (show_title_edit_dialog && song != null) {
        MediaItemTitleEditDialog(song) { show_title_edit_dialog = false }
    }

    Column(modifier, verticalArrangement = vertical_arrangement) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            textRowStartContent()

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Marquee(Modifier.fillMaxWidth(), disable = disable_text_marquees) {
                    Text(
                        song_title ?: "",
                        fontSize = title_font_size,
                        color = getOnBackgroundColour(player),
                        textAlign = text_align,
                        maxLines = title_text_max_lines,
                        // Using ellipsis makes this go weird, no clue why
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .platformClickable(
                                enabled = enabled,
                                onAltClick = {
                                    show_title_edit_dialog = !show_title_edit_dialog
                                    player.context.vibrateShort()
                                }
                            )
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    var selecting_artists: List<Artist>? by remember { mutableStateOf(null) }
                    selecting_artists?.also { artists ->
                        AlertDialog(
                            onDismissRequest = {
                                selecting_artists = null
                            },
                            confirmButton = {
                                Button({
                                    selecting_artists = null
                                }) {
                                    Text(stringResource(Res.string.action_cancel))
                                }
                            },
                            title = {
                                Text(stringResource(MediaItemType.ARTIST.getReadable(true)))
                            },
                            text = {
                                CompositionLocalProvider(LocalPlayerClickOverrides provides click_overrides.copy(
                                    onClickOverride = { item, _ ->
                                        player.openMediaItem(item)
                                        selecting_artists = null
                                    },
                                    onAltClickOverride = { item, p2 ->
                                        click_overrides.onMediaItemAltClicked(item, player, p2)
                                        selecting_artists = null
                                    }
                                )) {
                                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        for (artist in artists) {
                                            MediaItemPreviewLong(artist)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    artistRowStartContent()
                    Spacer(Modifier)
                    Text(
                        song_artist_titles?.let { formatArtistTitles(it, player.context) } ?: "",
                        fontSize = artist_font_size,
                        color = getOnBackgroundColour(player).copy(alpha = 0.5f),
                        textAlign = text_align,
                        maxLines = 1,
                        softWrap = false,
                        // Using ellipsis makes this go weird, no clue why
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .platformClickable(
                                enabled = enabled,
                                onClick = {
                                    val artists: List<Artist> = song?.Artists?.get(player.database)?.filter { !it.isForItem() } ?: return@platformClickable
                                    if (artists.size == 1) {
                                        click_overrides.onMediaItemClicked(artists.first(), player)
                                    }
                                    else if (artists.size > 1) {
                                        selecting_artists = artists
                                    }
                                },
                                onAltClick = {
                                    val artist: Artist? = song?.Artists?.get(player.database)?.singleOrNull()
                                    if (artist?.isForItem() == false) {
                                        click_overrides.onMediaItemAltClicked(artist, player)
                                        player.context.vibrateShort()
                                    }
                                }
                            )
                    )
                    Spacer(Modifier)
                    artistRowEndContent()
                }
            }
        }

        val getSeekBarTrackColour: PlayerState.() -> Color = {
            getOnBackgroundColour(this).copy(alpha = 0.1f)
        }

        if (!seek_bar_next_to_buttons) {
            SeekBar(seek, getColour = getOnBackgroundColour, getTrackColour = getSeekBarTrackColour, enabled = enabled && player.status.m_duration_ms > 0)
        }

        Row(
            horizontalArrangement = button_row_arrangement,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            buttonRowStartContent()

            // Previous
            PlayerButton(
                Icons.Rounded.SkipPrevious,
                enabled = enabled && player.status.m_has_previous,
                size = button_size,
                getBackgroundColour = getBackgroundColour,
                getOnBackgroundColour = getOnBackgroundColour,
                getAccentColour = getAccentColour
            ) {
                player.controller?.seekToPreviousOrRepeat()
            }

            // Play / pause
            PlayerButton(
                if (player.status.m_playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                enabled = enabled && song != null,
                size = button_size + 15.dp,
                getBackgroundColour = getBackgroundColour,
                getOnBackgroundColour = getOnBackgroundColour,
                getAccentColour = getAccentColour
            ) {
                player.controller?.playPause()
            }

            // Next
            PlayerButton(
                Icons.Rounded.SkipNext,
                enabled = enabled && player.status.m_has_next,
                size = button_size,
                getBackgroundColour = getBackgroundColour,
                getOnBackgroundColour = getOnBackgroundColour,
                getAccentColour = getAccentColour
            ) {
                player.controller?.seekToNext()
            }

            if (seek_bar_next_to_buttons) {
                SeekBar(
                    seek,
                    Modifier.fillMaxWidth().weight(1f).padding(start = 10.dp),
                    getColour = getOnBackgroundColour,
                    getTrackColour = getSeekBarTrackColour,
                    enabled = enabled && player.status.m_duration_ms > 0
                )
            }

            buttonRowEndContent()
        }
    }
}

private operator fun Size.plus(size: Size): Size =
    Size(width + size.width, height + size.height)
