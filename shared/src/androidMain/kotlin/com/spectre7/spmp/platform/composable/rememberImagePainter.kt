package com.spectre7.spmp.platform.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.rememberAsyncImagePainter

@Composable
actual fun rememberImagePainter(url: String): Painter = rememberAsyncImagePainter(url)
