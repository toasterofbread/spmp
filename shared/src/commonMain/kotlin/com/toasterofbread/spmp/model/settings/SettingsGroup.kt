package com.toasterofbread.spmp.model.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ComposeKitSettingsGroup

interface SettingsGroup: ComposeKitSettingsGroup {
    val hidden: Boolean get() = false

    @Composable
    fun titleBarEndContent(modifier: Modifier) {}
}