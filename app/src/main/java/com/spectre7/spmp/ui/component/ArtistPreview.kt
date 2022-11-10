package com.spectre7.spmp.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.model.Artist

@Composable
fun ArtistPreview (artist: Artist, large: Boolean, colour: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(10.dp, 0.dp)
    ) {

        Image(
            painter = rememberAsyncImagePainter(artist.thumbnail_url),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        Column(Modifier.padding(10.dp).fillMaxWidth(0.9f)) {
            Text(
                artist.name,
                fontSize = 15.sp,
                color = colour,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${artist.getFormattedSubscriberCount()} subscribers",
                fontSize = 11.sp,
                color = colour.copy(alpha=0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Info, null, Modifier, colour)
        }
    }
}