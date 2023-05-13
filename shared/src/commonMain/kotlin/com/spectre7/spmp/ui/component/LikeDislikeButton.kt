package com.spectre7.spmp.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.api.getSongLiked
import com.spectre7.spmp.api.setSongLiked
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.composable.SubtleLoadingIndicator
import kotlin.concurrent.thread

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikeDislikeButton(
    song: Song,
    modifier: Modifier = Modifier,
    colourProvider: () -> Color
) {
    if (!DataApi.ytm_authenticated) {
        return
    }

    var loaded: Boolean by remember { mutableStateOf(false) }
    var liked: Boolean? by remember { mutableStateOf(null) }
    LaunchedEffect(song) {
        loaded = false
        thread {
            liked = getSongLiked(song.id).getOrNull()
            loaded = true
        }
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
                liked = getSongLiked(song.id).getOrNull()
            }
        }
    }

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        Crossfade(if (!loaded) null else liked != null) { active ->
            if (active == null) {
                SubtleLoadingIndicator(Modifier.size(24.dp), colourProvider)
            }
            else if (active == true) {
                val rotation by animateFloatAsState(if (liked == false) 180f else 0f)

                Icon(
                    if (active) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    null,
                    Modifier
                        .rotate(rotation)
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
                                SpMp.context.vibrateShort()
                            }
                        ),
                    tint = colourProvider()
                )
            }

        }
    }
}
