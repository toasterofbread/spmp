package com.toasterofbread.spmp.widget

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlin.reflect.KClass

internal fun SpMpWidgetType.getWidgetReceiverClass(): KClass<out GlanceAppWidgetReceiver> =
    when (this) {
        SpMpWidgetType.LYRICS_LINE_HORIZONTAL -> LyricsLineHorizontalWidgetReceiver::class
    }

internal fun getSpMpWidgetTypeForActivityInfo(provider: ComponentName): SpMpWidgetType =
    SpMpWidgetType.entries.firstOrNull { it.getWidgetReceiverClass().qualifiedName == provider.className }
        ?: throw RuntimeException("No SpMpWidgetType found for $provider")
