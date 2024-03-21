package com.toasterofbread.spmp.model.appaction

import kotlinx.serialization.Serializable
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.resources.getString

@Serializable
sealed interface AppAction {
    fun getType(): Type
    suspend fun executeAction(player: PlayerState)

    @Composable
    fun Preview(modifier: Modifier)

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit)

    enum class Type {
        OPEN_PAGE,
        SONG,
        PLAYBACK,
        SYSTEM,
        MODIFY_SETTING;

        fun getName(): String =
            when (this) {
                OPEN_PAGE -> getString("appaction_open_page")
                SONG -> getString("appaction_song")
                PLAYBACK -> getString("appaction_playback")
                SYSTEM -> getString("appaction_system")
                MODIFY_SETTING -> getString("appaction_modify_setting")
            }

        fun getIcon(): ImageVector =
            when (this) {
                OPEN_PAGE -> Icons.Default.OpenInBrowser
                SONG -> Icons.Default.MusicNote
                PLAYBACK -> Icons.Default.PlayArrow
                SYSTEM -> Icons.Default.Settings
                MODIFY_SETTING -> Icons.Default.ToggleOn
            }

        fun createAction(): AppAction =
            when (this) {
                OPEN_PAGE -> OpenPageAppAction()
                SONG -> SongAppAction()
                PLAYBACK   -> TODO()
                SYSTEM -> TODO()
                MODIFY_SETTING -> TODO()
            }
    }
}
