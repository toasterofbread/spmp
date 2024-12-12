package com.toasterofbread.spmp.ui.layout.apppage.settingspage

//
//@Composable
//internal fun SettingsAppPage.SettingsTopPage(modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(), top_padding: Dp = 0.dp) {
//    val player: PlayerState = LocalPlayerState.current
//    val coroutine_scope: CoroutineScope = rememberCoroutineScope()
//
//    var importing: Boolean by remember { mutableStateOf(false) }
//    if (importing) {
//        SettingsImportDialog { importing = false }
//    }
//
//    var exporting: Boolean by remember { mutableStateOf(false) }
//    val export_categories: MutableList<SettingsGroup> = remember { mutableStateListOf() }
//
//    BackHandler(exporting) {
//        exporting = false
//    }
//
//    val horizontal_padding: PaddingValues = content_padding.horizontal
//    val category_pages: List<PlatformSettingsGroupScreen> = remember { player.settings.group_pages }
//    val item_spacing: Dp = 10.dp
//
//    LazyColumn(
//        modifier,
//        contentPadding =
//            PaddingValues(
//                top = top_padding,
//                bottom = content_padding.calculateBottomPadding() + PREFS_PAGE_EXTRA_PADDING_DP.dp
//            )
//    ) {
//        item {
//            Row(
//                Modifier.fillMaxWidth().padding(bottom = item_spacing + 10.dp).padding(horizontal_padding),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    stringResource(Res.string.s_page_preferences),
//                    style = MaterialTheme.typography.displaySmall
//                )
//
//                Spacer(Modifier.fillMaxWidth().weight(1f))
//
//                ImportExportButtons(
//                    exporting,
//                    export_categories,
//                    category_pages,
//                    {
//                        export_categories.clear()
//                        exporting = true
//                    },
//                    {
//                        exporting = false
//                        coroutine_scope.launch {
//                            peformExport(player.context, export_categories)
//                        }
//                    },
//                    { exporting = it },
//                    { importing = it }
//                )
//
//                AnimatedVisibility(!exporting) {
//                    ProjectButton(Modifier.padding(start = 20.dp))
//                }
//
//                AnimatedVisibility(!exporting) {
//                    InfoButton(Modifier.padding(start = 20.dp))
//                }
//            }
//        }
//
//        items(category_pages.filter { it.group.showPage(exporting) }) { page ->
//            val title_item: SettingsItem? = remember(page) { page.getTitleItem(player.context) }
//            if (title_item == null) {
//                return@items
//            }
//
//            Row(
//                Modifier
//                    .padding(bottom = item_spacing)
//                    .padding(horizontal_padding),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                AnimatedVisibility(exporting) {
//                    StyledCheckbox(
//                        checked = export_categories.contains(page.group),
//                        onCheckedChange = { checked ->
//                            export_categories.toggleItemPresence(page.group)
//                        }
//                    )
//                }
//
//                Box(Modifier.fillMaxWidth()) {
//                    val density: Density = LocalDensity.current
//
//                    // Using IntrinsicHeight breaks some item animations
//                    var item_height: Dp by remember { mutableStateOf(0.dp) }
//
//                    title_item.Item(
//                        Modifier
//                            .onSizeChanged {
//                                item_height = with (density) { it.height.toDp() }
//                            }
//                    )
//
//                    Box(
//                        Modifier
//                            .fillMaxWidth()
//                            .height(item_height)
//                            .thenIf(exporting) {
//                                clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
//                                    export_categories.toggleItemPresence(page.group)
//                                }
//                            }
//                    )
//                }
//            }
//        }
//
//        item {
//            FlowRow(
//                Modifier
//                    .fillMaxWidth()
//                    .padding(top = 10.dp)
//                    .padding(horizontal_padding)
//                    .alpha(0.5f),
//                horizontalArrangement = Arrangement.Center
//            ) {
//                for (part in ProgramArguments.getVersionMessageComposable(split_lines = true).split("\n")) {
//                    SelectionContainer {
//                        Text(
//                            part,
//                            fontSize = 12.sp,
//                            textAlign = TextAlign.Center
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//internal fun SettingsImportDialog(modifier: Modifier = Modifier, onFinished: () -> Unit) {
//    val context: AppContext = LocalPlayerState.current.context
//    val coroutine_scope: CoroutineScope = rememberCoroutineScope()
//
//    var import_data: SettingsImportExport.SettingsExportData? by remember { mutableStateOf(null) }
//    var import_error: Throwable? by remember { mutableStateOf(null) }
//    var import_result: SettingsImportExport.ImportResult? by remember { mutableStateOf(null) }
//
//    LaunchedEffect(Unit) {
//        context.promptUserForFile(setOf("text/plain", "application/json"), persist = false) { path ->
//            if (path != null) {
//                coroutine_scope.launch {
//                    try {
//                        import_data = SettingsImportExport.loadSettingsFile(context.getUserDirectoryFile(path)!!)
//                    }
//                    catch (e: Throwable) {
//                        import_error = e
//                    }
//                }
//            }
//            else {
//                onFinished()
//            }
//        }
//    }
//
//    import_error?.also { error ->
//        AlertDialog(
//            modifier = modifier,
//            onDismissRequest = onFinished,
//            confirmButton = {
//                Button(onFinished) {
//                    Text(stringResource(Res.string.action_close))
//                }
//            },
//            title = {
//                Text(stringResource(Res.string.settings_import_error_title))
//            },
//            text = {
//                ErrorInfoDisplay(error, onDismiss = null)
//            }
//        )
//        return
//    }
//
//    import_result?.also { result ->
//        AlertDialog(
//            modifier = modifier,
//            onDismissRequest = onFinished,
//            confirmButton = {
//                Button(onFinished) {
//                    Text(stringResource(Res.string.action_close))
//                }
//            },
//            title = {
//                WidthShrinkText(stringResource(Res.string.settings_import_result_title))
//            },
//            text = {
//                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
//                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
//                        Text(stringResource(Res.string.`settings_import_result_$x_from_file`).replace("\$x", result.directly_imported_count.toString()))
//                        Text(stringResource(Res.string.`settings_import_result_$x_from_default`).replace("\$x", result.default_imported_count.toString()))
//                    }
//                }
//            }
//        )
//        return
//    }
//
//    import_data?.also { data ->
//        val included_groups: List<SettingsGroup> = (data.getGroups(context) ?: emptyList()).ifEmpty { context.settings.groups_with_page }
//        val import_groups: MutableList<SettingsGroup> = remember {
//            mutableStateListOf<SettingsGroup>().apply {
//                addAll(included_groups)
//            }
//        }
//
//        AlertDialog(
//            modifier = modifier,
//            onDismissRequest = onFinished,
//            confirmButton = {
//                Button(
//                    {
//                        try {
//                            import_result = SettingsImportExport.importSettingsData(context, data, import_groups)
//                        }
//                        catch (e: Throwable) {
//                            import_error = e
//                        }
//                    },
//                    enabled = import_groups.isNotEmpty()
//                ) {
//                    Text(stringResource(Res.string.settings_import_button_import))
//                }
//            },
//            dismissButton = {
//                Row {
//                    IconButton({
//                        if (import_groups.size == included_groups.size) {
//                            import_groups.clear()
//                        }
//                        else {
//                            for (category in included_groups) {
//                                import_groups.addUnique(category)
//                            }
//                        }
//                    }) {
//                        Icon(Icons.Default.SelectAll, null)
//                    }
//
//                    Button(onFinished) {
//                        Text(stringResource(Res.string.action_cancel))
//                    }
//                }
//            },
//            title = {
//                Text(stringResource(Res.string.settings_import_prep_title))
//            },
//            text = {
//                Column {
//                    Text(stringResource(Res.string.settings_import_category_selection_subtitle), style = MaterialTheme.typography.titleMedium)
//                    LazyColumn {
//                        items(included_groups) { group ->
//                            val title: String = group.getTitle().ifEmpty {
//                                group.group_key.lowercase().replaceFirstChar { it.uppercaseChar() }
//                            }
//
//                            Row(
//                                Modifier.clickable {
//                                    import_groups.toggleItemPresence(group)
//                                },
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                StyledCheckbox(
//                                    import_groups.contains(group),
//                                    { import_groups.toggleItemPresence(group) }
//                                )
//
//                                Text(title, Modifier.fillMaxWidth().weight(1f), style = MaterialTheme.typography.labelLarge)
//                            }
//                        }
//                    }
//                }
//            }
//        )
//    }
//}
//
//@Composable
//private fun ProjectButton(modifier: Modifier = Modifier) {
//    val player: PlayerState = LocalPlayerState.current
//    val clipboard: ClipboardManager = LocalClipboardManager.current
//
//    val project_url: String = stringResource(Res.string.project_url)
//    val project_url_name: String = stringResource(Res.string.project_url_name)
//    val notif_copied_x_to_clipboard: String = stringResource(Res.string.notif_copied_x_to_clipboard)
//
//    fun copyProjectUrl() {
//        clipboard.setText(AnnotatedString(project_url))
//        player.context.sendToast(notif_copied_x_to_clipboard.replace("\$x", project_url_name))
//    }
//
//    Icon(
//        painterResource(Res.drawable.ic_github),
//        null,
//        modifier.platformClickable(
//            onClick = {
//                if (player.context.canOpenUrl()) {
//                    player.context.openUrl(project_url)
//                }
//                else {
//                    copyProjectUrl()
//                }
//            },
//            onAltClick = {
//                if (player.context.canOpenUrl()) {
//                    copyProjectUrl()
//                    player.context.vibrateShort()
//                }
//            }
//        )
//    )
//}
//
//@Composable
//private fun InfoButton(modifier: Modifier = Modifier) {
//    var show_info_dialog: Boolean by remember { mutableStateOf(false) }
//
//    if (show_info_dialog) {
//        ProjectInfoDialog { show_info_dialog = false }
//    }
//
//    Icon(
//        Icons.Default.Info,
//        null,
//        modifier.platformClickable(
//            onClick = { show_info_dialog = !show_info_dialog }
//        )
//    )
//}
//
//@Composable
//private fun StyledCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
//    val player: PlayerState = LocalPlayerState.current
//    Checkbox(
//        checked,
//        onCheckedChange,
//        colors = CheckboxDefaults.colors(
//            checkedColor = player.theme.accent,
//            uncheckedColor = player.theme.accent,
//            checkmarkColor = player.theme.onAccent
//        )
//    )
//}
//
//@OptIn(DelicateCoroutinesApi::class, FormatStringsInDatetimeFormats::class)
//private suspend fun peformExport(context: AppContext, groups: List<SettingsGroup>) {
//    val datetime: String =
//        Clock.System.now()
//            .toLocalDateTime(TimeZone.currentSystemDefault())
//            .format(
//                LocalDateTime.Format {
//                    byUnicodePattern("yyyy-MM-dd_HH:mm:ss")
//                }
//            )
//    val filename: String = getString(Res.string.`settings_export_filename_$date`).replace("\$date", datetime)
//
//    context.promptUserForFileCreation("application/json", filename, persist = false) { path ->
//        if (path == null) {
//            return@promptUserForFileCreation
//        }
//
//        GlobalScope.launch(Dispatchers.PlatformIO) {
//            val settings_data: SettingsImportExport.SettingsExportData =
//                SettingsImportExport.exportSettingsData(
//                    prefs = context.getPrefs(),
//                    groups = groups
//                )
//
//            val file: PlatformFile = context.getUserDirectoryFile(path)!!
//            file.outputStream().buffer().use { writer ->
//                writer.writeUtf8(Json.encodeToString(settings_data))
//                writer.flush()
//            }
//        }
//    }
//}
//
//@Composable
//private fun ImportExportButtons(
//    exporting: Boolean,
//    export_groups: MutableList<SettingsGroup>,
//    group_pages: List<SettingsGroup.CategoryPage>,
//    beginExport: () -> Unit,
//    completeExport: () -> Unit,
//    setExporting: (Boolean) -> Unit,
//    setImporting: (Boolean) -> Unit
//) {
//    val initial_icon_modifier: Modifier = Modifier.alpha(0.5f)
//
//    AnimatedVisibility(
//        exporting,
//        enter = expandHorizontally(),
//        exit = shrinkHorizontally()
//    ) {
//        IconButton({
//            if (export_groups.size == group_pages.size) {
//                export_groups.clear()
//            }
//            else {
//                for (page in group_pages) {
//                    export_groups.addUnique(page.group)
//                }
//            }
//        }) {
//            Icon(Icons.Default.SelectAll, null)
//        }
//    }
//
//    Crossfade(exporting) { ex ->
//        if (!ex) {
//            IconButton({ beginExport() }) {
//                Icon(Icons.Default.Save, null, initial_icon_modifier)
//            }
//        }
//        else {
//            IconButton({ setExporting(false) }) {
//                Icon(Icons.Default.Close, null)
//            }
//        }
//    }
//
//    Crossfade(exporting) { ex ->
//        if (!ex) {
//            IconButton({ setImporting(true) }) {
//                Icon(Icons.Default.FolderOpen, null, initial_icon_modifier)
//            }
//        }
//        else {
//            IconButton({ completeExport() }) {
//                Icon(Icons.Default.Done, null)
//            }
//        }
//    }
//}
