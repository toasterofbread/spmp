package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerticalSplit
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLayoutCategoryItems
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.composekit.settings.ui.SettingsPage
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import com.toasterofbread.composekit.settings.ui.item.SettingsItem

data object LayoutSettings: SettingsCategory("player") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_layout"),
            getString("s_cat_desc_layout"),
            { getLayoutCategoryItems() }
        ) { Icons.Outlined.VerticalSplit }

    enum class Key: SettingsKey {
        // Map of LayoutSlot to int where values are:
        // - Positive = InternalContentBar index + 1
        // - Negative = CUSTOM_BARS index + 1
        // - Zero = none
        PORTRAIT_SLOTS,
        LANDSCAPE_SLOTS,

        // Map of LayoutSlot to string where values are either:
        // - Hex colours starting with '#'
        // - Or an integer index of Theme.Colour
        SLOT_COLOURS,

        // List of serialised CustomBars
        CUSTOM_BARS;

        override val category: SettingsCategory get() = PlayerSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                PORTRAIT_SLOTS -> "{}"
                LANDSCAPE_SLOTS -> "{}"
                SLOT_COLOURS -> "{}"
                CUSTOM_BARS -> "[]"
            } as T
    }
}
