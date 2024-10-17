package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_lyrics_key_furigana_mode
import spmp.shared.generated.resources.widget_config_lyrics_option_furigana_mode_app
import spmp.shared.generated.resources.widget_config_lyrics_option_furigana_mode_hide
import spmp.shared.generated.resources.widget_config_lyrics_option_furigana_mode_show
import spmp.shared.generated.resources.widget_config_type_name_lyrics

@Serializable
internal data class LyricsWidgetConfiguration(
    val furigana_mode: FuriganaMode = FuriganaMode.APP_DEFAULT,
    override val click_action: WidgetClickAction<LyricsWidgetClickAction> = WidgetClickAction.DEFAULT
): TypeWidgetConfiguration<LyricsWidgetClickAction>() {
    @Composable
    override fun getTypeName(): String = stringResource(Res.string.widget_config_type_name_lyrics)

    enum class FuriganaMode {
        APP_DEFAULT,
        SHOW,
        HIDE
    }

    override fun LazyListScope.ConfigurationItems(
        context: AppContext,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfiguration<LyricsWidgetClickAction>) -> Unit
    ) {
        item {
            FuriganaModeItem(item_modifier, onChanged)
        }
    }

    override fun getActions(): List<LyricsWidgetClickAction> = LyricsWidgetClickAction.entries

    override fun getActionNameResource(action: LyricsWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<LyricsWidgetClickAction>): TypeWidgetConfiguration<LyricsWidgetClickAction> =
        copy(click_action = click_action)

    @Composable
    private fun FuriganaModeItem(modifier: Modifier, onChanged: (TypeWidgetConfiguration<LyricsWidgetClickAction>) -> Unit) {
        val furigana_mode_state: MutableState<FuriganaMode> = remember { mutableStateOf(furigana_mode) }
        val furigana_mode_property: PreferencesProperty<FuriganaMode> = remember {
            MutableStatePreferencesProperty(
                furigana_mode_state,
                { stringResource(Res.string.widget_config_lyrics_key_furigana_mode) },
                { null }
            )
        }

        OnChangedEffect(furigana_mode_state.value) {
            onChanged(this.copy(furigana_mode = furigana_mode_state.value))
        }

        remember {
            DropdownSettingsItem(furigana_mode_property) { mode ->
                when (mode) {
                    FuriganaMode.APP_DEFAULT -> stringResource(Res.string.widget_config_lyrics_option_furigana_mode_app)
                    FuriganaMode.SHOW -> stringResource(Res.string.widget_config_lyrics_option_furigana_mode_show)
                    FuriganaMode.HIDE -> stringResource(Res.string.widget_config_lyrics_option_furigana_mode_hide)
                }
            }
        }.Item(modifier)
    }
}
