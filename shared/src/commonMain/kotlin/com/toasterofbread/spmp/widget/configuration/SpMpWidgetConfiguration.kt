package com.toasterofbread.spmp.widget.configuration

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.db.observeAsState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.TypeConfigurationDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.TypeWidgetConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@Serializable
data class SpMpWidgetConfiguration<A: TypeWidgetClickAction>(
    val base_configuration: BaseWidgetConfig,
    val base_configuration_defaults_mask: BaseWidgetConfigDefaultsMask,
    val type_configuration: TypeWidgetConfig<A>,
    val type_configuration_defaults_mask: TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>
) {
    companion object {
        val DEFAULTS_ICON: ImageVector
            get() = Icons.AutoMirrored.Outlined.Label

        val json: Json = Json {
            useArrayPolymorphism = true
            serializersModule = SerializersModule {
                // I have no idea why, but these aren't detected automatically
                for (type in SpMpWidgetType.entries) {
                    registerPolymorphicSerialiser(type.click_action_class)
                }
            }
        }

        @OptIn(InternalSerializationApi::class)
        private inline fun <reified Base: Any, Sub: Base> SerializersModuleBuilder.registerPolymorphicSerialiser(subclass: KClass<Sub>) {
            polymorphic(Base::class, subclass, subclass.serializer())
        }

        @Composable
        fun observeForWidget(context: AppContext, type: SpMpWidgetType, id: Int): State<SpMpWidgetConfiguration<out TypeWidgetClickAction>> =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .observeAsState(
                    id,
                    mapValue = { query ->
                        query.executeAsOneOrNull()
                            ?.let { json.decodeFromString(it) }
                            ?: runBlocking { type.getDefaultConfiguration(context) }
                    },
                    onExternalChange = null
                )

        suspend fun getForWidget(context: AppContext, type: SpMpWidgetType, id: Int): SpMpWidgetConfiguration<out TypeWidgetClickAction> =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .executeAsOneOrNull()
                ?.let { json.decodeFromString(it) }
                ?: type.getDefaultConfiguration(context)

        private suspend fun SpMpWidgetType.getDefaultConfiguration(context: AppContext): SpMpWidgetConfiguration<out TypeWidgetClickAction> {
            val base: BaseWidgetConfig = context.settings.Widget.DEFAULT_BASE_WIDGET_CONFIGURATION.get()
            val type: TypeWidgetConfig<out TypeWidgetClickAction> = context.settings.Widget.DEFAULT_TYPE_WIDGET_CONFIGURATIONS.get()[this] ?: this.default_config
            return createDefaultConfig(base, type)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <A: TypeWidgetClickAction> SpMpWidgetType.createDefaultConfig(base: BaseWidgetConfig, type: TypeWidgetConfig<A>): SpMpWidgetConfiguration<A> {
            return SpMpWidgetConfiguration(
                base,
                BaseWidgetConfigDefaultsMask(),
                type,
                this.default_defaults_mask as TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>
            )
        }
    }
}
