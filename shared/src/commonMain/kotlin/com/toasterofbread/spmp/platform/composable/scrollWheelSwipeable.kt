@file:OptIn(ExperimentalMaterialApi::class)

package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.ThresholdConfig
import androidx.compose.ui.Modifier

expect fun Modifier.scrollWheelSwipeable(
    state: SwipeableState<Int>,
    anchors: Map<Float, Int>,
    thresholds: (from: Int, to: Int) -> ThresholdConfig,
    orientation: Orientation,
    reverseDirection: Boolean
): Modifier