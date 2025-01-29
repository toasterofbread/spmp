package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.configuration.WidgetConfig
import dev.toastbits.composekit.settingsitem.domain.MutableStateSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.util.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_common_key_click_action

@Serializable
sealed class TypeWidgetConfig<A: TypeWidgetClickAction>: WidgetConfig() {
    abstract val click_action: WidgetClickAction<A>

    @Composable
    abstract fun getTypeName(): String

    fun LazyListScope.ConfigItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<A>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>) -> Unit
    ) {
        SubConfigurationItems(context, defaults_mask, item_modifier, onChanged) {
            @Suppress("UNCHECKED_CAST")
            onDefaultsMaskChanged(it as TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>)
        }

        configItem(
            defaults_mask?.click_action,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.setClickAction(it)) }
        ) { modifier, onItemChanged ->
            ClickActionItem(
                click_action,
                Res.string.widget_config_common_key_click_action,
                modifier
            ) {
                onChanged(setClickAction(it))
                onItemChanged()
            }
        }
    }

    protected abstract fun LazyListScope.SubConfigurationItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<out TypeWidgetConfig<A>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<A>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<out TypeWidgetConfig<A>>) -> Unit
    )

    protected abstract fun getActions(): List<A>

    protected abstract fun getActionNameResource(action: A): StringResource

    protected abstract fun setClickAction(click_action: WidgetClickAction<A>): TypeWidgetConfig<A>

    @Composable
    protected fun ClickActionItem(
        action: WidgetClickAction<A>,
        title: StringResource,
        modifier: Modifier = Modifier,
        onChanged: (WidgetClickAction<A>) -> Unit
    ) {
        val actions: List<WidgetClickAction<A>> = remember {
            WidgetClickAction.CommonWidgetClickAction.entries + getActions().map {
                WidgetClickAction.Type(it)
            }
        }

        val click_action_state: MutableState<Int> =
            remember { mutableIntStateOf(actions.indexOf(action)) }
        val click_action_property: PlatformSettingsProperty<Int> = remember {
            MutableStateSettingsProperty(
                click_action_state,
                { stringResource(title) },
                { null }
            )
        }

        remember {
            DropdownSettingsItem(
                click_action_property,
                actions.size
            ) {
                val this_action: WidgetClickAction<A> = actions[it]
                val resource: StringResource = when (this_action) {
                    is WidgetClickAction.CommonWidgetClickAction -> this_action.nameResource
                    is WidgetClickAction.Type -> getActionNameResource(this_action.actionEnum)
                }
                return@DropdownSettingsItem stringResource(resource)
            }
        }.Item(modifier)

        OnChangedEffect(click_action_state.value) {
            onChanged(actions[click_action_state.value])
        }
    }
}
