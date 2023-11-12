package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
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
import com.toasterofbread.composekit.settings.ui.SettingsPage
import com.toasterofbread.composekit.settings.ui.SettingsPageWithItems
import com.toasterofbread.composekit.settings.ui.item.SettingsComposableItem
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.composekit.utils.common.roundTo
import com.toasterofbread.composekit.utils.composable.RecomposeOnInterval

@Composable
private fun SizeIndicator(
    label: String,
    show_indicator: Boolean = true,
    show_percent_of_screen: Boolean = false,
    getHeight: @Composable Density.(AppContext) -> Any?,
) {
    val player = LocalPlayerState.current
    val density = LocalDensity.current

    RecomposeOnInterval(500) {
        it

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val value: Any? = getHeight(density, player.context)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(label)

                Spacer(Modifier.fillMaxWidth().weight(1f))

                if (value != null) {
                    Text(value.toString())
                }

                if (show_percent_of_screen && value is Dp) {
                    val height_percent: Float = value / player.screen_size.height
                    Text("(${height_percent.roundTo(2).toString().padEnd(4, '0')}%)")
                }

                Text("\u2022", Modifier.alpha(if (it) 1f else 0f))
            }

            if (show_indicator && value is Dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(value)
                        .background(Color.Red)
                )
            }
        }
    }
}

fun getUiDebugInfoPage(): SettingsPage =
    SettingsPageWithItems(
        { getString("s_subpage_ui_debug_info") },
        {
            listOf(
                SettingsComposableItem {
                    SizeIndicator("Displaying above navigation bar") { context ->
                        context.isDisplayingAboveNavigationBar()
                    }
                },

                // Window height
                SettingsComposableItem {
                    SizeIndicator("Screen height", show_indicator = false) { context ->
                        LocalPlayerState.current.screen_size.height
                    }
                },

                // Keyboard height
                SettingsComposableItem {
                    Column(Modifier.fillMaxWidth()) {
                        SizeIndicator("Keyboard height", show_percent_of_screen = true) { context ->
                            WindowInsets.ime.getBottom(this).toDp()
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
