package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.SongImageWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_type_name_song_image

@Serializable
data class SongImageWidgetConfig(
    override val click_action: WidgetClickAction<SongImageWidgetClickAction> = WidgetClickAction.DEFAULT
): TypeWidgetConfig<SongImageWidgetClickAction>() {
    @Composable
    override fun getTypeName(): String =
        stringResource(Res.string.widget_config_type_name_song_image)

    override fun LazyListScope.SubConfigurationItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<out TypeWidgetConfig<SongImageWidgetClickAction>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<SongImageWidgetClickAction>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<out TypeWidgetConfig<SongImageWidgetClickAction>>) -> Unit
    ) {
        require(defaults_mask is SongImageWidgetConfigDefaultsMask?)
    }

    override fun getActions(): List<SongImageWidgetClickAction> = SongImageWidgetClickAction.entries

    override fun getActionNameResource(action: SongImageWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<SongImageWidgetClickAction>): TypeWidgetConfig<SongImageWidgetClickAction> =
        copy(click_action = click_action)
}