package com.toasterofbread.spmp.widget

import androidx.glance.appwidget.GlanceAppWidget

abstract class SpMpWidget: GlanceAppWidget() {
    val widget_type: SpMpWidgetType = SpMpWidgetType.entries.first { it.widgetClass == this::class }
}
