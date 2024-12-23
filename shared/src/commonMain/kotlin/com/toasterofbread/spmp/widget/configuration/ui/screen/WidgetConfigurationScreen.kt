package com.toasterofbread.spmp.widget.configuration.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.TypeConfigurationDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.TypeWidgetConfig
import dev.toastbits.composekit.navigation.screen.Screen
import dev.toastbits.composekit.theme.core.ui.LocalComposeKitTheme
import dev.toastbits.composekit.util.composable.copy
import dev.toastbits.composekit.util.thenIf
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_button_cancel
import spmp.shared.generated.resources.widget_config_button_done
import spmp.shared.generated.resources.widget_config_button_set_as_default
import spmp.shared.generated.resources.widget_config_button_use_default_value
import spmp.shared.generated.resources.`widget_config_details_$type`
import spmp.shared.generated.resources.`widget_config_details_$type_$id`
import spmp.shared.generated.resources.widget_config_details_base
import spmp.shared.generated.resources.widget_config_title
import spmp.shared.generated.resources.widget_config_type_name_common

class WidgetConfigurationScreen<A: TypeWidgetClickAction>(
    initial_base_config: BaseWidgetConfig?,
    initial_base_config_defaults_mask: BaseWidgetConfigDefaultsMask?,
    initial_type_config: TypeWidgetConfig<A>?,
    initial_type_config_defaults_mask: TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>?,
    private val context: AppContext,
    private val widget_type: SpMpWidgetType?,
    private val widget_id: Int?,
    private val onCancel: () -> Unit,
    private val onDone: (
        BaseWidgetConfig?,
        BaseWidgetConfigDefaultsMask?,
        TypeWidgetConfig<A>?,
        TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>?
    ) -> Unit,
    private val onSetDefaultBaseConfig: ((BaseWidgetConfig) -> Unit)? = null,
    private val onSetDefaultTypeConfig: ((TypeWidgetConfig<A>) -> Unit)? = null
): Screen {
    private val list_state: LazyListState = LazyListState()

    private var base_config: BaseWidgetConfig? by mutableStateOf(initial_base_config)
    private var base_config_defaults_mask: BaseWidgetConfigDefaultsMask? by mutableStateOf(initial_base_config_defaults_mask)
    private var type_config_defaults_mask: TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>? by mutableStateOf(initial_type_config_defaults_mask)

    private var type_config: TypeWidgetConfig<A>? by mutableStateOf(initial_type_config)

    @Composable
    override fun Content(modifier: Modifier, contentPadding: PaddingValues) {
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
                                    .replace("\$type", widget_type.default_config.getTypeName())
                                    .replace("\$id", widget_id.toString())
                            else if (widget_type != null)
                                stringResource(Res.string.`widget_config_details_$type`)
                                    .replace("\$type", widget_type.default_config.getTypeName())
                            else
                                stringResource(Res.string.widget_config_details_base)

                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.5f)
                        )

                        if (base_config_defaults_mask != null) {
                            Row(
                                Modifier.padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    SpMpWidgetConfiguration.DEFAULTS_ICON,
                                    null,
                                    Modifier.size(20.dp)
                                )
                                Text(stringResource(Res.string.widget_config_button_use_default_value))
                            }
                        }
                    }
                }

                ConfigItems()
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
                    onDone(base_config, base_config_defaults_mask, type_config, type_config_defaults_mask)
                }) {
                    Text(stringResource(Res.string.widget_config_button_done))
                }
            }
        }
    }

    private fun LazyListScope.ConfigItems(item_modifier: Modifier = Modifier) {
        type_config?.also { config ->
            item {
                ItemHeading(
                    config.getTypeName(),
                    true,
                    onSetAsDefault = onSetDefaultTypeConfig?.let { lambda -> {
                        lambda(config)
                    } }
                )
            }
            with (config) {
                ConfigItems(
                    context,
                    type_config_defaults_mask,
                    item_modifier,
                    onChanged = { type_config = it },
                    onDefaultsMaskChanged = { type_config_defaults_mask = it }
                )
            }
        }

        base_config?.also { config ->
            item {
                ItemHeading(
                    stringResource(Res.string.widget_config_type_name_common),
                    onSetAsDefault = onSetDefaultBaseConfig?.let { lambda -> {
                        lambda(config)
                    } }
                )
            }
            with (config) {
                ConfigItems(
                    context,
                    widget_type,
                    base_config_defaults_mask,
                    item_modifier,
                    onChanged = { base_config = it },
                    onDefaultsMaskChanged = { base_config_defaults_mask = it }
                )
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
            CompositionLocalProvider(LocalContentColor provides LocalComposeKitTheme.current.accent) {
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
