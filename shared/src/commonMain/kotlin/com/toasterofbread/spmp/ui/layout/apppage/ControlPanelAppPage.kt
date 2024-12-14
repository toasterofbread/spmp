package com.toasterofbread.spmp.ui.layout.apppage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.composable.copy
import dev.toastbits.composekit.components.utils.modifier.horizontal
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.controlpanelpage.ControlPanelDownloadsPage
import com.toasterofbread.spmp.ui.layout.apppage.controlpanelpage.ControlPanelServerPage
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.control_panel_downloads_label
import spmp.shared.generated.resources.control_panel_server_label
import spmp.shared.generated.resources.control_panel_downloads_title
import spmp.shared.generated.resources.control_panel_server_title

class ControlPanelAppPage(override val state: AppPageState): AppPage() {
    private enum class Page {
        DOWNLOADS, SERVER;

        val should_show: Boolean get() =
            when (this) {
                SERVER -> Platform.DESKTOP.isCurrent()
                else -> true
            }

        val icon: ImageVector get() =
            when (this) {
                DOWNLOADS -> Icons.Default.Download
                SERVER -> Icons.Default.Cloud
            }

        val label: String
            @Composable
            get() =
                when (this) {
                    DOWNLOADS -> stringResource(Res.string.control_panel_downloads_label)
                    SERVER -> stringResource(Res.string.control_panel_server_label)
                }

        val title: String
            @Composable
            get() =
                when (this) {
                    DOWNLOADS -> stringResource(Res.string.control_panel_downloads_title)
                    SERVER -> stringResource(Res.string.control_panel_server_title)
                }

        @Composable
        fun Page(modifier: Modifier, multiselect_context: MediaItemMultiSelectContext, content_padding: PaddingValues) =
            when (this) {
                DOWNLOADS -> ControlPanelDownloadsPage(modifier, multiselect_context, content_padding)
                SERVER -> ControlPanelServerPage(modifier, multiselect_context, content_padding)
            }
    }

        @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        Column(
            modifier.padding(top = content_padding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val horizontal_padding: PaddingValues = content_padding.horizontal
            var current_page: Page by remember { mutableStateOf(Page.entries.first { it.should_show }) }

            Text(current_page.title, Modifier.padding(horizontal_padding), style = MaterialTheme.typography.displayMedium)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = horizontal_padding
            ) {
                items(Page.entries) { page ->
                    if (!page.should_show) {
                        return@items
                    }

                    FilterChip(
                        selected = page == current_page,
                        onClick = { current_page = page },
                        leadingIcon = {
                            Icon(page.icon, null)
                        },
                        label = {
                            Text(page.label)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            iconColor = LocalContentColor.current
                        )
                    )
                }
            }

            Crossfade(current_page, Modifier.fillMaxHeight().weight(1f).padding(top = 20.dp)) { page ->
                page.Page(Modifier.fillMaxSize(), multiselect_context, content_padding.copy(top = 0.dp))
            }
        }
    }
}
