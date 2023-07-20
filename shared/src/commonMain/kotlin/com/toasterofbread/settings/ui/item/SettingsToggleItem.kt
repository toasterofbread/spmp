package com.toasterofbread.composesettings.ui.item

class SettingsToggleItem(
    val state: BasicSettingsValueState<Boolean>,
    val title: String?,
    val subtitle: String?,
    val checker: ((target: Boolean, setLoading: (Boolean) -> Unit, (allow_change: Boolean) -> Unit) -> Unit)? = null
): SettingsItem() {

    private var loading: Boolean by mutableStateOf(false)

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ItemTitleText(title, theme)
                ItemText(subtitle, theme)
            }

            Crossfade(loading) {
                if (it) {
                    CircularProgressIndicator(color = theme.on_background)
                }
                else {
                    Switch(
                        state.value,
                        onCheckedChange = null,
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (checker == null) {
                                state.value = !state.value
                                return@clickable
                            }

                            checker.invoke(
                                !state.value,
                                { l ->
                                    loading = l
                                }
                            ) { allow_change ->
                                if (allow_change) {
                                    state.value = !state.value
                                }
                                loading = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.vibrant_accent,
                            checkedTrackColor = theme.vibrant_accent.setAlpha(0.5f)
                        )
                    )
                }
            }
        }
    }
}
