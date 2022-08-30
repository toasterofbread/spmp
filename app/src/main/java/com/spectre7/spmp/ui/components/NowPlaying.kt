package com.spectre7.spmp.ui.components

import android.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.Song
import kotlin.concurrent.thread

val default_image = BitmapFactory.decodeResource(MainActivity.instance!!.resources, R.drawable.ic_delete).asImageBitmap()

@Composable
fun NowPlaying(p_song: Song, p_playing: Boolean, p_position: Float) {

    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(p_song.getId()) {
        if (p_song.thumbnailLoaded()) {
            thumbnail = p_song.loadThumbnail()
        }
        else {
            thread {
                thumbnail = p_song.loadThumbnail()
            }
        }
    }

    Column(Modifier.padding(25.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
        Image(
            if (thumbnail != null) thumbnail!!.asImageBitmap() else default_image, "",
            contentScale = ContentScale.Crop,
            modifier = Modifier.aspectRatio(1.0f).clip(RoundedCornerShape(5))
        )

        Column() {
            Text(p_song.nativeData!!.title, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(p_song.artist.nativeData.name, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Row() {


        }

        var slider_moving by remember { mutableStateOf(false) }
        var slider_value by remember { mutableStateOf(0.0f) }
        var old_p_position by remember { mutableStateOf<Float?>(null) }

        LaunchedEffect(p_position) {
            if (!slider_moving && p_position != old_p_position) {
                slider_value = p_position
                old_p_position = null
            }
        }

        Slider(
            value = slider_value,
            onValueChange = { slider_moving = true; slider_value = it },
            onValueChangeFinished = {
                slider_moving = false
                old_p_position = p_position
                MainActivity.instance!!.player.interact {
                    it.player.seekTo((it.player.duration * slider_value).toLong())
                }
            }
        )

    }
}

@Composable
fun NowPlayingMinimised(p_song: Song?, p_playing: Boolean, p_position: Float) {
    Column(Modifier.fillMaxWidth()) {

        LinearProgressIndicator(progress = p_position, modifier = Modifier.requiredHeight(2.dp))

        Row(modifier = Modifier.fillMaxHeight().align(Alignment.CenterHorizontally)) {
            Text(
                if (p_song == null) "No song playing" else p_song.nativeData!!.title,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .animateContentSize()
            )

            IconButton(onClick = {
                MainActivity.instance!!.player.interact {
                    it.playPause()
                }
            }, modifier = Modifier.align(Alignment.CenterVertically)) {
                Image(
                    painterResource(if (p_playing) R.drawable.ic_media_pause else R.drawable.ic_media_play),
                    MainActivity.getString(if (p_playing) com.spectre7.spmp.R.string.media_pause else com.spectre7.spmp.R.string.media_play)
                )
            }
        }

    }
}

