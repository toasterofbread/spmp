package com.spectre7.spmp.model

import androidx.compose.runtime.Composable

abstract class Previewable {
    @Composable
    abstract fun getPreview()
    abstract fun getId(): String
}