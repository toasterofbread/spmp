package com.spectre7.spmp.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.PlayerHost
import com.spectre7.spmp.model.Song
import com.spectre7.utils.setAlpha

@Composable
fun SongPreview (song: Song, large: Boolean, colour: Color, modifier: Modifier = Modifier, basic: Boolean = false) {

    if (large) {
        Column(
            modifier.padding(10.dp, 0.dp).clickable {
                PlayerHost.service.playSong(song)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10))
            )

            Text(
                song.title,
                fontSize = 12.sp,
                color = colour,
                maxLines = 1,
                lineHeight = 14.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(10.dp, 0.dp)
        ) {

            Image(
                painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20))
            )

            Column(Modifier.padding(10.dp).fillMaxWidth(0.9f)) {
                Text(
                    song.title,
                    fontSize = 15.sp,
                    color = colour,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    song.artist.name,
                    fontSize = 11.sp,
                    color = colour.setAlpha(0.5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!basic) {
                IconButton(onClick = {
                    PlayerHost.service.addToQueue(song) {
                        PlayerHost.service.play()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier, colour)
                }
            }
        }
    }
}