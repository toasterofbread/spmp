package com.toasterofbread.spmp.model.settings.category

import com.toasterofbread.spmp.model.settings.SettingsGroup
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.commonsettings.impl.group.impl.ComposeKitSettingsGroupInterfaceImpl

class InterfaceSettings(
    context: AppContext
): ComposeKitSettingsGroupInterfaceImpl("INTERFACE", context.getPrefs()), SettingsGroup