@file:OptIn(ExperimentalMaterialApi::class)

package com.spectre7.spmp.platform.composable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.ThresholdConfig
import androidx.compose.material.swipeable
import androidx.compose.ui.Modifier

actual fun Modifier.scrollWheelSwipeable(
    state: SwipeableState<Int>,
    anchors: Map<Float, Int>,
    thresholds: (from: Int, to: Int) -> ThresholdConfig,
    orientation: Orientation,
    reverseDirection: Boolean
): Modifier = swipeable(state = state, anchors = anchors, thresholds = thresholds, orientation = orientation, reverseDirection = reverseDirection)
