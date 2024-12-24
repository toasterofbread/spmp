package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.getMediaNotificationImageMaxOffset
import com.toasterofbread.spmp.platform.playerservice.getMediaNotificationImageSize
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.song_notif_image_menu_open
import spmp.shared.generated.resources.song_notif_image_menu_adjust_offset
import spmp.shared.generated.resources.song_notif_image_menu_adjust_offset_desc

@Composable
actual fun notifImagePlayerOverlayMenuButtonText(): String? = stringResource(Res.string.song_notif_image_menu_open)

actual class NotifImagePlayerOverlayMenu: PlayerOverlayMenu() {
    @Composable
    override fun Menu(
        getSong: () -> Song?,
        getExpansion: () -> Float,
        openMenu: (PlayerOverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?,
    ) {
        val song: Song = getSong() ?: return
        val player: PlayerState = LocalPlayerState.current

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val thumbnail: ImageBitmap? = getCurrentSongThumb()
            val coroutine_scope: CoroutineScope = rememberCoroutineScope()

            var size_and_max_offset: Triple<ImageBitmap, IntSize, IntOffset>? by remember { mutableStateOf(null) }
            val song_notif_offset: IntOffset? by song.NotificationImageOffset.observe(player.database)

            val offset_x: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
            val offset_y: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }

            LaunchedEffect(song_notif_offset) {
                offset_x.animateTo(song_notif_offset?.x?.toFloat() ?: 0f)
                offset_y.animateTo(song_notif_offset?.y?.toFloat() ?: 0f)
            }

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

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)) {
                    val (image, image_size, max_offset) = state
                    val shape = RoundedCornerShape(16.dp)

                    Text(
                        stringResource(Res.string.song_notif_image_menu_adjust_offset),
                        Modifier.padding(bottom = 10.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(stringResource(Res.string.song_notif_image_menu_adjust_offset_desc))

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

                            val x = offset_x.value.takeIf { it != 0f }?.roundToInt()
                            val y = offset_y.value.takeIf { it != 0f }?.roundToInt()

                            song.NotificationImageOffset.set(
                                if (x != null || y != null) IntOffset( x ?: 0, y ?: 0)
                                else null,
                                player.database
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
