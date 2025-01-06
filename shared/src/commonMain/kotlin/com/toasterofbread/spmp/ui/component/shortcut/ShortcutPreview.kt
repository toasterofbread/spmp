package com.toasterofbread.spmp.ui.component.shortcut

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.utils.composable.animatedvisibility.NullableValueAnimatedVisibility
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.appaction.shortcut.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.shortcut.trigger.*
import dev.toastbits.composekit.theme.core.vibrantAccent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.shortcut_editor_trigger_label
import spmp.shared.generated.resources.shortcut_editor_trigger
import spmp.shared.generated.resources.shortcut_editor_empty_trigger

@Composable
fun ShortcutPreview(
    shortcut: Shortcut,
    modifier: Modifier = Modifier,
    onModification: (Shortcut?) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val shape: Shape = RoundedCornerShape(16.dp)
    var editing: Boolean by remember { mutableStateOf(false) }

    Column(
        modifier
            .background(player.theme.vibrantAccent.copy(alpha = 0.25f), shape)
            .padding(horizontal = 20.dp)
    ) {
        FlowRow {
            val item_modifier: Modifier = Modifier.align(Alignment.CenterVertically)

            CompositionLocalProvider(LocalContentColor provides player.theme.onBackground) {
                val action_type: AppAction.Type = shortcut.action.getType()
                Icon(action_type.getIcon(), null, item_modifier)
                Spacer(Modifier.width(10.dp))
                Text(action_type.getName(), item_modifier, softWrap = false)

                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                    shortcut.action.Preview(item_modifier.padding(start = 20.dp))

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    Text(stringResource(Res.string.shortcut_editor_trigger_label), item_modifier, softWrap = false)
                    Spacer(Modifier.width(10.dp))
                    ShortcutTriggerPreview(shortcut.trigger, item_modifier)
                }

                Crossfade(editing, item_modifier) {
                    IconButton({ editing = !it }) {
                        Icon(
                            if (it) Icons.Default.Close
                            else Icons.Default.Settings,
                            null
                        )
                    }
                }

                IconButton({ onModification(null) }, item_modifier) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        }

        AnimatedVisibility(editing) {
            Column(
                Modifier
                    .padding(vertical = 10.dp)
                    .background(player.theme.background, shape)
                    .padding(vertical = 10.dp, horizontal = 20.dp)
            ) {
                shortcut.action.ConfigurationItems(Modifier.fillMaxWidth()) {
                    onModification(shortcut.copy(action = it))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(Res.string.shortcut_editor_trigger))

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    ShortcutTriggerSelector(
                        stringResource(Res.string.shortcut_editor_trigger),
                        shortcut.trigger,
                        onModification = {
                            onModification(shortcut.copy(trigger = it))
                        }
                    )
                }

                NullableValueAnimatedVisibility(
                    shortcut.trigger,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) { trigger ->
                    if (trigger == null) {
                        return@NullableValueAnimatedVisibility
                    }

                    Column(Modifier.padding(start = 50.dp)) {
                        trigger.ConfigurationItems(
                            Modifier.fillMaxWidth(),
                            onModification = {
                                onModification(shortcut.copy(trigger = it))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutTriggerPreview(trigger: ShortcutTrigger?, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (trigger == null) {
            Text(stringResource(Res.string.shortcut_editor_empty_trigger), softWrap = false)
        }
        else {
            val type: ShortcutTrigger.Type = trigger.getType()
            Icon(type.getIcon(), null)
            Text(type.getName(), softWrap = false)

            trigger.IndicatorContent(Modifier)
        }
    }
}
