package com.toasterofbread.composesettings.ui.item

class SettingsLargeToggleItem(
    val state: BasicSettingsValueState<Boolean>,
    val enabled_text: String? = null,
    val disabled_text: String? = null,
    val enable_button: String,
    val disable_button: String,
    val enabled_content: (@Composable (Modifier) -> Unit)? = null,
    val disabled_content: (@Composable (Modifier) -> Unit)? = null,
    val warningDialog: (@Composable (dismiss: () -> Unit, openPage: (Int) -> Unit) -> Unit)? = null,
    val infoDialog: (@Composable (dismiss: () -> Unit, openPage: (Int) -> Unit) -> Unit)? = null,
    val onClicked: (target: Boolean, setEnabled: (Boolean) -> Unit, setLoading: (Boolean) -> Unit, openPage: (Int) -> Unit) -> Unit =
        { target, setEnabled, _, _ -> setEnabled(target) }
): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        val shape = RoundedCornerShape(25.dp)
        var loading: Boolean by remember { mutableStateOf(false) }

        var showing_dialog: (@Composable (dismiss: () -> Unit, openPage: (Int) -> Unit) -> Unit)? by remember { mutableStateOf(null) }

        showing_dialog?.invoke(
            { showing_dialog = null },
            openPage
        )

        Crossfade(state.value) { enabled ->
            CompositionLocalProvider(LocalContentColor provides if (!enabled) theme.on_background else theme.on_accent) {
                Row(
                    Modifier
                        .background(
                            if (!enabled) theme.background else theme.vibrant_accent,
                            shape
                        )
                        .border(2.dp, theme.vibrant_accent, shape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    (if (enabled) enabled_content else disabled_content)?.invoke(Modifier.weight(1f).padding(vertical = 5.dp))
                    (if (enabled) enabled_text else disabled_text)?.also { WidthShrinkText(it, Modifier.fillMaxWidth().weight(1f)) }

                    Button(
                        {
                            if (!enabled && warningDialog != null) {
                                showing_dialog = warningDialog
                            }
                            else {
                                onClicked(
                                    !enabled,
                                    { state.value = it },
                                    { loading = it },
                                    openPage
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) theme.background else theme.vibrant_accent,
                            contentColor = if (enabled) theme.on_background else theme.on_accent
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            this@Row.AnimatedVisibility(loading, enter = fadeIn(), exit = fadeOut()) {
                                SubtleLoadingIndicator()
                            }

                            val text_alpha = animateFloatAsState(if (loading) 0f else 1f)
                            Text(
                                if (enabled) disable_button else enable_button,
                                Modifier.graphicsLayer { alpha = text_alpha.value }
                            )
                        }
                    }

                    if (infoDialog != null) {
                        ShapedIconButton(
                            { showing_dialog = if (showing_dialog == null) infoDialog else null },
                            shape = CircleShape,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (enabled) theme.background else theme.vibrant_accent,
                                contentColor = if (enabled) theme.on_background else theme.on_accent
                            )
                        ) {
                            Icon(Icons.Default.Info, null)
                        }
                    }
                }
            }
        }
    }
}
