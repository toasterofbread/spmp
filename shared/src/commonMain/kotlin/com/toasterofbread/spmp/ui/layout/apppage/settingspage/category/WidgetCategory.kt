package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig
import com.toasterofbread.spmp.widget.configuration.type.TypeWidgetConfig
import com.toasterofbread.spmp.widget.configuration.ui.screen.WidgetConfigurationScreen
import dev.toastbits.composekit.navigation.compositionlocal.LocalNavigator
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.components.platform.composable.ScrollBarLazyColumn
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ComposableSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.button_widget_settings_change_default_configurations
import spmp.shared.generated.resources.prompt_select_widget_type
import spmp.shared.generated.resources.prompt_select_widget_type_option_base
import spmp.shared.generated.resources.s_key_default_widget_configurations

internal fun getWidgetCategoryItems(context: AppContext): List<SettingsItem> =
    listOf(
        ComposableSettingsItem(resetComposeUiState = {}) {
            val navigator: Navigator = LocalNavigator.current
            val base_configuration: BaseWidgetConfig by context.settings.Widget.DEFAULT_BASE_WIDGET_CONFIGURATION.observe()
            val type_configurations: Map<SpMpWidgetType, TypeWidgetConfig<out TypeWidgetClickAction>> by context.settings.Widget.DEFAULT_TYPE_WIDGET_CONFIGURATIONS.observe()

            var show_type_selector: Boolean by remember { mutableStateOf(false) }

            if (show_type_selector) {
                WidgetTypeOrBaseSelector(
                    onSelected = { type ->
                        if (navigator.currentScreen is WidgetConfigurationScreen<*>) {
                            return@WidgetTypeOrBaseSelector
                        }

                        val config_screen: WidgetConfigurationScreen<*> =
                            WidgetConfigurationScreen(
                                if (type == null) base_configuration else null,
                                null,
                                if (type != null) type_configurations[type] ?: type.default_config else null,
                                null,
                                context,
                                type,
                                null,
                                onCancel = {
                                    navigator.navigateBackward()
                                },
                                onDone = { new_base, _, new_type, _ ->
                                    navigator.navigateBackward()

                                    context.coroutineScope.launch {
                                        if (new_base != null) {
                                            context.settings.Widget.DEFAULT_BASE_WIDGET_CONFIGURATION.set(new_base)
                                        }
                                        if (new_type != null) {
                                            context.settings.Widget.DEFAULT_TYPE_WIDGET_CONFIGURATIONS.set(
                                                type_configurations.toMutableMap().apply { set(type!!, new_type) }
                                            )
                                        }
                                    }
                                }
                            )
                        navigator.pushScreen(config_screen)
                    },
                    onCancelled = { show_type_selector = false }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.s_key_default_widget_configurations), Modifier.fillMaxWidth().weight(1f))
                Button({
                    show_type_selector = !show_type_selector
                }) {
                    Text(stringResource(Res.string.button_widget_settings_change_default_configurations))
                }
            }
        }
    )

@Composable
private fun WidgetTypeOrBaseSelector(
    onSelected: (SpMpWidgetType?) -> Unit,
    onCancelled: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelled,
        confirmButton = {
            Button(onCancelled) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        title = {
            Text(stringResource(Res.string.prompt_select_widget_type))
        },
        text = {
            val button_modifier: Modifier = Modifier.fillMaxWidth()

            ScrollBarLazyColumn {
                item {
                    Button(
                        {
                            onSelected(null)
                        },
                        button_modifier
                    ) {
                        Text(stringResource(Res.string.prompt_select_widget_type_option_base))
                    }
                }

                items(SpMpWidgetType.entries) { type ->
                    OutlinedButton(
                        {
                            onSelected(type)
                        },
                        button_modifier
                    ) {
                        Text(type.default_config.getTypeName())
                    }
                }
            }
        }
    )
}
