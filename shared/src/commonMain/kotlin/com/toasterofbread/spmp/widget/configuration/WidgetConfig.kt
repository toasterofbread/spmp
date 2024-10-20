package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.utils.common.thenIf
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_button_use_default_value

abstract class WidgetConfig {
    protected fun LazyListScope.configItem(
        default_mask_value: Boolean?,
        modifier: Modifier,
        onDefaultMaskValueChanged: (Boolean) -> Unit,
        content: @Composable (Modifier, onChange: () -> Unit) -> Unit
    ) {
        item {
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (default_mask_value != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            SpMpWidgetConfiguration.DEFAULTS_ICON,
                            stringResource(Res.string.widget_config_button_use_default_value),
                            Modifier.size(15.dp)
                        )
                        RadioButton(
                            default_mask_value,
                            { onDefaultMaskValueChanged(!default_mask_value) },
                            Modifier.size(25.dp)
                        )
                    }
                }

                content(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .thenIf(default_mask_value == true) {
                            graphicsLayer { alpha = 0.5f; clip = false }
                        }
                ) {
                    if (default_mask_value != null) {
                        onDefaultMaskValueChanged(false)
                    }
                }
            }
        }
    }
}
