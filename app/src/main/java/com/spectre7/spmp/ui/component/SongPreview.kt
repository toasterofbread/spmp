package com.spectre7.spmp.ui.component

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import com.spectre7.utils.Marquee
import com.spectre7.utils.getStatusBarHeight
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun SongPreviewSquare(
    song: Song, 
    content_colour: Color, 
    modifier: Modifier = Modifier, 
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    var show_popup by remember { mutableStateOf(false) }

    Column(
        modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { 
                    if (onClick != null) {
                        onClick()
                    }
                    else {
                        PlayerServiceHost.service.playSong(song)
                    }
                },
                onLongClick = {
                    if (onLongClick != null) {
                        onLongClick()
                    }
                    else {
                        show_popup = true
                    }
                }
            )
            .aspectRatio(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LongPressIconMenu(
            showing = show_popup,
            onDismissRequest = {
                show_popup = false
            },
            media_item = song,
            _thumb_size = 100.dp,
            thumb_shape = RoundedCornerShape(10),
            actions = longPressPopupActions
        )

        Text(
            song.title,
            fontSize = 12.sp,
            color = content_colour,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongPreviewLong(
    song: Song, 
    content_colour: Color, 
    modifier: Modifier = Modifier, 
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    var show_popup by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress
            )
    ) {
        LongPressIconMenu(
            showing = show_popup,
            onDismissRequest = {
                show_popup = false
            },
            media_item = song,
            _thumb_size = 40.dp,
            thumb_shape = RoundedCornerShape(20),
            actions = longPressPopupActions
        )

        Column(
            Modifier
                .padding(10.dp)
                .fillMaxWidth(0.9f)) {
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
                PlayerServiceHost.service.addToQueue(song) {
                    PlayerServiceHost.service.play()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, null, Modifier, colour)
            }
        }
    }
}

val longPressPopupActions: @Composable LongPressMenuActionProvider.(MediaItem) -> Unit = { song ->
    if (!song is Song) {
        throw ClassCastException()
    }

    ActionButton(Icons.Filled.PlayArrow, "Play") {
        PlayerServiceHost.service.playSong(song)
    }

    ActionButton(Icons.Filled.ArrowRightAlt, "Play next") { }

    val queue_song = PlayerServiceHost.service.getSong(PlayerServiceHost.service.active_queue_index)
    if (queue_song != null) {
        Column {
            Row {
                ActionButton(Icons.Filled.SubdirectoryArrowRight, "Play after", Modifier.fillMaxWidth().weight(1f)) {
                    PlayerServiceHost.service.addToQueue(song, PlayerServiceHost.service.active_queue_index + 1, true)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                    val button_padding = PaddingValues(0.dp)
                    val button_modifier = Modifier.size(30.dp)

                    Button(
                        {
                            PlayerServiceHost.service.updateActiveQueueIndex(1)
                        },
                        button_modifier,
                        contentPadding = button_padding
                    ) {
                        Text("+")
                    }
                    Button(
                        {
                            PlayerServiceHost.service.updateActiveQueueIndex(-1)
                        },
                        button_modifier,
                        contentPadding = button_padding
                    ) {
                        Text("-")
                    }
                }
            }

            queue_song.PreviewBasic(
                false,
                Modifier,
                MainActivity.theme.getOnBackground(false)
            )
        }
    }

    ActionButton(Icons.Filled.Download, "Download") { } // TODO
}
