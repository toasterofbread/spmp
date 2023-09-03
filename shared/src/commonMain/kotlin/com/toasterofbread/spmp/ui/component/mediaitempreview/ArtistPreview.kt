package com.toasterofbread.spmp.ui.component.mediaitempreview

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuActionProvider
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.composable.WidthShrinkText

const val ARTIST_THUMB_CORNER_ROUNDING = 50

//@Composable
//fun ArtistPreviewSquare(
//    artist: Artist,
//    params: MediaItemPreviewParams
//) {
//    val long_press_menu_data = remember(artist) {
//        getArtistLongPressMenuData(artist, multiselect_context = params.multiselect_context)
//    }
//    MediaItemPreviewSquare(artist, params, long_press_menu_data)
//}
//
//@Composable
//fun ArtistPreviewLong(
//    artist: Artist,
//    params: MediaItemPreviewParams
//) {
//    val long_press_menu_data = remember(artist) {
//        getArtistLongPressMenuData(artist, multiselect_context = params.multiselect_context)
//    }
//
//    MediaItemPreviewLong(
//        artist,
//        params.modifier,
//        params.contentColour
//        long_press_menu_data
//    )
//}

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
        multiselect_context = multiselect_context
    )
}

@Composable
fun LongPressMenuActionProvider.ArtistLongPressMenuActions(artist: MediaItem) {
    require(artist is Artist)
    val player = LocalPlayerState.current

    ActionButton(
        Icons.Default.PlayArrow, 
        getString("lpm_action_play"), 
        onClick = {
            player.playMediaItem(artist)
        },
        onLongClick = {
            player.playMediaItem(artist, shuffle = true)
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

    ActionButton(Icons.Default.Person, getString("lpm_action_open_artist"), onClick = {
        player.openMediaItem(artist,)
    })
}

@Composable
private fun ColumnScope.ArtistLongPressMenuInfo(artist: Artist, getAccentColour: () -> Color) {
    @Composable
    fun Item(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = getAccentColour())
            WidthShrinkText(text, fontSize = 15.sp)
        }
    }

    Item(Icons.Default.PlayArrow, getString("lpm_action_radio"))
    Item(Icons.Default.SubdirectoryArrowRight, getString("lpm_action_radio_after_x_songs"))

    Spacer(
        Modifier
            .fillMaxHeight()
            .weight(1f)
    )
}
