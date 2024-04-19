package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import ProgramArguments
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.platform.PlatformFile
import dev.toastbits.composekit.platform.composable.BackHandler
import dev.toastbits.composekit.platform.composable.platformClickable
import dev.toastbits.composekit.platform.vibrateShort
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.utils.common.addUnique
import dev.toastbits.composekit.utils.common.thenIf
import dev.toastbits.composekit.utils.common.toggleItemPresence
import dev.toastbits.composekit.utils.composable.WidthShrinkText
import dev.toastbits.composekit.utils.modifier.horizontal
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsImportExport
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import spms.socketapi.shared.SPMS_API_VERSION
import java.text.SimpleDateFormat
import java.util.Date
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.*

@Composable
internal fun SettingsAppPage.SettingsTopPage(modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(), top_padding: Dp = 0.dp) {
    val player: PlayerState = LocalPlayerState.current

    var importing: Boolean by remember { mutableStateOf(false) }
    if (importing) {
        SettingsImportDialog { importing = false }
    }

    var exporting: Boolean by remember { mutableStateOf(false) }
    val export_categories: MutableList<SettingsCategory> = remember { mutableStateListOf() }

    BackHandler(exporting) {
        exporting = false
    }

    val horizontal_padding: PaddingValues = content_padding.horizontal
    val category_pages: List<SettingsCategory.CategoryPage> = remember { SettingsCategory.pages }
    val item_spacing: Dp = 10.dp

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(
            top = top_padding,
            bottom = content_padding.calculateBottomPadding() + PREFS_PAGE_EXTRA_PADDING_DP.dp
        )
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = item_spacing + 10.dp).padding(horizontal_padding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    getString("s_page_preferences"),
                    style = MaterialTheme.typography.displaySmall
                )

                Spacer(Modifier.fillMaxWidth().weight(1f))

                ImportExportButtons(
                    exporting,
                    export_categories,
                    category_pages,
                    {
                        export_categories.clear()
                        exporting = true
                    },
                    {
                        exporting = false
                        peformExport(player.context, export_categories)
                    },
                    { exporting = it },
                    { importing = it }
                )

                AnimatedVisibility(!exporting) {
                    ProjectButton(Modifier.padding(start = 20.dp))
                }
            }
        }

        items(category_pages.filter { it.category.showPage(exporting) }) { page ->
            val title_item: SettingsItem? = remember(page) { page.getTitleItem(player.context) }
            if (title_item == null) {
                return@items
            }

            Row(Modifier.padding(bottom = item_spacing), verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(exporting) {
                    StyledCheckbox(
                        checked = export_categories.contains(page.category),
                        onCheckedChange = { checked ->
                            export_categories.toggleItemPresence(page.category)
                        }
                    )
                }

                Box(Modifier.fillMaxWidth().padding(horizontal_padding)) {
                    val density: Density = LocalDensity.current

                    // Using IntrinsicHeight breaks some item animations
                    var item_height: Dp by remember { mutableStateOf(0.dp) }

                    title_item.Item(
                        settings_interface,
                        settings_interface::openPageById,
                        settings_interface::openPage,
                        Modifier
                            .onSizeChanged {
                                item_height = with (density) { it.height.toDp() }
                            }
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(item_height)
                            .thenIf(exporting) {
                                clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    export_categories.toggleItemPresence(page.category)
                                }
                            }
                    )
                }
            }
        }

        item {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .padding(horizontal_padding)
                    .alpha(0.5f),
                horizontalArrangement = Arrangement.Center
            ) {
                for (part in remember { ProgramArguments.getVersionMessage(split_lines = true).split("\n") }) {
                    SelectionContainer {
                        Text(
                            part,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsImportDialog(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val context: AppContext = LocalPlayerState.current.context
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    var import_data: SettingsImportExport.SettingsExportData? by remember { mutableStateOf(null) }
    var import_error: Throwable? by remember { mutableStateOf(null) }
    var import_result: SettingsImportExport.ImportResult? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        context.promptUserForFile(setOf("text/plain", "application/json"), persist = false) { path ->
            if (path != null) {
                coroutine_scope.launch {
                    try {
                        import_data = SettingsImportExport.loadSettingsFile(context.getUserDirectoryFile(path)!!)
                    }
                    catch (e: Throwable) {
                        import_error = e
                    }
                }
            }
            else {
                onFinished()
            }
        }
    }

    import_error?.also { error ->
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onFinished,
            confirmButton = {
                Button(onFinished) {
                    Text(getString("action_close"))
                }
            },
            title = {
                Text(getString("settings_import_error_title"))
            },
            text = {
                ErrorInfoDisplay(error, onDismiss = null)
            }
        )
        return
    }

    import_result?.also { result ->
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onFinished,
            confirmButton = {
                Button(onFinished) {
                    Text(getString("action_close"))
                }
            },
            title = {
                WidthShrinkText(getString("settings_import_result_title"))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                        Text(getString("settings_import_result_\$x_from_file").replace("\$x", result.directly_imported_count.toString()))
                        Text(getString("settings_import_result_\$x_from_default").replace("\$x", result.default_imported_count.toString()))
                    }
                }
            }
        )
        return
    }

    import_data?.also { data ->
        val included_categories: List<SettingsCategory> = (data.getCategories() ?: emptyList()).ifEmpty { SettingsCategory.with_page }
        val import_categories: MutableList<SettingsCategory> = remember {
            mutableStateListOf<SettingsCategory>().apply {
                addAll(included_categories)
            }
        }

        AlertDialog(
            modifier = modifier,
            onDismissRequest = onFinished,
            confirmButton = {
                Button(
                    {
                        try {
                            import_result = SettingsImportExport.importSettingsData(context.getPrefs(), data, import_categories)
                        }
                        catch (e: Throwable) {
                            import_error = e
                        }
                    },
                    enabled = import_categories.isNotEmpty()
                ) {
                    Text(getString("settings_import_button_import"))
                }
            },
            dismissButton = {
                Row {
                    IconButton({
                        if (import_categories.size == included_categories.size) {
                            import_categories.clear()
                        }
                        else {
                            for (category in included_categories) {
                                import_categories.addUnique(category)
                            }
                        }
                    }) {
                        Icon(Icons.Default.SelectAll, null)
                    }

                    Button(onFinished) {
                        Text(getString("action_cancel"))
                    }
                }
            },
            title = {
                Text(getString("settings_import_prep_title"))
            },
            text = {
                Column {
                    Text(getString("settings_import_category_selection_subtitle"), style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(included_categories) { category ->
                            val title: String = category.getPage()?.name
                                ?: category.id.lowercase().replaceFirstChar { it.uppercaseChar() }

                            Row(
                                Modifier.clickable {
                                    import_categories.toggleItemPresence(category)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyledCheckbox(
                                    import_categories.contains(category),
                                    { import_categories.toggleItemPresence(category) }
                                )

                                Text(title, Modifier.fillMaxWidth().weight(1f), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ProjectButton(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    fun copyProjectUrl() {
        clipboard.setText(AnnotatedString(getString("project_url")))
        player.context.sendToast(getString("notif_copied_x_to_clipboard").replace("\$x", getString("project_url_name")))
    }

    Icon(
        painterResource(Res.drawable.ic_github),
        null,
        modifier.platformClickable(
            onClick = {
                if (player.context.canOpenUrl()) {
                    player.context.openUrl(getString("project_url"))
                }
                else {
                    copyProjectUrl()
                }
            },
            onAltClick = {
                if (player.context.canOpenUrl()) {
                    copyProjectUrl()
                    player.context.vibrateShort()
                }
            }
        )
    )
}

@Composable
private fun StyledCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val player: PlayerState = LocalPlayerState.current
    Checkbox(
        checked,
        onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = player.theme.accent,
            uncheckedColor = player.theme.accent,
            checkmarkColor = player.theme.on_accent
        )
    )
}

@OptIn(DelicateCoroutinesApi::class)
private fun peformExport(context: AppContext, categories: List<SettingsCategory>) {
    val datetime: String = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Date())
    val filename: String = getString("settings_export_filename_\$date").replace("\$date", datetime)

    context.promptUserForFileCreation("application/json", filename, persist = false) { path ->
        if (path == null) {
            return@promptUserForFileCreation
        }

        GlobalScope.launch(Dispatchers.IO) {
            val settings_data: SettingsImportExport.SettingsExportData =
                SettingsImportExport.exportSettingsData(
                    prefs = context.getPrefs(),
                    categories = categories
                )

            val file: PlatformFile = context.getUserDirectoryFile(path)!!
            file.outputStream().writer().use { writer ->
                writer.write(Json.encodeToString(settings_data))
                writer.flush()
            }
        }
    }
}

@Composable
private fun ImportExportButtons(
    exporting: Boolean,
    export_categories: MutableList<SettingsCategory>,
    category_pages: List<SettingsCategory.CategoryPage>,
    beginExport: () -> Unit,
    completeExport: () -> Unit,
    setExporting: (Boolean) -> Unit,
    setImporting: (Boolean) -> Unit
) {
    val initial_icon_modifier: Modifier = Modifier.alpha(0.5f)

    AnimatedVisibility(exporting) {
        IconButton({
            if (export_categories.size == category_pages.size) {
                export_categories.clear()
            }
            else {
                for (page in category_pages) {
                    export_categories.addUnique(page.category)
                }
            }
        }) {
            Icon(Icons.Default.SelectAll, null)
        }
    }

    Crossfade(exporting) { ex ->
        if (!ex) {
            IconButton({ beginExport() }) {
                Icon(Icons.Default.Save, null, initial_icon_modifier)
            }
        }
        else {
            IconButton({ setExporting(false) }) {
                Icon(Icons.Default.Close, null)
            }
        }
    }

    Crossfade(exporting) { ex ->
        if (!ex) {
            IconButton({ setImporting(true) }) {
                Icon(Icons.Default.FolderOpen, null, initial_icon_modifier)
            }
        }
        else {
            IconButton({ completeExport() }) {
                Icon(Icons.Default.Done, null)
            }
        }
    }
}
