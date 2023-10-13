package com.toasterofbread.composesettings.ui.item

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.ui.theme.Theme

class SettingsAccessibilityServiceItem(
    val enabled_text: String,
    val disabled_text: String,
    val enable_button: String,
    val disable_button: String,
    val service_bridge: AccessibilityServiceBridge
): SettingsItem() {
    interface AccessibilityServiceBridge {
        fun addEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext)
        fun removeEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext)
        fun isEnabled(context: PlatformContext): Boolean
        fun setEnabled(enabled: Boolean, context: PlatformContext)
    }

    override fun initialiseValueStates(prefs: PlatformPreferences, default_provider: (String) -> Any) {}
    override fun releaseValueStates(prefs: PlatformPreferences) {}

    override fun resetValues() {}

    @Composable
    override fun Item(
        theme: Theme,
        openPage: (Int, Any?) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        var service_enabled: Boolean by remember { mutableStateOf(service_bridge.isEnabled(context)) }
        val listener: (Boolean) -> Unit = { service_enabled = it }
        DisposableEffect(Unit) {
            service_bridge.addEnabledListener(listener, context)
            onDispose {
                service_bridge.removeEnabledListener(listener, context)
            }
        }

        Crossfade(service_enabled) { enabled ->
            CompositionLocalProvider(LocalContentColor provides if (enabled) theme.on_background else theme.on_accent) {
                Row(
                    Modifier
                        .background(
                            if (enabled) theme.background else theme.vibrant_accent,
                            SETTINGS_ITEM_ROUNDED_SHAPE
                        )
                        .border(Dp.Hairline, theme.vibrant_accent, SETTINGS_ITEM_ROUNDED_SHAPE)
                        .padding(start = 20.dp, end = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (enabled) enabled_text else disabled_text)
                    Button({ service_bridge.setEnabled(!enabled, context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) theme.vibrant_accent else theme.background,
                            contentColor = if (enabled) theme.on_accent else theme.on_background
                        )
                    ) {
                        Text(if (enabled) disable_button else enable_button)
                    }
                }
            }
        }
    }
}
