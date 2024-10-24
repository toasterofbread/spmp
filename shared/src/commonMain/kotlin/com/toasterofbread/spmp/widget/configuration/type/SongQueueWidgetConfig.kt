package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
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
            ShowCurrentSongItem(modifier) {
                onChanged(it)
                onItemChanged()
            }
        }

        configItem(
            defaults_mask?.next_songs_to_show,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(next_songs_to_show = it)) }
        ) { modifier, onItemChanged ->
            NextSongsToShowItem(modifier) {
                onChanged(it)
                onItemChanged()
            }
        }
    }

    override fun getActions(): List<SongQueueWidgetClickAction> = SongQueueWidgetClickAction.entries

    override fun getActionNameResource(action: SongQueueWidgetClickAction): StringResource = action.nameResource

    override fun setClickAction(click_action: WidgetClickAction<SongQueueWidgetClickAction>): TypeWidgetConfig<SongQueueWidgetClickAction> =
        copy(click_action = click_action)

    @Composable
    private fun ShowCurrentSongItem(modifier: Modifier, onChanged: (TypeWidgetConfig<SongQueueWidgetClickAction>) -> Unit) {
        val show_current_song_state: MutableState<Boolean> =
            remember { mutableStateOf(show_current_song) }
        val show_current_song_property: PreferencesProperty<Boolean> = remember {
            MutableStatePreferencesProperty(
                show_current_song_state,
                { stringResource(Res.string.widget_config_song_queue_show_current_song) },
                { null }
            )
        }

        OnChangedEffect(show_current_song_state.value) {
            onChanged(this.copy(show_current_song = show_current_song_state.value))
        }

        remember {
            ToggleSettingsItem(show_current_song_property)
        }.Item(modifier)
    }

    @Composable
    private fun NextSongsToShowItem(modifier: Modifier, onChanged: (TypeWidgetConfig<SongQueueWidgetClickAction>) -> Unit) {
        val next_songs_to_show_state: MutableState<Int> =
            remember { mutableIntStateOf(next_songs_to_show) }
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
                range = -1f..5f
            )
        }.Item(modifier)
    }
}