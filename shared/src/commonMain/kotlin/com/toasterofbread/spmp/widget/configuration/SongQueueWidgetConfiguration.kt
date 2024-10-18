package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_song_queue_next_songs_to_show
import spmp.shared.generated.resources.widget_config_type_name_song_queue

@Serializable
internal data class SongQueueWidgetConfiguration(
    val next_songs_to_show: Int = 1,
    override val click_action: WidgetClickAction<SongQueueWidgetClickAction> = WidgetClickAction.DEFAULT
): TypeWidgetConfiguration<SongQueueWidgetClickAction>() {
    @Composable
    override fun getTypeName(): String = stringResource(Res.string.widget_config_type_name_song_queue)

    override fun LazyListScope.SubConfigurationItems(
        context: AppContext,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfiguration<SongQueueWidgetClickAction>) -> Unit
    ) {
        item {
            NextSongsToShowItem(item_modifier, onChanged)
        }
    }

    override fun getActions(): List<SongQueueWidgetClickAction> = SongQueueWidgetClickAction.entries

    override fun getActionNameResource(action: SongQueueWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<SongQueueWidgetClickAction>): TypeWidgetConfiguration<SongQueueWidgetClickAction> =
        copy(click_action = click_action)

    @Composable
    private fun NextSongsToShowItem(modifier: Modifier, onChanged: (TypeWidgetConfiguration<SongQueueWidgetClickAction>) -> Unit) {
        val next_songs_to_show_state: MutableState<Int> = remember { mutableIntStateOf(next_songs_to_show) }
        val next_songs_to_show_property: PreferencesProperty<Int> = remember {
            MutableStatePreferencesProperty(
                next_songs_to_show_state,
                { stringResource(Res.string.widget_config_song_queue_next_songs_to_show) },
                { null },
                getPropertyDefaultValue = { 1 },
                getPropertyDefaultValueComposable = { 1 }
            )
        }

        OnChangedEffect(next_songs_to_show_state.value) {
            onChanged(this.copy(next_songs_to_show = next_songs_to_show_state.value))
        }

        remember {
            AppSliderItem(
                next_songs_to_show_property,
                range = 0f..5f
            )
        }.Item(modifier)
    }
}
