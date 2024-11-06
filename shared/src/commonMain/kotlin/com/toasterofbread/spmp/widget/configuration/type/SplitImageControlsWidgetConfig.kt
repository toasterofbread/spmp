package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.SplitImageControlsWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.configuration.enum.WidgetSectionTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_split_image_controls_bottom_end_button_action
import spmp.shared.generated.resources.widget_config_split_image_controls_bottom_start_button_action
import spmp.shared.generated.resources.widget_config_split_image_controls_content_row_theme
import spmp.shared.generated.resources.widget_config_split_image_controls_swap_title_content_rows
import spmp.shared.generated.resources.widget_config_split_image_controls_title_row_theme
import spmp.shared.generated.resources.widget_config_split_image_controls_top_end_button_action
import spmp.shared.generated.resources.widget_config_split_image_controls_top_start_button_action
import spmp.shared.generated.resources.widget_config_type_name_song_image

@Serializable
data class SplitImageControlsWidgetConfig(
    val display_lyrics: Boolean = true, // TODO
    val swap_title_content_rows: Boolean = false,

    val title_row_theme: WidgetSectionTheme =
        WidgetSectionTheme(WidgetSectionTheme.Mode.BACKGROUND, 0.7f),
    val content_row_theme: WidgetSectionTheme =
        WidgetSectionTheme(WidgetSectionTheme.Mode.ACCENT),

    val top_start_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.TOGGLE_LIKE,
    val top_end_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.PLAY_PAUSE,
    val bottom_start_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.SEEK_PREVIOUS,
    val bottom_end_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.SEEK_NEXT,

    override val click_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.DEFAULT
): TypeWidgetConfig<SplitImageControlsWidgetClickAction>() {
    @Composable
    override fun getTypeName(): String =
        stringResource(Res.string.widget_config_type_name_song_image)

    override fun LazyListScope.SubConfigurationItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<out TypeWidgetConfig<SplitImageControlsWidgetClickAction>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<SplitImageControlsWidgetClickAction>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<out TypeWidgetConfig<SplitImageControlsWidgetClickAction>>) -> Unit
    ) {
        require(defaults_mask is SplitImageControlsWidgetConfigDefaultsMask?)

        configItem(
            defaults_mask?.swap_title_content_rows,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(swap_title_content_rows = it)) }
        ) { modifier, onItemChanged ->
            ToggleItem(
                swap_title_content_rows,
                Res.string.widget_config_split_image_controls_swap_title_content_rows,
                modifier
            ) {
                onChanged(copy(swap_title_content_rows = it))
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.title_row_theme,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(title_row_theme = it)) }
        ) { modifier, onItemChanged ->
            SectionThemeItem(
                title_row_theme,
                Res.string.widget_config_split_image_controls_title_row_theme,
                modifier
            ) {
                onChanged(copy(title_row_theme = it))
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.content_row_theme,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(content_row_theme = it)) }
        ) { modifier, onItemChanged ->
            SectionThemeItem(
                content_row_theme,
                Res.string.widget_config_split_image_controls_content_row_theme,
                modifier
            ) {
                onChanged(copy(content_row_theme = it))
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.top_start_button_action,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(top_start_button_action = it)) }
        ) { modifier, onItemChanged ->
            ClickActionItem(
                top_start_button_action,
                Res.string.widget_config_split_image_controls_top_start_button_action,
                modifier
            ) {
                onChanged(copy(top_start_button_action = it))
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.top_end_button_action,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(top_end_button_action = it)) }
        ) { modifier, onItemChanged ->
            ClickActionItem(
                top_end_button_action,
                Res.string.widget_config_split_image_controls_top_end_button_action,
                modifier
            ) {
                onChanged(copy(top_end_button_action = it))
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.bottom_start_button_action,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(bottom_start_button_action = it)) }
        ) { modifier, onItemChanged ->
            ClickActionItem(
                bottom_start_button_action,
                Res.string.widget_config_split_image_controls_bottom_start_button_action,
                modifier
            ) {
                onChanged(copy(bottom_start_button_action = it))
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.bottom_end_button_action,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(bottom_end_button_action = it)) }
        ) { modifier, onItemChanged ->
            ClickActionItem(
                bottom_end_button_action,
                Res.string.widget_config_split_image_controls_bottom_end_button_action,
                modifier
            ) {
                onChanged(copy(bottom_end_button_action = it))
                onItemChanged()
            }
        }
    }

    override fun getActions(): List<SplitImageControlsWidgetClickAction> = SplitImageControlsWidgetClickAction.entries

    override fun getActionNameResource(action: SplitImageControlsWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<SplitImageControlsWidgetClickAction>): TypeWidgetConfig<SplitImageControlsWidgetClickAction> =
        copy(click_action = click_action)
}