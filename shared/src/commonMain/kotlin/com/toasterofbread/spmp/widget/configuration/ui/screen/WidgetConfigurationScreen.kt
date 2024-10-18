package com.toasterofbread.spmp.widget.configuration.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import dev.toastbits.composekit.navigation.Screen
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.utils.common.copy
import dev.toastbits.composekit.utils.common.thenIf
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_button_cancel
import spmp.shared.generated.resources.widget_config_button_done
import spmp.shared.generated.resources.widget_config_button_set_as_default
import spmp.shared.generated.resources.`widget_config_details_$type`
import spmp.shared.generated.resources.`widget_config_details_$type_$id`
import spmp.shared.generated.resources.widget_config_details_base
import spmp.shared.generated.resources.widget_config_title
import spmp.shared.generated.resources.widget_config_type_name_common

class WidgetConfigurationScreen(
    initial_base_configuration: BaseWidgetConfiguration?,
    initial_type_configuration: TypeWidgetConfiguration<out TypeWidgetClickAction>?,
    private val context: AppContext,
    private val widget_type: SpMpWidgetType?,
    private val widget_id: Int?,
    private val onCancel: () -> Unit,
    private val onDone: (BaseWidgetConfiguration?, TypeWidgetConfiguration<out TypeWidgetClickAction>?) -> Unit,
    private val onSetDefaultBaseConfiguration: ((BaseWidgetConfiguration) -> Unit)? = null,
    private val onSetDefaultTypeConfiguration: ((TypeWidgetConfiguration<out TypeWidgetClickAction>) -> Unit)? = null
): Screen {
    private val list_state: LazyListState = LazyListState()
    private var base_configuration: BaseWidgetConfiguration? by mutableStateOf(initial_base_configuration)
    private var type_configuration: TypeWidgetConfiguration<out TypeWidgetClickAction>? by mutableStateOf(initial_type_configuration)

    @Composable
    override fun Content(navigator: Navigator, modifier: Modifier, contentPadding: PaddingValues) {
        Column(modifier) {
            LazyColumn(
                Modifier.fillMaxHeight().weight(1f),
                state = list_state,
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = contentPadding.copy(bottom = 25.dp)
            ) {
                item {
                    Column {
                        Text(
                            stringResource(Res.string.widget_config_title),
                            style = MaterialTheme.typography.headlineMedium
                        )

                        val subtitle: String =
                            if (widget_type != null && widget_id != null)
                                stringResource(Res.string.`widget_config_details_$type_$id`)
                                    .replace("\$type", widget_type.defaultConfiguration.getTypeName())
                                    .replace("\$id", widget_id.toString())
                            else if (widget_type != null)
                                stringResource(Res.string.`widget_config_details_$type`)
                                    .replace("\$type", widget_type.defaultConfiguration.getTypeName())
                            else
                                stringResource(Res.string.widget_config_details_base)

                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.5f)
                        )
                    }
                }

                ConfigurationItems(context)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding.copy(top = contentPadding.calculateBottomPadding())),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                OutlinedButton(onCancel) {
                    Text(stringResource(Res.string.widget_config_button_cancel))
                }

                Button({
                    onDone(base_configuration, type_configuration)
                }) {
                    Text(stringResource(Res.string.widget_config_button_done))
                }
            }
        }
    }

    private fun LazyListScope.ConfigurationItems(
        context: AppContext,
        item_modifier: Modifier = Modifier
    ) {
        type_configuration?.also { configuration ->
            item {
                ItemHeading(
                    configuration.getTypeName(),
                    true,
                    onSetAsDefault = onSetDefaultTypeConfiguration?.let { lambda -> {
                        lambda(configuration)
                    } }
                )
            }
            with (configuration) {
                ConfigurationItems(context, item_modifier) {
                    type_configuration = it
                }
            }
        }

        base_configuration?.also { configuration ->
            item {
                ItemHeading(
                    stringResource(Res.string.widget_config_type_name_common),
                    onSetAsDefault = onSetDefaultBaseConfiguration?.let { lambda -> {
                        lambda(configuration)
                    } }
                )
            }
            with (configuration) {
                ConfigurationItems(context, item_modifier) {
                    base_configuration = it
                }
            }
        }
    }

    @Composable
    private fun ItemHeading(
        name: String,
        first: Boolean = false,
        onSetAsDefault: (() -> Unit)? = null
    ) {
        Row(
            Modifier
                .alpha(0.5f)
                .thenIf(!first) {
                    padding(top = 25.dp)
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides LocalApplicationTheme.current.accent) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge
                )

                HorizontalDivider(Modifier.fillMaxWidth().weight(1f))

                if (onSetAsDefault != null) {
                    OutlinedButton(
                        onSetAsDefault,
                        Modifier.height(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(stringResource(Res.string.widget_config_button_set_as_default))
                    }
                }
            }
        }
    }
}
