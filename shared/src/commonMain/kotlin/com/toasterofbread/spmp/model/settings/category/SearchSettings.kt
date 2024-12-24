package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getSearchCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_search_search_for_non_music
import spmp.shared.generated.resources.s_sub_search_search_for_non_music

class SearchSettings(val context: AppContext): SettingsGroupImpl("SEARCH", context.getPrefs()) {
    val SEARCH_FOR_NON_MUSIC: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_search_search_for_non_music) },
        getDescription = { stringResource(Res.string.s_sub_search_search_for_non_music) },
        getDefaultValue = { false }
    )

    @Composable
    override fun getTitle(): String = ""

    @Composable
    override fun getDescription(): String = ""

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Search

    override fun getConfigurationItems(): List<SettingsItem> = getSearchCategoryItems(context)

    override val hidden: Boolean = true
}
