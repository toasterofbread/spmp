package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_common_key_click_action

@Serializable
sealed class TypeWidgetConfiguration<A: TypeWidgetClickAction> {
    abstract val click_action: WidgetClickAction<A>

    @Composable
    abstract fun getTypeName(): String

    @Composable
    fun ClickActionItem(modifier: Modifier, onChanged: (TypeWidgetConfiguration<A>) -> Unit) {
        val actions: List<WidgetClickAction<A>> = remember {
            WidgetClickAction.CommonWidgetClickAction.entries + getActions().map { WidgetClickAction.Type(it) }
        }

        val click_action_state: MutableState<Int> = remember { mutableIntStateOf(actions.indexOf(click_action)) }
        val click_action_property: PreferencesProperty<Int> = remember {
            MutableStatePreferencesProperty(
                click_action_state,
                { stringResource(Res.string.widget_config_common_key_click_action) },
                { null }
            )
        }

        remember {
            DropdownSettingsItem(
                click_action_property,
                actions.size
            ) {
                val action: WidgetClickAction<A> = actions[it]
                val resource: StringResource = when (action) {
                    is WidgetClickAction.CommonWidgetClickAction -> action.nameResource
                    is WidgetClickAction.Type -> getActionNameResource(action.actionEnum)
                }
                return@DropdownSettingsItem stringResource(resource)
            }
        }.Item(modifier)

        OnChangedEffect(click_action_state.value) {
            onChanged(setClickAction(actions[click_action_state.value]))
        }
    }

    abstract fun LazyListScope.ConfigurationItems(context: AppContext, item_modifier: Modifier, onChanged: (TypeWidgetConfiguration<A>) -> Unit)

    protected abstract fun getActions(): List<A>

    protected abstract fun getActionNameResource(action: A): StringResource

    protected abstract fun setClickAction(click_action: WidgetClickAction<A>): TypeWidgetConfiguration<A>
}
