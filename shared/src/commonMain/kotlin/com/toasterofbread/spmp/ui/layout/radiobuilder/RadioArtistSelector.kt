package com.toasterofbread.spmp.ui.layout.radiobuilder

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.getArtistThumbShape
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.components.utils.composable.crossOut
import dev.toastbits.composekit.util.composable.OnChangedEffect
import dev.toastbits.composekit.util.composable.times
import dev.toastbits.ytmkt.endpoint.RadioBuilderArtist
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.radio_builder_next_button
import dev.toastbits.ytmkt.model.external.ThumbnailProvider as YtmThumbnailProvider

@Composable
internal fun RadioArtistSelector(
    radio_artists: List<RadioBuilderArtist>?,
    pill_menu: PillMenu,
    modifier: Modifier = Modifier,
    onFinished: (List<Int>?) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val form_factor: FormFactor by FormFactor.observe()
    val selected_artists: MutableList<Int> = remember { mutableStateListOf() }

    DisposableEffect(Unit) {
        val actions = pill_menu.run {
            listOf(
                addExtraAction(false) {
                    IconButton({ onFinished(null) }) {
                        Icon(Icons.Filled.Close, null, tint = content_colour)
                    }
                },
                addExtraAction(false) {
                    IconButton({ selected_artists.clear() }) {
                        Icon(Icons.Filled.Refresh, null, tint = content_colour)
                    }
                },
                addExtraAction(false) {
                    Button(
                        { if (selected_artists.isNotEmpty()) onFinished(selected_artists) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = content_colour
                        ),
                        contentPadding = PaddingValues(0.dp, 0.dp)
                    ) {
                        Text(stringResource(Res.string.radio_builder_next_button), Modifier.crossOut(selected_artists.isEmpty(), { content_colour }) { it * 1.2f })
                    }
                }
            )
        }

        onDispose {
            for (action in actions) {
                pill_menu.removeExtraAction(action)
            }
        }
    }

    Crossfade(radio_artists) { artists ->
        if (artists == null) {
            Box(modifier, contentAlignment = Alignment.Center) {
                SubtleLoadingIndicator(getColour = { player.theme.onBackground })
            }
        }
        else {
            val selected_border_size: Dp = 10.dp
            val thumb_size: Dp = if (form_factor.is_large) 120.dp else 80.dp
            val artist_padding: Dp = if (form_factor.is_large) 30.dp else 10.dp

            LazyVerticalGrid(
                with (LocalDensity.current) {
                    GridCells.FixedSize(thumb_size +  14.sp.toDp() + (artist_padding * 2) + 5.dp)
                },
                modifier,
                horizontalArrangement = Arrangement.Center
            ) {
                itemsIndexed(artists) { index, radio_artist ->
                    val artist: ArtistData = remember(radio_artist, index) {
                        ArtistData("RB$index").apply {
                            name = radio_artist.name
                            thumbnail_provider = YtmThumbnailProvider.fromThumbnails(listOf(radio_artist.thumbnail))
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        val selected: Boolean by remember { derivedStateOf { selected_artists.contains(index) } }
                        val border_expansion: Animatable<Float, AnimationVector1D> = remember { Animatable(if (selected) 1f else 0f) }

                        OnChangedEffect(selected) {
                            border_expansion.animateTo(if (selected) 1f else 0f)
                        }

                        val long_press_menu_data = remember(artist) {
                            artist.getLongPressMenuData()
                        }

                        Column(
                            Modifier
                                .padding(artist_padding, 0.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (!selected_artists.remove(index)) {
                                            selected_artists.add(index)
                                        }
                                    },
                                    onLongClick = {
                                        player.showLongPressMenu(long_press_menu_data)
                                    }
                                )
                                .aspectRatio(0.8f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.size(thumb_size + selected_border_size), contentAlignment = Alignment.Center) {
                                if (border_expansion.value > 0f) {
                                    Box(
                                        Modifier
                                            .size(thumb_size + selected_border_size * border_expansion.value)
                                            .border(1.dp, player.theme.onBackground, getArtistThumbShape())
                                    )
                                }

                                artist.Thumbnail(
                                    YtmThumbnailProvider.Quality.LOW,
                                    Modifier
                                        .longPressMenuIcon(long_press_menu_data)
                                        .size(thumb_size),
                                    provider_override = artist.thumbnail_provider,
                                    disable_cache = true
                                )
                            }

                            Text(
                                artist.name ?: "",
                                fontSize = 12.sp,
                                color = player.theme.onBackground,
                                maxLines = 1,
                                lineHeight = 14.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
