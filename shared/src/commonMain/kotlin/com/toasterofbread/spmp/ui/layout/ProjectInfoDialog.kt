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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import LocalAppState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import spmp.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.project_info_dialog_title
import spmp.shared.generated.resources.project_info_dialog_section_project
import spmp.shared.generated.resources.donation_url
import spmp.shared.generated.resources.project_info_dialog_project_donate_button
import spmp.shared.generated.resources.project_url
import spmp.shared.generated.resources.project_info_dialog_project_source_button
import spmp.shared.generated.resources.project_info_dialog_dependencies_button
import spmp.shared.generated.resources.project_info_dialog_section_gestures

private val GESTURES: List<Triple<ImageVector, StringResource, List<StringResource>>> = listOf(
    Triple(Icons.Default.SwipeVertical, Res.string.project_info_dialog_gesture_player_swipes, listOf(Res.string.project_info_dialog_gesture_player_swipe_player, Res.string.project_info_dialog_gesture_player_swipe_queue)),
    Triple(Icons.Default.TouchApp, Res.string.project_info_dialog_gesture_item_long_presses, listOf(Res.string.project_info_dialog_gesture_item_long_press_normal, Res.string.project_info_dialog_gesture_item_long_press_long)),
    Triple(Icons.Default.ThumbUp, Res.string.project_info_dialog_gesture_dislike, emptyList())
)

@Composable
fun ProjectInfoDialog(modifier: Modifier = Modifier, close: () -> Unit) {
    val state: SpMp.State = LocalAppState.current

    AlertDialog(
        close,
        modifier = modifier,
        confirmButton = {
            FilledTonalButton(close) {
                Text(stringResource(Res.string.action_close))
            }
        },
        title = {
            Text(stringResource(Res.string.project_info_dialog_title))
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InfoSection(stringResource(Res.string.project_info_dialog_section_project)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(Res.string.`project_info_dialog_project_$author`).replace("\$author", stringResource(Res.string.project_author)), Modifier.align(Alignment.CenterVertically))

                            if (state.context.canOpenUrl()) {
                                Box(
                                    Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val donation_url: String = stringResource(Res.string.donation_url)
                                    FilledTonalButton({ state.context.openUrl(donation_url) }) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocalCafe, null)
                                            Text(stringResource(Res.string.project_info_dialog_project_donate_button), softWrap = false)
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(Res.string.`project_info_dialog_project_$license`).replace("\$license", stringResource(Res.string.project_license)), Modifier.align(Alignment.CenterVertically))

                            if (state.context.canOpenUrl()) {
                                Box(
                                    Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val project_url: String = stringResource(Res.string.project_url)
                                    FilledTonalButton({ state.context.openUrl(project_url) }) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(Res.drawable.ic_github), null)
                                            Text(stringResource(Res.string.project_info_dialog_project_source_button), softWrap = false)
                                        }
                                    }
                                }
                            }
                        }

                        FilledTonalButton(
                            {
                                val settings_page: SettingsAppPage = state.ui.app_page_state.Settings
                                state.settings.deps.page!!.openPageOnInterface(state.context, settings_page.settings_interface)

                                if (state.ui.app_page != settings_page) {
                                    state.ui.openAppPage(settings_page)
                                }

                                close()
                            },
                            Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.project_info_dialog_dependencies_button), maxLines = 2)
                        }
                    }
                }

                InfoSection(stringResource(Res.string.project_info_dialog_section_gestures)) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        for ((icon, gesture, subgestures) in GESTURES) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(icon, null)

                                Column {
                                    Text(stringResource(gesture), style = if (subgestures.isEmpty()) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
                                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                                        for (subgesture in subgestures) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text("\u2022")
                                                Text(stringResource(subgesture))
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
