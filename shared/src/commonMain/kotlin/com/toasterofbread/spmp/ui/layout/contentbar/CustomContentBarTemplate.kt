package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import com.toasterofbread.spmp.ui.theme.appHover

enum class CustomContentBarTemplate {
    NAVIGATION;

    fun getName(): String =
        when (this) {
            NAVIGATION -> getString("content_bar_template_navigation")
        }

    fun getIcon(): ImageVector =
        when (this) {
            NAVIGATION -> Icons.Default.Widgets
        }

    fun getElements(): List<ContentBarElement> =
        when (this) {
            NAVIGATION -> listOf(
                ContentBarElementButton(ContentBarElementButton.Type.FEED),
                ContentBarElementButton(ContentBarElementButton.Type.LIBRARY),
                ContentBarElementButton(ContentBarElementButton.Type.SEARCH),
                ContentBarElementButton(ContentBarElementButton.Type.RADIOBUILDER),
                ContentBarElementButton(ContentBarElementButton.Type.RELOAD),
                ContentBarElementData(
                    ContentBarElement.Type.PINNED_ITEMS,
                    size_mode = ContentBarElement.SizeMode.FILL
                ).toElement(),
                ContentBarElementButton(ContentBarElementButton.Type.PROFILE),
                ContentBarElementButton(ContentBarElementButton.Type.CONTROL),
                ContentBarElementButton(ContentBarElementButton.Type.SETTINGS)
            )
        }

    @Composable
    private fun BarPreview(modifier: Modifier = Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val bar: CustomContentBar = remember { CustomContentBar("", element_data = getElements().map { it.getData() }) }

        Column(
            modifier
                .background(player.theme.card, RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(getIcon(), null)
                Text(getName())
            }

            bar.CustomBarContent(
                modifier = Modifier.background(player.theme.vibrant_accent, RoundedCornerShape(16.dp)),
                background_colour = Theme.Colour.VIBRANT_ACCENT,
                vertical = false,
                content_padding = PaddingValues(5.dp),
                buttonContent = { _, element, size ->
                    element.Element(false, size, enable_interaction = false)
                }
            )
        }
    }

    companion object {
        @Composable
        fun SelectionDialog(modifier: Modifier = Modifier, onSelected: (CustomContentBarTemplate?) -> Unit) {
            val player: PlayerState = LocalPlayerState.current

            AlertDialog(
                { onSelected(null) },
                modifier = modifier,
                confirmButton = {
                    Button(
                        { onSelected(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = player.theme.background,
                            contentColor = player.theme.on_background
                        ),
                        modifier = Modifier.appHover(true)
                    ) {
                        Text(getString("action_cancel"))
                    }
                },
                title = {
                    Text(getString("content_bar_editor_template_dialog_title"))
                },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries) { template ->
                            template.BarPreview(
                                Modifier
                                    .platformClickable(
                                        onClick = { onSelected(template) }
                                    )
                                    .appHover(true)
                            )
                        }
                    }
                }
            )
        }
    }
}
