package com.toasterofbread.spmp.model.appaction

import SpMp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_navigation
import spmp.shared.generated.resources.appaction_other
import spmp.shared.generated.resources.appaction_playback
import spmp.shared.generated.resources.appaction_song

@Serializable
sealed interface AppAction {
    fun getType(): Type
    fun getIcon(): ImageVector
    fun isUsableDuringTextInput(): Boolean = false

    suspend fun executeAction(state: SpMp.State)

    fun hasCustomContent(): Boolean = false
    @Composable
    fun CustomContent(onClick: (() -> Unit)?, modifier: Modifier) {}

    @Composable
    fun Preview(modifier: Modifier)

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit)

    enum class Type {
        NAVIGATION,
        SONG,
        PLAYBACK,
        OTHER;
        // MODIFY_SETTING; // TODO

        @Composable
        fun getName(): String =
            when (this) {
                NAVIGATION -> stringResource(Res.string.appaction_navigation)
                SONG -> stringResource(Res.string.appaction_song)
                PLAYBACK -> stringResource(Res.string.appaction_playback)
                OTHER -> stringResource(Res.string.appaction_other)
                // MODIFY_SETTING -> stringResource(Res.string.appaction_modify_setting)
            }

        fun getIcon(): ImageVector =
            when (this) {
                NAVIGATION -> Icons.Default.NearMe
                SONG -> Icons.Default.MusicNote
                PLAYBACK -> Icons.Default.PlayArrow
                OTHER -> Icons.Default.MoreHoriz
                // MODIFY_SETTING -> Icons.Default.ToggleOn
            }

        fun createAction(): AppAction =
            when (this) {
                NAVIGATION -> NavigationAppAction()
                SONG -> SongAppAction()
                PLAYBACK -> PlaybackAppAction()
                OTHER -> OtherAppAction()
                // MODIFY_SETTING -> TODO()
            }

        @Composable
        fun Preview(modifier: Modifier = Modifier) {
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(getIcon(), null)
                Text(getName(), softWrap = false)
            }
        }
    }
}
