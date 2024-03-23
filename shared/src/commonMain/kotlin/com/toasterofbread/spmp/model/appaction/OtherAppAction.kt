package com.toasterofbread.spmp.model.appaction

import SpMp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.platform.composable.onWindowBackPressed
import kotlinx.serialization.Serializable

@Serializable
data class OtherAppAction(
    val action: Action = Action.DEFAULT
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.OTHER
    override fun getIcon(): ImageVector = action.getIcon()
    override fun isUsableDuringTextInput(): Boolean = action.isUsableDuringTextInput()

    override suspend fun executeAction(player: PlayerState) {
        action.execute(player)
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(action.getIcon(), null)
            Text(action.getName(), softWrap = false)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_action_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            item_count = Action.AVAILABLE.size,
            selected = action.ordinal,
            itemContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val action: Action = Action.AVAILABLE[it]
                    Icon(action.getIcon(), null)
                    Text(action.getName())
                }
            },
            onSelected = {
                onModification(copy(action = Action.AVAILABLE[it]))
                show_action_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("appaction_config_other_action"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_action_selector = !show_action_selector }) {
                Preview(Modifier)
            }
        }
    }

    enum class Action {
        NAVIGATE_BACK,
        TOGGLE_FULLSCREEN,
        RELOAD_PAGE,
        INCREASE_UI_SCALE,
        DECREASE_UI_SCALE;

        companion object {
            val DEFAULT: Action = NAVIGATE_BACK
            val AVAILABLE: List<Action> get() = entries.filter { it.isAvailable() }
        }

        fun getName(): String =
            when (this) {
                NAVIGATE_BACK -> getString("appaction_other_action_navigate_back")
                TOGGLE_FULLSCREEN -> getString("appaction_other_action_toggle_fullscreen")
                RELOAD_PAGE -> getString("appaction_other_action_reload_page")
                INCREASE_UI_SCALE -> getString("appaction_other_action_increase_ui_scale")
                DECREASE_UI_SCALE -> getString("appaction_other_action_decrease_ui_scale")
            }

        fun getIcon(): ImageVector =
            when (this) {
                NAVIGATE_BACK -> Icons.Default.ArrowBack
                TOGGLE_FULLSCREEN -> Icons.Default.Fullscreen
                RELOAD_PAGE -> Icons.Default.Refresh
                INCREASE_UI_SCALE -> Icons.Default.UnfoldMore
                DECREASE_UI_SCALE -> Icons.Default.UnfoldLess
            }

        fun isAvailable(): Boolean =
            when (this) {
                TOGGLE_FULLSCREEN -> Platform.DESKTOP.isCurrent()
                else -> true
            }

        fun isUsableDuringTextInput(): Boolean =
            when (this) {
                NAVIGATE_BACK -> true
                TOGGLE_FULLSCREEN -> true
                else -> false
            }

        suspend fun execute(player: PlayerState) {
            when (this) {
                NAVIGATE_BACK -> onWindowBackPressed(player.context)

                TOGGLE_FULLSCREEN -> SpMp.toggleFullscreenWindow()

                RELOAD_PAGE -> player.app_page.onReload()

                INCREASE_UI_SCALE, DECREASE_UI_SCALE -> {
                    val delta: Float = if (this == INCREASE_UI_SCALE) 0.1f else -0.1f
                    val current: Float = SystemSettings.Key.UI_SCALE.get()
                    SystemSettings.Key.UI_SCALE.set((current + delta).coerceAtLeast(0.1f))
                }
            }
        }
    }
}
