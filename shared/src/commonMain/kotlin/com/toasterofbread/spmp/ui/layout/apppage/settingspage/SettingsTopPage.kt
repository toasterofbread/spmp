package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import ProgramArguments
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Info
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
import dev.toastbits.composekit.context.PlatformFile
import dev.toastbits.composekit.components.platform.composable.BackHandler
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.util.addUnique
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.util.toggleItemPresence
import dev.toastbits.composekit.util.composable.WidthShrinkText
import dev.toastbits.composekit.components.utils.modifier.horizontal
import com.toasterofbread.spmp.model.settings.SettingsImportExport
import com.toasterofbread.spmp.model.settings.SettingsGroup
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.layout.ProjectInfoDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.*
import PlatformIO
import dev.toastbits.composekit.theme.onAccent
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import okio.buffer
import okio.use
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.s_page_preferences
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.settings_import_error_title
import spmp.shared.generated.resources.settings_import_result_title
import spmp.shared.generated.resources.`settings_import_result_$x_from_file`
import spmp.shared.generated.resources.`settings_import_result_$x_from_default`
import spmp.shared.generated.resources.settings_import_button_import
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.settings_import_prep_title
import spmp.shared.generated.resources.settings_import_category_selection_subtitle
import spmp.shared.generated.resources.project_url
import spmp.shared.generated.resources.notif_copied_x_to_clipboard
import spmp.shared.generated.resources.project_url_name

