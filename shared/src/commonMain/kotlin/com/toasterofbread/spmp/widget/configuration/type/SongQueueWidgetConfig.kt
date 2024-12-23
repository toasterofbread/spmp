package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_song_queue_next_songs_to_show
import spmp.shared.generated.resources.widget_config_song_queue_show_current_song
import spmp.shared.generated.resources.widget_config_type_name_song_queue

@Serializable
data class SongQueueWidgetConfig(
    val show_current_song: Boolean = true,
    val next_songs_to_show: Int = -1,
    override val click_action: WidgetClickAction<SongQueueWidgetClickAction> = WidgetClickAction.DEFAULT
): TypeWidgetConfig<SongQueueWidgetClickAction>() {
    @Composable
    override fun getTypeName(): String =
        stringResource(Res.string.widget_config_type_name_song_queue)

    override fun LazyListScope.SubConfigurationItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<out TypeWidgetConfig<SongQueueWidgetClickAction>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<SongQueueWidgetClickAction>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<out TypeWidgetConfig<SongQueueWidgetClickAction>>) -> Unit
    ) {
        require(defaults_mask is SongQueueWidgetConfigDefaultsMask?)

        configItem(
            defaults_mask?.show_current_song,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(show_current_song = it)) }
        ) { modifier, onItemChanged ->
            ToggleItem(
                show_current_song,
                Res.string.widget_config_song_queue_show_current_song,
                modifier
            ) {
                onChanged(copy(show_current_song = it))
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.next_songs_to_show,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(next_songs_to_show = it)) }
        ) { modifier, onItemChanged ->
            SliderItem(
                next_songs_to_show,
                1,
                Res.string.widget_config_song_queue_next_songs_to_show,
                modifier,
                range = -1f..5f
            ) {
                onChanged(copy(next_songs_to_show = it))
                onItemChanged()
            }
        }
    }

    override fun getActions(): List<SongQueueWidgetClickAction> = SongQueueWidgetClickAction.entries

    override fun getActionNameResource(action: SongQueueWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<SongQueueWidgetClickAction>): TypeWidgetConfig<SongQueueWidgetClickAction> =
        copy(click_action = click_action)
}