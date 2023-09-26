package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.Indication
import androidx.compose.ui.Modifier

expect fun Modifier.platformClickable(
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onAltClick: (() -> Unit)? = null,
    indication: Indication? = null
): Modifier
