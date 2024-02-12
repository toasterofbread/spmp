package com.toasterofbread.spmp.platform.playerservice

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import kotlin.math.roundToInt

private const val A13_MEDIA_NOTIFICATION_ASPECT = 2.9f / 5.7f

fun getMediaNotificationImageMaxOffset(image: Bitmap): IntOffset {
    val dimensions: IntSize = getMediaNotificationImageSize(image)
    return IntOffset(
        (image.width - dimensions.width) / 2,
        (image.height - dimensions.height) / 2
    )
}

fun getMediaNotificationImageSize(image: Bitmap): IntSize {
    val aspect: Float = if (Build.VERSION.SDK_INT >= 33) A13_MEDIA_NOTIFICATION_ASPECT else 1f
    if (image.width > image.height) {
        return IntSize(
            image.height,
            (image.height * aspect).roundToInt()
        )
    }
    else {
        return IntSize(
            image.width,
            (image.width * aspect).roundToInt()
        )
    }
}

internal fun formatMediaNotificationImage(
    image: Bitmap,
    song: Song,
    context: AppContext,
    ): Bitmap {
    val dimensions: IntSize = getMediaNotificationImageSize(image)
    val offset: IntOffset = song.NotificationImageOffset.get(context.database) ?: IntOffset.Zero

    return Bitmap.createBitmap(
        image,
        (((image.width - dimensions.width) / 2) + offset.x).coerceIn(0, image.width - dimensions.width),
        (((image.height - dimensions.height) / 2) + offset.y).coerceIn(0, image.height - dimensions.height),
        dimensions.width,
        dimensions.height
    )
}
