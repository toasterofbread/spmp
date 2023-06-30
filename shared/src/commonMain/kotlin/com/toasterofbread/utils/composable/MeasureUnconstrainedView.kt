package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints

@Composable
fun MeasureUnconstrainedView(
    view_to_measure: @Composable () -> Unit,
    view_constraints: Constraints = Constraints(),
    content: @Composable (width: Int, height: Int) -> Unit,
) {
    SubcomposeLayout { constraints ->
        val measurement = subcompose("viewToMeasure", view_to_measure)[0].measure(view_constraints)

        val contentPlaceable = subcompose("content") {
            content(measurement.width, measurement.height)
        }[0].measure(constraints)

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}
