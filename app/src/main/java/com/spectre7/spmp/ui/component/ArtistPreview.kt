@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)

package com.spectre7.spmp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Song
import com.spectre7.utils.setAlpha

@Composable
fun ArtistPreviewSquare(
    artist: Artist, 
    content_colour: Color, 
    modifier: Modifier = Modifier, 
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    var show_popup by remember { mutableStateOf(false) }

    Column(
        modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (onClick != null) {
                        onClick()
                    }
                },
                onLongClick = {
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        show_popup = true
                    }
                }
            )
            .aspectRatio(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LongPressIconMenu(
            showing = show_popup,
            onDismissRequest = {
                show_popup = false
            },
            media_item = artist,
            _thumb_size = 100.dp,
            thumb_shape = CircleShape,
            actions = { } // TODO
        )

        Text(
            artist.name,
            fontSize = 12.sp,
            color = content_colour,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistPreviewLong(
    artist: Artist, 
    content_colour: Color, 
    modifier: Modifier = Modifier, 
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    var show_popup by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick ?: {},
                onLongClick = onLongClick
            )
    ) {
        LongPressIconMenu(
            showing = show_popup,
            onDismissRequest = {
                show_popup = false
            },
            media_item = artist,
            _thumb_size = 40.dp,
            thumb_shape = CircleShape,
            actions = { } // TODO
        )

        Column(Modifier.padding(8.dp)) {
            Text(
                artist.name,
                fontSize = 15.sp,
                color = content_colour,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${artist.getFormattedSubscriberCount()} subscribers",
                fontSize = 12.sp,
                color = content_colour.setAlpha(0.5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
