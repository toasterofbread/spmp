package com.spectre7.spmp.platform.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
actual fun rememberImagePainter(url: String): Painter = com.lt.load_the_image.rememberImagePainter(url)
