package com.toasterofbread.spmp.widget.modifier

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius

@Composable
fun GlanceModifier.systemCornerRadius(): GlanceModifier {
    if (android.os.Build.VERSION.SDK_INT >= 31) {
        val systemCornerRadiusDefined: Boolean =
            androidx.glance.LocalContext.current.resources.getResourceName(android.R.dimen.system_app_widget_background_radius) != null
        if (systemCornerRadiusDefined) {
            return cornerRadius(android.R.dimen.system_app_widget_background_radius)
        }
    }
    return this
}
