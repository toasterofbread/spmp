package com.toasterofbread.spmp.widget.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.createThemeSelectorSettingsItem
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.settings.ui.component.item.ThemeSelectorSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable

@Serializable
data class BaseWidgetConfiguration(
    val theme_index: Int? = null
) {
    @Composable
    fun ConfigurationItems(context: AppContext, item_modifier: Modifier = Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
        ThemeIndexItem(context, item_modifier, onChanged)
    }

    @Composable
    private fun ThemeIndexItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
        val theme_index_state: MutableState<Int> = remember { mutableIntStateOf(theme_index?.plus(1) ?: 0) }
        val theme_index_property: PreferencesProperty<Int> = remember {
            MutableStatePreferencesProperty(
                theme_index_state,
                { "" },
                { null }
            )
        }

        val theme_selector_item: ThemeSelectorSettingsItem = remember {
            createThemeSelectorSettingsItem(
                context,
                theme_index_property,
                getExtraStartThemes = {
                    listOf(
                        NamedTheme("APPLICATION THEME", ThemeValuesData.of(context.theme))
                    )
                }
            )
        }

        OnChangedEffect(theme_index_state.value) {
            onChanged(
                this.copy(
                    theme_index =
                        theme_index_state.value.let { index ->
                            if (index <= 0) null
                            else index - 1
                        }
                )
            )
        }

        theme_selector_item.Item(modifier)
    }
}
