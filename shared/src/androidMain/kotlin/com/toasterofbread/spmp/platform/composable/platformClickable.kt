package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.platformClickable(enabled: Boolean, onClick: (() -> Unit)?, onAltClick: (() -> Unit)?, indication: Indication?): Modifier =
    composed { combinedClickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = indication,
        onClick = onClick ?: {},
        onLongClick = onAltClick
    ) }

