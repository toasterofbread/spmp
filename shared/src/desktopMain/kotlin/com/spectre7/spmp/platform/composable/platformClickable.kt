package com.spectre7.spmp.platform.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import com.spectre7.utils.thenIf

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.platformClickable(onClick: (() -> Unit)?, onAltClick: (() -> Unit)?, indication: Indication?): Modifier =
    this.thenIf(onClick != null) { Modifier.onClick(onClick = onClick!!) }
        .thenIf(onAltClick != null) {
            Modifier.onClick(
                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                onClick = onAltClick!!
            )
        }
