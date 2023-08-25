package com.toasterofbread.spmp.ui.layout.prefspage

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.composesettings.ui.SettingsPageWithItems
import com.toasterofbread.composesettings.ui.item.SettingsComposableItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.getNavigationBarHeightDp
import com.toasterofbread.utils.composable.RecomposeOnInterval
import com.toasterofbread.utils.roundTo

@Composable
private fun SizeIndicator(
    label: String,
    show_indicator: Boolean = true,
    show_percent_of_screen: Boolean = false,
    getHeight: @Composable Density.(PlatformContext) -> Dp,
) {
    val player = LocalPlayerState.current
    val density = LocalDensity.current

    RecomposeOnInterval(500) {
        it

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val indicator_height: Dp = getHeight(density, player.context)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(label)

                Spacer(Modifier.fillMaxWidth().weight(1f))

                Text(indicator_height.toString())

                if (show_percent_of_screen) {
                    val screen_height: Dp = player.context.getScreenHeight()
                    val height_percent: Float = indicator_height / screen_height
                    Text("(${height_percent.roundTo(2).toString().padEnd(4, '0')}%)")
                }

                Text("\u2022", Modifier.alpha(if (it) 1f else 0f))
            }

            if (show_indicator) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(indicator_height)
                        .background(Color.Red)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun getUiDebugInfoPage(): SettingsPage =
    SettingsPageWithItems(
        { getString("s_subpage_ui_debug_info") },
        {
            listOf(
                // Window height
                SettingsComposableItem {
                    Column(Modifier.fillMaxWidth()) {
                        SizeIndicator("Screen height", show_indicator = false) { context ->
                            context.getScreenHeight()
                        }
                    }
                },

                // Status bar height
                SettingsComposableItem {
                    Column(Modifier.fillMaxWidth()) {
                        SizeIndicator("Status bar height") { context ->
                            context.getStatusBarHeight()
                        }
                    }
                },

                // Navigation bar height
                SettingsComposableItem {
                    Column(Modifier.fillMaxWidth()) {
                        SizeIndicator("Navigation bar height") { context ->
                            context.getNavigationBarHeightDp()
                        }
                    }
                },

                // Keyboard height
                SettingsComposableItem {
                    Column(Modifier.fillMaxWidth()) {
                        SizeIndicator("Keyboard height", show_percent_of_screen = true) { context ->
                            context.getImeInsets()!!.getBottom(this).toDp()
                        }
                        OutlinedTextField(
                            "",
                            {},
                            Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Test field")
                            }
                        )
                    }
                }
            )
        }
    )
