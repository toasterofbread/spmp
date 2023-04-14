package com.spectre7.spmp.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()

actual fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(this.asAndroidBitmap(), x, y, width, height).asImageBitmap()

actual fun ImageBitmap.getPixel(x: Int, y: Int): Color =
    Color(asAndroidBitmap().getPixel(x, y))

actual fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap =
    Bitmap.createScaledBitmap(this.asAndroidBitmap(), width, height, false).asImageBitmap()