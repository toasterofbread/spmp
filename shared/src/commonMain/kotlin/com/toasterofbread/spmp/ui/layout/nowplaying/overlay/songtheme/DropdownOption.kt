package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.songtheme

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal abstract class DropdownOption<T: Enum<*>>(
    val entries: List<T>,
    val getEntryText: @Composable (T) -> String,
    override val titleResource: StringResource,
    override val icon: ImageVector,
    val getProperty: Settings.() -> PlatformSettingsProperty<T>,
    val getSongProperty: Song.() -> Property<T?>
): SongThemeOption() {
    @Composable
    override fun Content(song: Song, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current

        var song_value: T? by getSongProperty(song).observe(player.database)
        val global_value: T by getProperty(player.settings).observe()

        var show_position_selector: Boolean by remember { mutableStateOf(false) }
        LargeDropdownMenu(
            title = stringResource(titleResource),
            isOpen = show_position_selector,
            onDismissRequest = { show_position_selector = false },
            items = entries,
            selectedItem = song_value ?: global_value,
            onSelected = { _, item ->
                song_value = item
                show_position_selector = false
            }
        ) { item ->
            Text(getEntryText(item))
        }

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TitleBar(Modifier.align(Alignment.CenterVertically))

            Button({ show_position_selector = !show_position_selector }) {
                Text(getEntryText(song_value ?: global_value))
            }
        }
    }
}
