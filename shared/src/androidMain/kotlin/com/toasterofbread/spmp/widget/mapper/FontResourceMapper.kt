package com.toasterofbread.spmp.widget.mapper

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font

@Composable
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun Font.toAndroidTypeface(): Typeface =
    when (this) {
        is androidx.compose.ui.text.font.AndroidAssetFont ->
            this.typeface ?: this.loadCached(LocalContext.current)!!
        else -> throw NotImplementedError(this::class.toString())
    }
