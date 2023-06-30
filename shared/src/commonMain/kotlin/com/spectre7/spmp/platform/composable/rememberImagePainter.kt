package com.toasterofbread.spmp.platform.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
expect fun rememberImagePainter(url: String): Painter
