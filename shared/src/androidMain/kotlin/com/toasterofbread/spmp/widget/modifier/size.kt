package com.toasterofbread.spmp.widget.modifier

import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.layout.size

fun GlanceModifier.size(size: DpSize): GlanceModifier =
    size(size.width, size.height)
