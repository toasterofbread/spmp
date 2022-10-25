package com.spectre7.spmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.spectre7.spmp.model.Song
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerHost

@Composable
fun SongPreview (song: Song, large: Boolean, colour: Color, modifier: Modifier = Modifier, basic: Boolean = false) {

    if (large) {
        Column(
            modifier = modifier.padding(10.dp, 0.dp).clickable {
                PlayerHost.interactService {
                    it.playSong(song)
                    // it.addToQueue(song)
                }
            }
        ) {

            Image(
                painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10))
            )

            Column {
                Text(
                    song.title,
                    fontSize = 12.sp,
                    color = colour,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                    song.artist.nativeData.name,
                    fontSize = 11.sp,
                    color = colour.copy(alpha=0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!basic) {
                IconButton(onClick = {
                    PlayerHost.interactService {
                        it.addToQueue(song) {
                            it.play()
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier, colour)
                }
            }
        }
    }
}