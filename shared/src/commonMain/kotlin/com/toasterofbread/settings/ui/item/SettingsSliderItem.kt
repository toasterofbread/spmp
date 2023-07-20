package com.toasterofbread.composesettings.ui.item

class SettingsSliderItem(
    val state: BasicSettingsValueState<out Number>,
    val title: String?,
    val subtitle: String?,
    val min_label: String? = null,
    val max_label: String? = null,
    val steps: Int = 0,
    val range: ClosedFloatingPointRange<Float> = 0f .. 1f,
    val getValueText: ((value: Float) -> String?)? = { it.roundTo(2).toString() }
): SettingsItem() {

    private var is_int: Boolean = false
    private var value_state by mutableStateOf(0f)

    @Suppress("UNCHECKED_CAST")
    fun setValue(value: Float) {
        value_state = value
        if (is_int) {
            (state as BasicSettingsValueState<Int>).value = value.roundToInt()
        } else {
            (state as BasicSettingsValueState<Float>).value = value
        }
    }

    fun getValue(): Float {
        return value_state
    }

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
        value_state = state.value.toFloat()
        is_int = when (state.getDefault(default_provider)) {
            is Float -> false
            is Int -> true
            else -> throw NotImplementedError(state.getDefault(default_provider).javaClass.name)
        }
    }

    override fun resetValues() {
        state.reset()
        value_state = state.value.toFloat()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        var show_edit_dialog by remember { mutableStateOf(false) }

        if (show_edit_dialog) {
            var text by remember { mutableStateOf((if (is_int) getValue().roundToInt() else getValue()).toString()) }
            var error by remember { mutableStateOf<String?>(null) }

            PlatformAlertDialog(
                {
                    show_edit_dialog = false
                },
                confirmButton = {
                    FilledTonalButton(
                        {
                            try {
                                setValue(if (is_int) text.toInt().toFloat() else text.toFloat())
                                show_edit_dialog = false
                            }
                            catch(_: NumberFormatException) {}
                        },
                        enabled = error == null
                    ) {
                        Text("Done")
                    }
                },
                dismissButton = { TextButton( { show_edit_dialog = false } ) { Text("Cancel") } },
                title = { ItemTitleText(title ?: "Edit field", theme) },
                text = {
                    OutlinedTextField(
                        value = text,
                        isError = error != null,
                        label = {
                            Crossfade(error) { error_text ->
                                if (error_text != null) {
                                    Text(error_text)
                                }
                            }
                        },
                        onValueChange = {
                            text = it

                            try {
                                val value: Float = if (is_int) text.toInt().toFloat() else text.toFloat()
                                if (!range.contains(value)) {
                                    error = getString("settings_value_out_of_$range").replace("\$range", range.toString())
                                    return@OutlinedTextField
                                }

                                error = null
                            }
                            catch(_: NumberFormatException) {
                                error = getString(if (is_int) "settings_value_not_int" else "settings_value_not_float")
                            }
                        },
                        singleLine = true
                    )
                }
            )
        }

        Column(Modifier.fillMaxWidth()) {
            if (title != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ItemTitleText(title, theme)
                    IconButton({ show_edit_dialog = true }, Modifier.size(25.dp)) {
                        Icon(Icons.Filled.Edit, null)
                    }
                }
            }

            ItemText(subtitle, theme)

            Spacer(Modifier.requiredHeight(10.dp))

            if (state is SettingsValueState) {
                state.autosave = false
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (min_label != null) {
                    ItemText(min_label, theme)
                }
                SliderValueHorizontal(
                    value = getValue(),
                    onValueChange = { setValue(it) },
                    onValueChangeFinished = {
                        state.save()
                    },
                    thumbSizeInDp = DpSize(12.dp, 12.dp),
                    track = { a, b, c, d, e ->
                        DefaultTrack(a, b, c, d, e,
                            theme.vibrant_accent.setAlpha(0.5f),
                            theme.vibrant_accent,
                            colorTickProgress = theme.vibrant_accent.getContrasted().setAlpha(0.5f)
                        )
                    },
                    thumb = { modifier, offset, interaction_source, enabled, thumb_size ->
                        val colour = theme.vibrant_accent
                        val scale_on_press = 1.15f
                        val animation_spec = SpringSpec<Float>(0.65f)
                        val value_text = getValueText?.invoke(getValue())

                        if (value_text != null) {
                            MeasureUnconstrainedView({ ItemText(value_text, theme) }) { width, height ->

                                var is_pressed by remember { mutableStateOf(false) }
                                interaction_source.ListenOnPressed { is_pressed = it }
                                val scale: Float by animateFloatAsState(
                                    if (is_pressed) scale_on_press else 1f,
                                    animationSpec = animation_spec
                                )

                                Column(
                                    Modifier
                                        .offset(with(LocalDensity.current) { offset - (width.toDp() / 2) + 12.dp })
                                        .requiredHeight(55.dp)
                                        .graphicsLayer(scale, scale),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(
                                        Modifier
                                            .size(12.dp)
                                            .background(
                                                if (enabled) colour else
                                                    colour.setAlpha(0.6f), CircleShape
                                            )
                                    )
                                    ItemText(value_text, theme)
                                }
                            }
                        }
                        else {
                            DefaultThumb(
                                modifier,
                                offset,
                                interaction_source,
                                true,
                                thumb_size,
                                colour,
                                scale_on_press,
                                animation_spec
                            )
                        }
                    },
                    steps = steps,
                    modifier = Modifier.weight(1f),
                    valueRange = range
                )
                if (max_label != null) {
                    ItemText(max_label, theme)
                }
            }
        }
    }
}
