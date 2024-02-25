package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerticalSplit
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLayoutCategoryItems
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.toasterofbread.composekit.settings.ui.SettingsPage
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import com.toasterofbread.composekit.settings.ui.item.SettingsItem

data object LayoutSettings: SettingsCategory("player") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_layout"),
            getString("s_cat_desc_layout"),
            { getLayoutCategoryItems() }
        ) { Icons.Outlined.VerticalSplit }

    enum class Key: SettingsKey {
        // Map of LayoutSlot to int where values are:
        // - Positive or zero = InternalContentBar index
        // - Negative = CUSTOM_BARS index + 1
        PORTRAIT_SLOTS,
        LANDSCAPE_SLOTS,

        // List of serialised CustomBars
        CUSTOM_BARS;

        override val category: SettingsCategory get() = PlayerSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                PORTRAIT_SLOTS -> Json.encodeToString(ContentBar.getDefaultPortraitSlots())
                LANDSCAPE_SLOTS -> Json.encodeToString(ContentBar.getDefaultLandscapeSlots())
                CUSTOM_BARS -> "[]"
            } as T
    }
}
