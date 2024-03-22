package com.toasterofbread.spmp.model.appaction

import kotlinx.serialization.Serializable
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.model.appaction.action.playback.*

@Serializable
sealed interface AppAction {
    fun getType(): Type
    fun isUsableDuringTextInput(): Boolean = false

    suspend fun executeAction(player: PlayerState)

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

        fun getName(): String =
            when (this) {
                NAVIGATION -> getString("appaction_navigation")
                SONG -> getString("appaction_song")
                PLAYBACK -> getString("appaction_playback")
                OTHER -> getString("appaction_other")
                // MODIFY_SETTING -> getString("appaction_modify_setting")
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
    }
}
