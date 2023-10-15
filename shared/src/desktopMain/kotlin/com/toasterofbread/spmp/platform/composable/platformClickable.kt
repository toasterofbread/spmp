package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import com.toasterofbread.utils.common.thenIf
import com.toasterofbread.utils.common.thenWithNotNull

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.platformClickable(enabled: Boolean, onClick: (() -> Unit)?, onAltClick: (() -> Unit)?, indication: Indication?): Modifier =
    this
        .thenWithNotNull(onClick) {
            onClick(onClick = it)
        }
        .thenWithNotNull(onAltClick) {
            onClick(
                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                onClick = it
            )
        }
