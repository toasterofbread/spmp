package com.spectre7.spmp.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.model.Artist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPreview (artist: Artist, large: Boolean, colour: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        onClick = {  },
        colors = CardDefaults.cardColors(Color.Unspecified)
    ) {
        Row(
            Modifier.padding(10.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = rememberAsyncImagePainter(artist.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )

            Column(Modifier.padding(10.dp)) {
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
                    color = colour.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}