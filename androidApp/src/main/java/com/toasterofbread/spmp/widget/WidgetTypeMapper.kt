package com.toasterofbread.spmp.widget

import android.content.pm.ActivityInfo
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlin.reflect.KClass

internal fun SpMpWidgetType.getWidgetReceiverClass(): KClass<out GlanceAppWidgetReceiver> =
    when (this) {
        SpMpWidgetType.LYRICS_LINE_HORIZONTAL -> LyricsLineHorizontalWidgetReceiver::class
    }

internal fun getSpMpWidgetTypeForActivityInfo(activity_info: ActivityInfo): SpMpWidgetType =
    SpMpWidgetType.entries.firstOrNull { it.getWidgetReceiverClass().qualifiedName == activity_info.name }
        ?: throw RuntimeException("No SpMpWidgetType found for ${activity_info.name}")
