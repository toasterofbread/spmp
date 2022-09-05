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
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.MainActivity

@Composable
fun SongPreview (song: Song, large: Boolean) {

    if (large) {
        Column(
            modifier = Modifier.padding(10.dp, 0.dp).clickable {
                MainActivity.player.interact {
                    it.addToQueue(song)
                }
            }
        ) {

            Image(
                painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10))
            )

            Column() {
                Text(
                    song.getTitle(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
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
            modifier = Modifier.padding(10.dp, 0.dp)
        ) {

            Image(
                painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20))
            )

            Column(Modifier.padding(10.dp).fillMaxWidth(0.9f)) {
                Text(
                    song.getTitle(),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    song.artist.nativeData.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = {
                MainActivity.player.interact {
                    it.addToQueue(song) {
                        it.play()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, null, Modifier, MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}