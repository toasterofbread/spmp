@file:OptIn(ExperimentalMaterialApi::class)

package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    reverse_direction: Boolean,
    interaction_source: MutableInteractionSource?
): Modifier = swipeable(
    state = state,
    anchors = anchors,
    thresholds = thresholds,
    orientation = orientation,
    reverseDirection = reverse_direction,
    interactionSource = interaction_source
)
