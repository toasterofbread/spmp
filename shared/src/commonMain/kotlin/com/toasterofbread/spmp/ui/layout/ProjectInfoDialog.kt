package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import org.jetbrains.compose.resources.painterResource
import spmp.shared.generated.resources.*
import spmp.shared.generated.resources.Res

private val GESTURES: List<Triple<ImageVector, String, List<String>>> = listOf(
    Triple(Icons.Default.SwipeVertical, "project_info_dialog_gesture_player_swipes", listOf("project_info_dialog_gesture_player_swipe_player", "project_info_dialog_gesture_player_swipe_queue")),
    Triple(Icons.Default.TouchApp, "project_info_dialog_gesture_item_long_presses", listOf("project_info_dialog_gesture_item_long_press_normal", "project_info_dialog_gesture_item_long_press_long")),
    Triple(Icons.Default.ThumbUp, "project_info_dialog_gesture_dislike", emptyList())
)

@Composable
fun ProjectInfoDialog(modifier: Modifier = Modifier, close: () -> Unit) {
    val player: PlayerState = LocalPlayerState.current

    AlertDialog(
        close,
        modifier = modifier,
        confirmButton = {
            FilledTonalButton(close) {
                Text(getString("action_close"))
            }
        },
        title = {
            Text(getString("project_info_dialog_title"))
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InfoSection(getString("project_info_dialog_section_project")) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(getString("project_info_dialog_project_\$author").replace("\$author", getString("project_author")), Modifier.align(Alignment.CenterVertically))

                            if (player.context.canOpenUrl()) {
                                Box(
                                    Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    FilledTonalButton({ player.context.openUrl(getString("donation_url")) }) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocalCafe, null)
                                            Text(getString("project_info_dialog_project_donate_button"), softWrap = false)
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(getString("project_info_dialog_project_\$license").replace("\$license", getString("project_license")), Modifier.align(Alignment.CenterVertically))

                            if (player.context.canOpenUrl()) {
                                Box(
                                    Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    FilledTonalButton({ player.context.openUrl(getString("project_url")) }) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(Res.drawable.ic_github), null)
                                            Text(getString("project_info_dialog_project_source_button"), softWrap = false)
                                        }
                                    }
                                }
                            }
                        }

                        FilledTonalButton(
                            {
                                val settings_page: SettingsAppPage = player.app_page_state.Settings
                                player.settings.deps.page!!.openPageOnInterface(player.context, settings_page.settings_interface)

                                if (player.app_page != settings_page) {
                                    player.openAppPage(settings_page)
                                }

                                close()
                            },
                            Modifier.fillMaxWidth()
                        ) {
                            Text(getString("project_info_dialog_dependencies_button"), maxLines = 2)
                        }
                    }
                }

                InfoSection(getString("project_info_dialog_section_gestures")) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        for ((icon, gesture, subgestures) in GESTURES) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(icon, null)

                                Column {
                                    Text(getString(gesture), style = if (subgestures.isEmpty()) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
                                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                                        for (subgesture in subgestures) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text("\u2022")
                                                Text(getString(subgesture))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun InfoSection(name: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(name, style = MaterialTheme.typography.labelLarge)

        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, LocalContentColor.current.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            content()
        }
    }
}
