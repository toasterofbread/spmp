package com.toasterofbread.spmp.ui.layout.radiobuilder

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.api.RadioBuilderArtist
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.ARTIST_THUMB_CORNER_ROUNDING
import com.toasterofbread.spmp.ui.component.mediaitempreview.getArtistLongPressMenuData
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.composable.crossOut
import com.toasterofbread.utils.times
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RadioArtistSelector(
    radio_artists: List<RadioBuilderArtist>?,
    pill_menu: PillMenu,
    modifier: Modifier = Modifier,
    onFinished: (List<Int>?) -> Unit
) {
    val selected_artists: MutableList<Int> = remember { mutableStateListOf() }
    val player = LocalPlayerState.current

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
                        Text(getString("radio_builder_next_button"), Modifier.crossOut(selected_artists.isEmpty(), { content_colour }) { it * 1.2f })
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
                SubtleLoadingIndicator(getColour = { Theme.on_background })
            }
        } else {
            val thumb_size = 80.dp
            val selected_border_size = 10.dp

            LazyVerticalGrid(GridCells.Fixed(3), modifier) {
                items(artists.size) { index ->

                    val radio_artist = artists[index]
                    val artist = remember(radio_artist) {
                        ArtistData(index.toString()).apply {
                            title = radio_artist.name
                            thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(listOf(radio_artist.thumbnail))
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        val selected by remember { derivedStateOf { selected_artists.contains(index) } }
                        val border_expansion = remember { Animatable(if (selected) 1f else 0f) }

                        OnChangedEffect(selected) {
                            border_expansion.animateTo(if (selected) 1f else 0f)
                        }

                        val long_press_menu_data = remember(artist) {
                            getArtistLongPressMenuData(artist)
                        }

                        Column(
                            modifier
                                .padding(10.dp, 0.dp)
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
                                            .border(1.dp, Theme.on_background, RoundedCornerShape(ARTIST_THUMB_CORNER_ROUNDING))
                                    )
                                }
                                artist.Thumbnail(
                                    MediaItemThumbnailProvider.Quality.LOW,
                                    Modifier
                                        .longPressMenuIcon(long_press_menu_data)
                                        .size(thumb_size)
                                )
                            }

                            Text(
                                artist.title ?: "",
                                fontSize = 12.sp,
                                color = Theme.on_background,
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
