package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.db.observeAsState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.utils.common.thenIf
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_type_name_common
import kotlin.reflect.KClass

@Serializable
data class SpMpWidgetConfiguration<A: TypeWidgetClickAction>(
    val type_configuration: TypeWidgetConfiguration<A>,
    val base_configuration: BaseWidgetConfiguration = BaseWidgetConfiguration()
) {
    fun LazyListScope.ConfigurationItems(context: AppContext, item_modifier: Modifier = Modifier, onChanged: (SpMpWidgetConfiguration<A>) -> Unit) {
        item {
            ItemHeading(type_configuration.getTypeName(), true)
        }
        with (type_configuration) {
            ConfigurationItems(context, item_modifier) {
                onChanged(copy(type_configuration = it))
            }
        }

        item {
            ItemHeading(stringResource(Res.string.widget_config_type_name_common))
        }
        item {
            type_configuration.ClickActionItem(item_modifier) {
                onChanged(copy(type_configuration = it))
            }
        }
        with (base_configuration) {
            ConfigurationItems(context, item_modifier) {
                onChanged(copy(base_configuration = it))
            }
        }
    }

    @Composable
    private fun ItemHeading(name: String, first: Boolean = false) {
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
                HorizontalDivider(Modifier.fillMaxWidth().weight(1f))

                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    companion object {
        fun encodeToString(configuration: SpMpWidgetConfiguration<out TypeWidgetClickAction>): String =
            json.encodeToString(configuration)

        fun decodeFromString(encoded: String): SpMpWidgetConfiguration<TypeWidgetClickAction> =
            json.decodeFromString(encoded)

        @Suppress("UNCHECKED_CAST")
        @Composable
        fun observeForWidget(context: AppContext, type: SpMpWidgetType, id: Int): State<SpMpWidgetConfiguration<TypeWidgetClickAction>> =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .observeAsState(
                    id,
                    mapValue = { query ->
                        query.executeAsOneOrNull()
                            ?.let { decodeFromString(it) }
                            ?: SpMpWidgetConfiguration(type.defaultConfiguration as TypeWidgetConfiguration<TypeWidgetClickAction>)
                    },
                    onExternalChange = null
                )

        @Suppress("UNCHECKED_CAST")
        fun getForWidget(context: AppContext, type: SpMpWidgetType, id: Int): SpMpWidgetConfiguration<TypeWidgetClickAction> =
            context.database.androidWidgetQueries.configurationById(id.toLong())
                .executeAsOneOrNull()
                ?.let { decodeFromString(it) }
                ?: SpMpWidgetConfiguration(type.defaultConfiguration as TypeWidgetConfiguration<TypeWidgetClickAction>)

        private val json: Json by lazy {
            Json {
                useArrayPolymorphism = true
                serializersModule = SerializersModule {
                    // I have no idea why, but these aren't detected automatically
                    for (type in SpMpWidgetType.entries) {
                        registerSerialiser(type.clickActionClass)
                    }
                }
            }
        }

        @OptIn(InternalSerializationApi::class)
        private fun <T: TypeWidgetClickAction> SerializersModuleBuilder.registerSerialiser(subclass: KClass<T>) {
            polymorphic(TypeWidgetClickAction::class, subclass, subclass.serializer())
        }
    }
}
