package com.spectre7.spmp.ui.component

import LocalPlayerState
import SpMp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.ArtistSubscribeButton
import com.spectre7.utils.composable.WidthShrinkText
import com.spectre7.utils.isDebugBuild

const val ARTIST_THUMB_CORNER_ROUNDING = 50

@Composable
fun ArtistPreviewSquare(
    artist: Artist,
    params: MediaItemPreviewParams
) {
    val long_press_menu_data = remember(artist) {
        getArtistLongPressMenuData(artist, multiselect_context = params.multiselect_context)
    }

    Column(
        params.modifier.mediaItemPreviewInteraction(artist, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            artist.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                contentColourProvider = params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(artist, Modifier.fillMaxSize())
            }
        }

        Text(
            artist.title ?: "",
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistPreviewLong(
    artist: Artist,
    params: MediaItemPreviewParams
) {
    val long_press_menu_data = remember(artist) {
        getArtistLongPressMenuData(artist, multiselect_context = params.multiselect_context)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier.mediaItemPreviewInteraction(artist, long_press_menu_data)
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            artist.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                    .size(40.dp)
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(artist, Modifier.fillMaxSize())
            }
        }

        Column(Modifier.padding(8.dp)) {
            Text(
                artist.title ?: "",
                fontSize = 15.sp,
                color = params.contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val sub_count = artist.getReadableSubscriberCount()
            if (sub_count.isNotEmpty()) {
                Text(
                    sub_count,
                    Modifier.alpha(0.5f),
                    fontSize = 12.sp,
                    color = params.contentColour?.invoke() ?: Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun getArtistLongPressMenuData(
    artist: Artist,
    thumb_shape: Shape? = RoundedCornerShape(ARTIST_THUMB_CORNER_ROUNDING),
    multiselect_context: MediaItemMultiSelectContext? = null
): LongPressMenuData {
    return LongPressMenuData(
        artist,
        thumb_shape,
        { ArtistLongPressMenuInfo(artist, it) },
        getString("lpm_long_press_actions"),
        multiselect_context = multiselect_context,
        sideButton = { modifier, background, accent ->
            ArtistSubscribeButton(
                artist,
                modifier = modifier
            )
        }
    ) { item, _ ->
        ArtistLongPressPopupActions(item)
    }
}

@Composable
private fun LongPressMenuActionProvider.ArtistLongPressPopupActions(artist: MediaItem) {
    require(artist is Artist)

    // TODO | Should radio actions be replaced with shuffle?

    ActionButton(
        Icons.Default.PlayArrow, 
        getString("lpm_action_play"), 
        onClick = {
            TODO() // Play songs
        },
        onLongClick = {
            TODO() // Play radio
        }
    )

    ActiveQueueIndexAction(
        { distance ->
            getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString()) 
        },
        onClick = { active_queue_index ->
            TODO() // Insert songs
        },
        onLongClick = { active_queue_index ->
            TODO() // Insert radio
        }
    )

    val player = LocalPlayerState.current
    ActionButton(Icons.Default.Person, getString("lpm_action_open_artist"), onClick = {
        player.openMediaItem(artist)
    })
}

@Composable
private fun ColumnScope.ArtistLongPressMenuInfo(artist: Artist, accent_colour: Color) {
    @Composable
    fun Item(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent_colour)
            WidthShrinkText(text, fontSize = 15.sp)
        }
    }
    @Composable
    fun Item() {
        Spacer(Modifier.height(25.dp)) // TODO
    }

    Item(Icons.Default.PlayArrow, getString("lpm_action_radio"))
    Item(Icons.Default.SubdirectoryArrowRight, getString("lpm_action_radio_after_x_songs"))

    Spacer(
        Modifier
            .fillMaxHeight()
            .weight(1f)
    )

    Row(Modifier.requiredHeight(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            getString("lpm_info_id").replace("\$id", artist.id),
            Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        SpMp.context.CopyShareButtons { artist.id }
    }

    if (isDebugBuild()) {
        Item(Icons.Default.Print, getString("lpm_action_print_info"), Modifier.clickable {
            println(artist)
        })
    }
}