@Composable
internal fun SettingsAppPage.SettingsTopPage(modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(), top_padding: Dp = 0.dp) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    var importing: Boolean by remember { mutableStateOf(false) }
    if (importing) {
        SettingsImportDialog { importing = false }
    }

    var exporting: Boolean by remember { mutableStateOf(false) }
    val export_categories: MutableList<SettingsGroup> = remember { mutableStateListOf() }

    BackHandler(exporting) {
        exporting = false
    }

    val horizontal_padding: PaddingValues = content_padding.horizontal
    val category_pages: List<SettingsGroup.CategoryPage> = remember { player.settings.group_pages }
    val item_spacing: Dp = 10.dp

    LazyColumn(
        modifier,
        contentPadding =
            PaddingValues(
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
                    stringResource(Res.string.s_page_preferences),
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
                        coroutine_scope.launch {
                            peformExport(player.context, export_categories)
                        }
                    },
                    { exporting = it },
                    { importing = it }
                )

                AnimatedVisibility(!exporting) {
                    ProjectButton(Modifier.padding(start = 20.dp))
                }

                AnimatedVisibility(!exporting) {
                    InfoButton(Modifier.padding(start = 20.dp))
                }
            }
        }

        items(category_pages.filter { it.group.showPage(exporting) }) { page ->
            val title_item: SettingsItem? = remember(page) { page.getTitleItem(player.context) }
            if (title_item == null) {
                return@items
            }

            Row(
                Modifier
                    .padding(bottom = item_spacing)
                    .padding(horizontal_padding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(exporting) {
                    StyledCheckbox(
                        checked = export_categories.contains(page.group),
                        onCheckedChange = { checked ->
                            export_categories.toggleItemPresence(page.group)
                        }
                    )
                }

                Box(Modifier.fillMaxWidth()) {
                    val density: Density = LocalDensity.current

                    // Using IntrinsicHeight breaks some item animations
                    var item_height: Dp by remember { mutableStateOf(0.dp) }

                    title_item.Item(
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
                                    export_categories.toggleItemPresence(page.group)
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
                for (part in ProgramArguments.getVersionMessageComposable(split_lines = true).split("\n")) {
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
                    Text(stringResource(Res.string.action_close))
                }
            },
            title = {
                Text(stringResource(Res.string.settings_import_error_title))
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
                    Text(stringResource(Res.string.action_close))
                }
            },
            title = {
                WidthShrinkText(stringResource(Res.string.settings_import_result_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                        Text(stringResource(Res.string.`settings_import_result_$x_from_file`).replace("\$x", result.directly_imported_count.toString()))
                        Text(stringResource(Res.string.`settings_import_result_$x_from_default`).replace("\$x", result.default_imported_count.toString()))
                    }
                }
            }
        )
        return
    }

    import_data?.also { data ->
        val included_groups: List<SettingsGroup> = (data.getGroups(context) ?: emptyList()).ifEmpty { context.settings.groups_with_page }
        val import_groups: MutableList<SettingsGroup> = remember {
            mutableStateListOf<SettingsGroup>().apply {
                addAll(included_groups)
            }
        }

        AlertDialog(
            modifier = modifier,
            onDismissRequest = onFinished,
            confirmButton = {
                Button(
                    {
                        try {
                            import_result = SettingsImportExport.importSettingsData(context, data, import_groups)
                        }
                        catch (e: Throwable) {
                            import_error = e
                        }
                    },
                    enabled = import_groups.isNotEmpty()
                ) {
                    Text(stringResource(Res.string.settings_import_button_import))
                }
            },
            dismissButton = {
                Row {
                    IconButton({
                        if (import_groups.size == included_groups.size) {
                            import_groups.clear()
                        }
                        else {
                            for (category in included_groups) {
                                import_groups.addUnique(category)
                            }
                        }
                    }) {
                        Icon(Icons.Default.SelectAll, null)
                    }

                    Button(onFinished) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            },
            title = {
                Text(stringResource(Res.string.settings_import_prep_title))
            },
            text = {
                Column {
                    Text(stringResource(Res.string.settings_import_category_selection_subtitle), style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(included_groups) { group ->
                            val title: String = group.getTitle().ifEmpty {
                                group.group_key.lowercase().replaceFirstChar { it.uppercaseChar() }
                            }

                            Row(
                                Modifier.clickable {
                                    import_groups.toggleItemPresence(group)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyledCheckbox(
                                    import_groups.contains(group),
                                    { import_groups.toggleItemPresence(group) }
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

    val project_url: String = stringResource(Res.string.project_url)
    val project_url_name: String = stringResource(Res.string.project_url_name)
    val notif_copied_x_to_clipboard: String = stringResource(Res.string.notif_copied_x_to_clipboard)

    fun copyProjectUrl() {
        clipboard.setText(AnnotatedString(project_url))
        player.context.sendToast(notif_copied_x_to_clipboard.replace("\$x", project_url_name))
    }

    Icon(
        painterResource(Res.drawable.ic_github),
        null,
        modifier.platformClickable(
            onClick = {
                if (player.context.canOpenUrl()) {
                    player.context.openUrl(project_url)
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
private fun InfoButton(modifier: Modifier = Modifier) {
    var show_info_dialog: Boolean by remember { mutableStateOf(false) }

    if (show_info_dialog) {
        ProjectInfoDialog { show_info_dialog = false }
    }

    Icon(
        Icons.Default.Info,
        null,
        modifier.platformClickable(
            onClick = { show_info_dialog = !show_info_dialog }
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
            checkmarkColor = player.theme.onAccent
        )
    )
}

@OptIn(DelicateCoroutinesApi::class, FormatStringsInDatetimeFormats::class)
private suspend fun peformExport(context: AppContext, groups: List<SettingsGroup>) {
    val datetime: String =
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(
                LocalDateTime.Format {
                    byUnicodePattern("yyyy-MM-dd_HH:mm:ss")
                }
            )
    val filename: String = getString(Res.string.`settings_export_filename_$date`).replace("\$date", datetime)

    context.promptUserForFileCreation("application/json", filename, persist = false) { path ->
        if (path == null) {
            return@promptUserForFileCreation
        }

        GlobalScope.launch(Dispatchers.PlatformIO) {
            val settings_data: SettingsImportExport.SettingsExportData =
                SettingsImportExport.exportSettingsData(
                    prefs = context.getPrefs(),
                    groups = groups
                )

            val file: PlatformFile = context.getUserDirectoryFile(path)!!
            file.outputStream().buffer().use { writer ->
                writer.writeUtf8(Json.encodeToString(settings_data))
                writer.flush()
            }
        }
    }
}

@Composable
private fun ImportExportButtons(
    exporting: Boolean,
    export_groups: MutableList<SettingsGroup>,
    group_pages: List<SettingsGroup.CategoryPage>,
    beginExport: () -> Unit,
    completeExport: () -> Unit,
    setExporting: (Boolean) -> Unit,
    setImporting: (Boolean) -> Unit
) {
    val initial_icon_modifier: Modifier = Modifier.alpha(0.5f)

    AnimatedVisibility(
        exporting,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        IconButton({
            if (export_groups.size == group_pages.size) {
                export_groups.clear()
            }
            else {
                for (page in group_pages) {
                    export_groups.addUnique(page.group)
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
