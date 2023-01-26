package com.spectre7.spmp.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.model.Artist
import com.spectre7.utils.setAlpha

@Composable
fun ArtistPreviewSquare(
    song: Song, 
    content_colour: Color, 
    modifier: Modifier = Modifier, 
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPreview (artist: Artist, large: Boolean, colour: Color, modifier: Modifier = Modifier, icon_size: Dp = 40.dp, font_size: TextUnit = 15.sp) {
    Card(
        modifier = modifier,
        onClick = {  },
        colors = CardDefaults.cardColors(Color.Unspecified)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = rememberAsyncImagePainter(artist.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(icon_size)
                    .clip(CircleShape)
            )

            Column(Modifier.padding(8.dp)) {
                Text(
                    artist.name,
                    fontSize = font_size,
                    color = colour,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "${artist.getFormattedSubscriberCount()} subscribers",
                    fontSize = font_size * 0.75,
                    color = colour.setAlpha(0.5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}