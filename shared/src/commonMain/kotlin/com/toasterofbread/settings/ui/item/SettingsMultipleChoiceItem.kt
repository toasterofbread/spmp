package com.toasterofbread.composesettings.ui.item

class SettingsMultipleChoiceItem(
    val state: BasicSettingsValueState<Int>,
    val title: String?,
    val subtitle: String?,
    val choice_amount: Int,
    val radio_style: Boolean,
    val get_choice: (Int) -> String,
): SettingsItem() {

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
        Column {
            Column(Modifier.fillMaxWidth()) {
                ItemTitleText(title, theme, Modifier.padding(bottom = 7.dp))
                ItemText(subtitle, theme)

                Spacer(Modifier.height(10.dp))

                if (radio_style) {
                    Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until choice_amount) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .border(
                                        Dp.Hairline,
                                        theme.on_background,
                                        SETTINGS_ITEM_ROUNDED_SHAPE
                                    )
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        null
                                    ) { state.value = i }
                            ) {
                                WidthShrinkText(get_choice(i))
                                RadioButton(i == state.value, onClick = { state.value = i }, colors = RadioButtonDefaults.colors(theme.vibrant_accent))
                            }
                        }
                    }
                }
                else {
                    Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until choice_amount) {

                            val colour = remember(i) { Animatable(if (state.value == i) theme.vibrant_accent else Color.Transparent) }
                            LaunchedEffect(state.value, theme.vibrant_accent) {
                                colour.animateTo(if (state.value == i) theme.vibrant_accent else Color.Transparent, TweenSpec(150))
                            }

                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .border(
                                        Dp.Hairline,
                                        theme.on_background,
                                        SETTINGS_ITEM_ROUNDED_SHAPE
                                    )
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable(remember { MutableInteractionSource() }, null) {
                                        state.value = i
                                    }
                                    .background(colour.value, SETTINGS_ITEM_ROUNDED_SHAPE)
                            ) {
                                Box(Modifier.padding(horizontal = 10.dp)) {
                                    WidthShrinkText(
                                        get_choice(i),
                                        style = LocalTextStyle.current.copy(color = if (state.value == i) theme.on_accent else theme.on_background)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
