package com.toasterofbread.spmp.model.appaction

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.appaction.action.playback.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_navigation
import spmp.shared.generated.resources.appaction_song
import spmp.shared.generated.resources.appaction_playback
import spmp.shared.generated.resources.appaction_other
import spmp.shared.generated.resources.appaction_modify_setting

@Serializable
sealed interface AppAction {
    fun getType(): Type
    fun getIcon(): ImageVector
    fun isUsableDuringTextInput(): Boolean = false

    suspend fun executeAction(player: PlayerState)

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
