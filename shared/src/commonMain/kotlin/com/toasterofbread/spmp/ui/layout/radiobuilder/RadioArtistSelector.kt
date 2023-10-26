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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.getArtistThumbShape
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import com.toasterofbread.spmp.youtubeapi.RadioBuilderArtist
import com.toasterofbread.toastercomposetools.utils.common.times
import com.toasterofbread.toastercomposetools.utils.composable.OnChangedEffect
import com.toasterofbread.toastercomposetools.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.toastercomposetools.utils.composable.crossOut

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
                SubtleLoadingIndicator(getColour = { player.theme.on_background })
            }
        } 
        else {
            val thumb_size = 80.dp
            val selected_border_size = 10.dp

            LazyVerticalGrid(GridCells.Fixed(3), modifier) {
                itemsIndexed(artists) { index, radio_artist ->
                    val artist = remember(radio_artist, index) {
                        ArtistData("RB$index").apply {
                            title = radio_artist.name
                            thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(listOf(radio_artist.thumbnail))
                        }
                    }

                    LaunchedEffect(artist) {
                        artist.saveToDatabase(player.database)
                    }

                    Box(contentAlignment = Alignment.Center) {
                        val selected by remember { derivedStateOf { selected_artists.contains(index) } }
                        val border_expansion = remember { Animatable(if (selected) 1f else 0f) }

                        OnChangedEffect(selected) {
                            border_expansion.animateTo(if (selected) 1f else 0f)
                        }

                        val long_press_menu_data = remember(artist) {
                            artist.getLongPressMenuData()
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
                                            .border(1.dp, player.theme.on_background, getArtistThumbShape())
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
                                color = player.theme.on_background,
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
