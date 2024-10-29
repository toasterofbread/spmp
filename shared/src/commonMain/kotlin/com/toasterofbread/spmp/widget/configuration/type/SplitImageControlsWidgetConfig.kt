package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.SplitImageControlsWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.configuration.enum.WidgetSectionThemeMode
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_type_name_song_image

@Serializable
data class SplitImageControlsWidgetConfig(
    // TODO Mask
    val display_lyrics: Boolean = true, // TODO
    val title_row_theme_mode: WidgetSectionThemeMode = WidgetSectionThemeMode.TRANSPARENT,
    val content_row_theme_mode: WidgetSectionThemeMode = WidgetSectionThemeMode.BACKGROUND,
    val swap_title_content_rows: Boolean = false,
    val top_start_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.SEEK_PREVIOUS,
    val top_end_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.SEEK_NEXT,
    val bottom_start_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.OPEN_WIDGET_CONFIG,
    val bottom_end_button_action: WidgetClickAction<SplitImageControlsWidgetClickAction> =
        WidgetClickAction.CommonWidgetClickAction.PLAY_PAUSE,
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
    }

    override fun getActions(): List<SplitImageControlsWidgetClickAction> = SplitImageControlsWidgetClickAction.entries

    override fun getActionNameResource(action: SplitImageControlsWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<SplitImageControlsWidgetClickAction>): TypeWidgetConfig<SplitImageControlsWidgetClickAction> =
        copy(click_action = click_action)
}