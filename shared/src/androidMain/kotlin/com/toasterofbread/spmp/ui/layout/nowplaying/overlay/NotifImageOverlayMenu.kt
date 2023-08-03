package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.getMediaNotificationImageMaxOffset
import com.toasterofbread.spmp.platform.getMediaNotificationImageSize
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.launchSingle
import kotlin.math.roundToInt
import kotlin.math.roundToLong

actual fun notifImageOverlayMenuButtonText(): String? = getString("song_notif_image_menu_open")

actual class NotifImageOverlayMenu: OverlayMenu() {
    @Composable
    override fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (OverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val song = getSong()
            val thumbnail = getCurrentSongThumb()
            val coroutine_scope = rememberCoroutineScope()

            var size_and_max_offset: Triple<ImageBitmap, IntSize, IntOffset>? by remember { mutableStateOf(null) }
            val offset_x = remember { Animatable(0f) }
            val offset_y = remember { Animatable(0f) }

            LaunchedEffect(thumbnail) {
                size_and_max_offset = thumbnail?.let {
                    val image = it.asAndroidBitmap()
                    Triple(
                        it,
                        getMediaNotificationImageSize(image),
                        getMediaNotificationImageMaxOffset(image)
                    )
                }
            }

            Crossfade(size_and_max_offset) { state ->
                if (state == null) {
                    SubtleLoadingIndicator()
                    return@Crossfade
                }

                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    val (image, image_size, max_offset) = state
                    val shape = RoundedCornerShape(16.dp)

                    Text(
                        getString("song_notif_image_menu_adjust_offset"),
                        Modifier.padding(bottom = 10.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(getString("song_notif_image_menu_adjust_offset_desc"))

                    Spacer(Modifier.height(35.dp))

                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(image_size.width / image_size.height.toFloat())
                            .border(1.dp, LocalContentColor.current, shape)
                            .clip(shape)
                            .pointerInput(Unit) {
                                detectDragGestures() { _, drag_amount ->
                                    coroutine_scope.launchSingle {
                                        // TODO scale by image size
                                        val max_x = max_offset.x.toFloat()
                                        val max_y = max_offset.y.toFloat()

                                        offset_x.snapTo((offset_x.value - drag_amount.x).coerceIn(-max_x, max_x))
                                        offset_y.snapTo((offset_y.value - drag_amount.y).coerceIn(-max_y, max_y))
                                    }
                                }
                            }
                    ) {
                        drawImage(
                            image,
                            srcSize = image_size,
                            srcOffset = IntOffset(
                                (image.width - image_size.width) / 2 + offset_x.value.roundToInt(),
                                (image.height - image_size.height) / 2 + offset_y.value.roundToInt()
                            ),
                            dstSize = IntSize(
                                size.width.toInt(),
                                size.height.toInt()
                            )
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton({
                            coroutine_scope.launchSingle {
                                offset_x.animateTo(0f)
                                offset_y.animateTo(0f)
                            }
                        }) {
                            Icon(Icons.Default.Refresh, null)
                        }
                        IconButton({
                            openMenu(null)
                            SpMp.context.database.songQueries.updateNotifImageOffsetById(
                                offset_x.value.takeIf { it != 0f }?.roundToLong(),
                                offset_y.value.takeIf { it != 0f }?.roundToLong(),
                                song.id
                            )
                        }) {
                            Icon(Icons.Default.Done, null)
                        }
                    }
                }
            }
        }
    }

    override fun closeOnTap(): Boolean = false
}
