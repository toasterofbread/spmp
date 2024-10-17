package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.db.observeAsState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SpMpWidgetConfiguration(
    val type_configuration: TypeWidgetConfiguration,
    val base_configuration: BaseWidgetConfiguration = BaseWidgetConfiguration()
) {
    fun LazyListScope.ConfigurationItems(context: AppContext, item_modifier: Modifier = Modifier, onChanged: (SpMpWidgetConfiguration) -> Unit) {
        item {
            Text(type_configuration.getName())
        }
        with (type_configuration) {
            ConfigurationItems(context, item_modifier) {
                onChanged(copy(type_configuration = it))
            }
        }
        item {
            Text("COMMON", Modifier.padding(top = 50.dp))
        }
        with (base_configuration) {
            ConfigurationItems(context, item_modifier) {
                onChanged(copy(base_configuration = it))
            }
        }
    }

    companion object {
        @Composable
        fun observeForWidget(context: AppContext, type: SpMpWidgetType, id: Int): State<SpMpWidgetConfiguration> =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .observeAsState(
                    id,
                    mapValue = { query ->
                        query.executeAsOneOrNull()
                            ?.let { Json.decodeFromString(it) }
                            ?: SpMpWidgetConfiguration(type.defaultConfiguration)
                    },
                    onExternalChange = null
                )

        fun getForWidget(context: AppContext, type: SpMpWidgetType, id: Int): SpMpWidgetConfiguration =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .executeAsOneOrNull()
                ?.let { Json.decodeFromString(it) }
                ?: SpMpWidgetConfiguration(type.defaultConfiguration)

    }
}
