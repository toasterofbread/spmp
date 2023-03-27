package com.spectre7.spmp.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.api.getSongLiked
import com.spectre7.spmp.api.setSongLiked
import com.spectre7.spmp.model.Song
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.vibrateShort
import kotlin.concurrent.thread

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikeDislikeButton(
    song: Song,
    modifier: Modifier = Modifier,
    colour: Color = LocalContentColor.current
) {
    var loaded: Boolean by remember { mutableStateOf(false) }
    var liked: Boolean? by remember { mutableStateOf(null) }
    LaunchedEffect(song) {
        loaded = false
        thread {
            liked = getSongLiked(song.id).getOrThrowHere()
            loaded = true
        }
    }

    val rotation = remember { Animatable(0f) }
    OnChangedEffect(liked) {
        rotation.animateTo(if (liked == false) 180f else 0f)
    }

    fun setLiked(value: Boolean?) {
        if (!loaded) {
            return
        }

        thread {
            if (setSongLiked(song.id, value).isSuccess) {
                liked = value
            }
            else {
                liked = getSongLiked(song.id).getOrThrowHere()
            }
        }
    }

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        Crossfade(liked != null) { active ->
            Icon(
                if (active) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                null,
                Modifier
                    .rotate(rotation.value)
                    .combinedClickable(
                        onClick = { when (liked) {
                            true -> setLiked(null)
                            false -> setLiked(null)
                            null -> setLiked(true)
                        }},
                        onLongClick = {
                            when (liked) {
                                true -> setLiked(false)
                                false -> setLiked(true)
                                null -> setLiked(false)
                            }
                            vibrateShort()
                        }
                    ),
                tint = colour
            )
        }
    }
}
