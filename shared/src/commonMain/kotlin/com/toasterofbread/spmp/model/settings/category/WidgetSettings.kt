package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getWidgetCategoryItems
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.type.TypeWidgetConfig
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_widget
import spmp.shared.generated.resources.s_cat_widget

class WidgetSettings(
    val context: AppContext
): SettingsGroupImpl("WIDGET", context.getPrefs()) {
    val DEFAULT_BASE_WIDGET_CONFIGURATION: PlatformSettingsProperty<BaseWidgetConfig> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { BaseWidgetConfig() },
        json = SpMpWidgetConfiguration.json
    )

    val DEFAULT_TYPE_WIDGET_CONFIGURATIONS: PlatformSettingsProperty<Map<SpMpWidgetType, TypeWidgetConfig<out TypeWidgetClickAction>>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() },
        json = SpMpWidgetConfiguration.json
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_widget)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_widget)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Widgets

    override val hidden: Boolean = !Platform.ANDROID.isCurrent()

    override fun getConfigurationItems(): List<SettingsItem> = getWidgetCategoryItems(context)
}
