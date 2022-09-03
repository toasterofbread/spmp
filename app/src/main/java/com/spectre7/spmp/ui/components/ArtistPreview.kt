package com.spectre7.spmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.model.Artist

@Composable
fun ArtistPreview (artist: Artist, large: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(10.dp, 0.dp)
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
                artist.nativeData.name,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${artist.getFormattedSubscriberCount()} subscribers",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Info, null, Modifier, MaterialTheme.colorScheme.onBackground)
        }
    }
}