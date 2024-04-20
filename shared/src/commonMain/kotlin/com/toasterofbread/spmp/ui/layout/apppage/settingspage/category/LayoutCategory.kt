package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.getLayoutSlotEditorSettingsItems
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.settings.ui.item.SettingsItem

internal fun getLayoutCategoryItems(context: AppContext): List<SettingsItem> = getLayoutSlotEditorSettingsItems(context)
