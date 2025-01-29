package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getExperimentalCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_experimental
import spmp.shared.generated.resources.s_cat_experimental
import spmp.shared.generated.resources.s_key_android_monet_colour_enable
import spmp.shared.generated.resources.s_sub_android_monet_colour_enable

class ExperimentalSettings(
    val context: AppContext
): SettingsGroupImpl("EXPERIMENTAL", context.getPrefs()) {
    val ANDROID_MONET_COLOUR_ENABLE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_android_monet_colour_enable) },
        getDescription = { stringResource(Res.string.s_sub_android_monet_colour_enable) },
        getDefaultValue = { false }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_experimental)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_experimental)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Science

    override fun getConfigurationItems(): List<SettingsItem> = getExperimentalCategoryItems(context)
}
