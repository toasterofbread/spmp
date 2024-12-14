package com.toasterofbread.spmp.model.appaction

import SpMp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.components.platform.composable.onWindowBackPressed
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import dev.toastbits.composekit.util.platform.Platform
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_config_other_action
import spmp.shared.generated.resources.appaction_other_action_decrease_ui_scale
import spmp.shared.generated.resources.appaction_other_action_increase_ui_scale
import spmp.shared.generated.resources.appaction_other_action_navigate_back
import spmp.shared.generated.resources.appaction_other_action_reload_page
import spmp.shared.generated.resources.appaction_other_action_toggle_fullscreen

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

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_action_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            title =stringResource(Res.string.appaction_config_other_action),
            isOpen = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            items = Action.AVAILABLE,
            selectedItem = action,
            itemContent = { action ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(action.getIcon(), null)
                    Text(action.getName())
                }
            },
            onSelected = { _, action ->
                onModification(copy(action = action))
                show_action_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.appaction_config_other_action),
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

        @Composable
        fun getName(): String =
            when (this) {
                NAVIGATE_BACK -> stringResource(Res.string.appaction_other_action_navigate_back)
                TOGGLE_FULLSCREEN -> stringResource(Res.string.appaction_other_action_toggle_fullscreen)
                RELOAD_PAGE -> stringResource(Res.string.appaction_other_action_reload_page)
                INCREASE_UI_SCALE -> stringResource(Res.string.appaction_other_action_increase_ui_scale)
                DECREASE_UI_SCALE -> stringResource(Res.string.appaction_other_action_decrease_ui_scale)
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
                    val current: Float = player.context.settings.Interface.UI_SCALE.get()
                    player.context.settings.Interface.UI_SCALE.set((current + delta).coerceAtLeast(0.1f))
                }
            }
        }
    }
}
