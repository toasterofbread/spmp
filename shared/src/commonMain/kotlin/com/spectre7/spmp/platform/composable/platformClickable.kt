package com.spectre7.spmp.platform.composable

import androidx.compose.foundation.Indication
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.platformClickable(onClick: (() -> Unit)? = null, onAltClick: (() -> Unit)? = null, indication: Indication? = null): Modifier
