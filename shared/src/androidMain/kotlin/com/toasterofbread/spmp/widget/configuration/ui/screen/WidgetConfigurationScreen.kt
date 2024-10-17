package com.toasterofbread.spmp.widget.configuration.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import dev.toastbits.composekit.navigation.Screen
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.utils.common.copy
import kotlinx.serialization.json.Json

class WidgetConfigurationScreen(
    private val context: AppContext,
    widget_id: Int,
    private val widget_type: SpMpWidgetType,
    private val onCancel: () -> Unit,
    private val onDone: (SpMpWidgetConfiguration) -> Unit
): Screen {
    private val list_state: LazyListState = LazyListState()
    private var configuration: SpMpWidgetConfiguration by mutableStateOf(
        SpMpWidgetConfiguration.getForWidget(context, widget_type, widget_id)
    )

    @Composable
    override fun Content(navigator: Navigator, modifier: Modifier, contentPadding: PaddingValues) {
        Column(modifier) {
            LazyColumn(
                Modifier.fillMaxHeight().weight(1f),
                state = list_state,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = contentPadding.copy(bottom = 0.dp)
            ) {
                item {
                    Text("WIDGET CONFIGURATION")
                }

                with (configuration) {
                    ConfigurationItems(context, item_modifier = Modifier) {
                        configuration = it
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding.copy(top = 0.dp)),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                OutlinedButton(onCancel) {
                    Text("Cancel")
                }

                Button({
                    onDone(configuration)
                }) {
                    Text("Finish")
                }
            }
        }
    }
}
