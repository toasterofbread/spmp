package com.toasterofbread.spmp.widget.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.toasterofbread.spmp.model.mediaitem.db.observeAsState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.ProjectJson
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class SpMpWidgetConfiguration<A: TypeWidgetClickAction>(
    val type_configuration: TypeWidgetConfiguration<A>,
    val base_configuration: BaseWidgetConfiguration = BaseWidgetConfiguration()
) {
    companion object {
        fun encodeToString(configuration: SpMpWidgetConfiguration<out TypeWidgetClickAction>): String =
            ProjectJson.instance.encodeToString(configuration)

        fun decodeFromString(encoded: String): SpMpWidgetConfiguration<TypeWidgetClickAction> =
            ProjectJson.instance.decodeFromString(encoded)

        @Composable
        fun observeForWidget(context: AppContext, type: SpMpWidgetType, id: Int): State<SpMpWidgetConfiguration<out TypeWidgetClickAction>> =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .observeAsState(
                    id,
                    mapValue = { query ->
                        query.executeAsOneOrNull()
                            ?.let { decodeFromString(it) }
                            ?: runBlocking { type.getDefaultConfiguration(context) }
                    },
                    onExternalChange = null
                )

        suspend fun getForWidget(context: AppContext, type: SpMpWidgetType, id: Int): SpMpWidgetConfiguration<out TypeWidgetClickAction> =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .executeAsOneOrNull()
                ?.let { decodeFromString(it) }
                ?: type.getDefaultConfiguration(context)

        private suspend fun SpMpWidgetType.getDefaultConfiguration(context: AppContext): SpMpWidgetConfiguration<out TypeWidgetClickAction> {
            val base: BaseWidgetConfiguration = context.settings.widget.DEFAULT_BASE_WIDGET_CONFIGURATION.get()
            val type: TypeWidgetConfiguration<out TypeWidgetClickAction> = context.settings.widget.DEFAULT_TYPE_WIDGET_CONFIGURATIONS.get()[this] ?: this.defaultConfiguration
            return SpMpWidgetConfiguration(type, base)
        }
    }
}
