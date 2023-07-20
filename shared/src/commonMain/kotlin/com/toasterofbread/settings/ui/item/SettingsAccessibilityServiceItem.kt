package com.toasterofbread.composesettings.ui.item

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
        fun setEnabled(enabled: Boolean)
    }

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
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
                    Button({ service_bridge.setEnabled(!enabled) },
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
