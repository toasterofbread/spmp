package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import dev.toastbits.composekit.settingsitem.domain.MutableStateSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.util.composable.OnChangedEffect
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
internal data class LyricsWidgetConfig(
    val furigana_mode: FuriganaMode = FuriganaMode.APP_DEFAULT,
    override val click_action: WidgetClickAction<LyricsWidgetClickAction> = WidgetClickAction.DEFAULT
): TypeWidgetConfig<LyricsWidgetClickAction>() {
    @Composable
    override fun getTypeName(): String = stringResource(Res.string.widget_config_type_name_lyrics)

    enum class FuriganaMode {
        APP_DEFAULT,
        SHOW,
        HIDE
    }

    override fun LazyListScope.SubConfigurationItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<out TypeWidgetConfig<LyricsWidgetClickAction>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<LyricsWidgetClickAction>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<out TypeWidgetConfig<LyricsWidgetClickAction>>) -> Unit
    ) {
        require(defaults_mask is LyricsWidgetConfigDefaultsMask?)

        configItem(
            defaults_mask?.furigana_mode,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(furigana_mode = it)) }
        ) { modifier, onItemChanged ->
            FuriganaModeItem(modifier) {
                onChanged(it)
                onItemChanged()
            }
        }
    }

    override fun getActions(): List<LyricsWidgetClickAction> = LyricsWidgetClickAction.entries

    override fun getActionNameResource(action: LyricsWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<LyricsWidgetClickAction>): TypeWidgetConfig<LyricsWidgetClickAction> =
        copy(click_action = click_action)

    @Composable
    private fun FuriganaModeItem(modifier: Modifier, onChanged: (TypeWidgetConfig<LyricsWidgetClickAction>) -> Unit) {
        val furigana_mode_state: MutableState<FuriganaMode> =
            remember { mutableStateOf(furigana_mode) }
        val furigana_mode_property: PlatformSettingsProperty<FuriganaMode> = remember {
            MutableStateSettingsProperty(
                furigana_mode_state,
                { stringResource(Res.string.widget_config_lyrics_key_furigana_mode) },
                { null }
            )
        }

        OnChangedEffect(furigana_mode_state.value) {
            onChanged(this.copy(furigana_mode = furigana_mode_state.value))
        }

        remember {
            DropdownSettingsItem.ofEnumState(furigana_mode_property) { mode ->
                when (mode) {
                    FuriganaMode.APP_DEFAULT -> stringResource(Res.string.widget_config_lyrics_option_furigana_mode_app)
                    FuriganaMode.SHOW -> stringResource(Res.string.widget_config_lyrics_option_furigana_mode_show)
                    FuriganaMode.HIDE -> stringResource(Res.string.widget_config_lyrics_option_furigana_mode_hide)
                }
            }
        }.Item(modifier)
    }
}