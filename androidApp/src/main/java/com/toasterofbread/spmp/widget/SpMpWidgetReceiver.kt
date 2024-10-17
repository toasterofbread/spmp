package com.toasterofbread.spmp.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

abstract class SpMpWidgetReceiver(type: SpMpWidgetType): GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = type.widgetClass.java.getDeclaredConstructor().newInstance()
}
