package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.utils.common.addUnique
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.common.toggleItemPresence
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsImportExport
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsAppPage.SettingsTopPage(modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(), top_padding: Dp = 0.dp) {
    val player: PlayerState = LocalPlayerState.current

    var importing: Boolean by remember { mutableStateOf(false) }
    if (importing) {
        SettingsImportDialog { importing = false }
    }

    var exporting: Boolean by remember { mutableStateOf(false) }
    val export_categories: MutableList<SettingsCategory> = remember { mutableStateListOf() }

    fun beginExport() {
        export_categories.clear()
        exporting = true
    }

    fun completeExport() {
        exporting = false
        peformExport(player.context, export_categories)
    }

    BackHandler(exporting) {
        exporting = false
    }

    val horizontal_padding: PaddingValues = content_padding.horizontal
    val category_pages: List<SettingsCategory.Page> = remember { SettingsCategory.pages }

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(
            top = top_padding,
            bottom = content_padding.calculateBottomPadding() + PREFS_PAGE_EXTRA_PADDING_DP.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val export_import_modifier: Modifier = Modifier.alpha(0.5f)

            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).padding(horizontal_padding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    getString("s_page_preferences"),
                    style = MaterialTheme.typography.displaySmall
                )

                Spacer(Modifier.fillMaxWidth().weight(1f))

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
                            Icon(Icons.Default.Publish, null, export_import_modifier)
                        }
                    }
                    else {
                        IconButton({ exporting = false }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }

                Crossfade(exporting) { ex ->
                    if (!ex) {
                        IconButton({ importing = true }) {
                            Icon(Icons.Default.Download, null, export_import_modifier)
                        }
                    }
                    else {
                        IconButton({ completeExport() }) {
                            Icon(Icons.Default.Done, null)
                        }
                    }
                }

                AnimatedVisibility(!exporting) {
                    ProjectButton(Modifier.padding(start = 20.dp))
                }
            }
        }

        item {
            val ytm_auth_populated: Boolean = ytm_auth.get().isNotEmpty()

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(exporting && ytm_auth_populated) {
                    StyledCheckbox(
                        checked = false,
                        onCheckedChange = { checked ->
                            TODO()
                        }
                    )
                }

                Box(Modifier.fillMaxWidth().padding(horizontal_padding)) {
                    val density: Density = LocalDensity.current

                    // Using IntrinsicHeight breaks animation of item
                    var item_height: Dp by remember { mutableStateOf(0.dp) }

                    val item: SettingsItem = remember { getYtmAuthItem(player.context, ytm_auth, true) }
                    item.Item(
                        settings_interface,
                        settings_interface::openPageById,
                        settings_interface::openPage,
                        Modifier.onSizeChanged {
                            item_height = with (density) { it.height.toDp() }
                        }
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(item_height)
                            .thenIf(exporting && ytm_auth_populated) {
                                clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    TODO()
                                }
                            }
                    )
                }
            }
        }

        items(category_pages) { page ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(exporting) {
                    StyledCheckbox(
                        checked = export_categories.contains(page.category),
                        onCheckedChange = { checked ->
                            if (checked) {
                                export_categories.addUnique(page.category)
                            }
                            else {
                                export_categories.remove(page.category)
                            }
                        }
                    )
                }

                ElevatedCard(
                    onClick = {
                        if (!exporting) {
                            current_category = page
                        }
                        else {
                            export_categories.toggleItemPresence(page.category)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal_padding),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f),
                        contentColor = player.theme.on_background
                    )
                ) {
                    Row(
                        Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Icon(page.getIcon(), null)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(page.title, style = MaterialTheme.typography.titleMedium)
                            Text(page.description, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        item {
            val version_string: String = "v${getString("version_string")}"
            val on_release_commit: Boolean = ProjectBuildConfig.GIT_TAG == version_string

            Text(
                if (on_release_commit) {
                    getString("info_using_release_\$x")
                        .replace("\$x", version_string)
                }
                else {
                    getString("info_using_non_release_commit_\$x")
                        .replace("\$x", ProjectBuildConfig.GIT_COMMIT_HASH?.take(7).toString())
                },
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .padding(horizontal_padding)
                    .alpha(0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun SettingsImportDialog(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val context: AppContext = LocalPlayerState.current.context
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    var import_data: SettingsImportExport.SettingsExportData? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        context.promptUserForFile(setOf("text/plain", "application/json"), persist = false) { path ->
            if (path != null) {
                coroutine_scope.launch {
                    import_data = SettingsImportExport.loadSettingsFile(context.getUserDirectoryFile(path))
                }
            }
            else {
                onFinished()
            }
        }
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
                        SettingsImportExport.importData(context, data, import_categories)
                        onFinished()
                    },
                    enabled = import_categories.isNotEmpty()
                ) {
                    Text("Import")
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
                Text("Importing settings")
            },
            text = {
                Column {
                    Text("Select categories to import", style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(included_categories) { category ->
                            val title: String = category.getPage()?.title
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

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ProjectButton(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    fun copyProjectUrl() {
        clipboard.setText(AnnotatedString(getString("project_url")))
        player.context.sendToast(getString("notif_copied_x_to_clipboard").replace("\$x", getString("project_url_name")))
    }

    Icon(
        painterResource("drawable/ic_github.xml"),
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

    context.promptUserForJsonCreation(filename, persist = false) { path ->
        if (path == null) {
            return@promptUserForJsonCreation
        }

        GlobalScope.launch {
            SettingsImportExport.exportSettings(
                context = context,
                file = context.getUserDirectoryFile(path),
                categories = categories
            )
        }
    }
}
