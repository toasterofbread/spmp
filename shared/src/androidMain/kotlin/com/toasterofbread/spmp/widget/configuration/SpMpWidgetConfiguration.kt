package com.toasterofbread.spmp.widget.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
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
    @Composable
    fun ConfigurationItems(context: AppContext, item_modifier: Modifier = Modifier, onChanged: (SpMpWidgetConfiguration) -> Unit) {
        base_configuration.ConfigurationItems(context, item_modifier) {
            onChanged(this.copy(base_configuration = it))
        }
        type_configuration.ConfigurationItems(context, item_modifier) {
            onChanged(this.copy(type_configuration = it))
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
