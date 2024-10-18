package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getWidgetCategoryItems
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_widget
import spmp.shared.generated.resources.s_cat_widget

class WidgetSettings(
    val context: AppContext
): SettingsGroup("WIDGET", context.getPrefs()) {
    val DEFAULT_BASE_WIDGET_CONFIGURATION: PreferencesProperty<BaseWidgetConfiguration> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { BaseWidgetConfiguration() }
    )

    val DEFAULT_TYPE_WIDGET_CONFIGURATIONS: PreferencesProperty<Map<SpMpWidgetType, TypeWidgetConfiguration<out TypeWidgetClickAction>>> by serialisableProperty(
        getName = { "" },
        getDescription = { null },
        getDefaultValue = { emptyMap() }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_widget)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_widget)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.Widgets

    override fun showPage(exporting: Boolean): Boolean =
        Platform.ANDROID.isCurrent()

    override fun getConfigurationItems(): List<SettingsItem> = getWidgetCategoryItems(context)
}
