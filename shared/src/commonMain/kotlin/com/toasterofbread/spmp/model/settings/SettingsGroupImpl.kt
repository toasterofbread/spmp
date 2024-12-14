package com.toasterofbread.spmp.model.settings

import dev.toastbits.composekit.settings.ComposeKitSettingsGroupImpl
import dev.toastbits.composekit.settings.PlatformSettings

abstract class SettingsGroupImpl(
    groupKey: String,
    settings: PlatformSettings
): ComposeKitSettingsGroupImpl(groupKey, settings), SettingsGroup