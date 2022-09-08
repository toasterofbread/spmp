package com.spectre7.spmp.ui.components

@Stable
interface MultiSelectorState {
    val selectedIndex: Float
    val startCornerPercent: Int
    val endCornerPercent: Int

    fun selectOption(scope: CoroutineScope, index: Int)
}

@Stable
class MultiSelectorStateImpl(
    val option_count: Int,
    selected_option: Int,
) : MultiSelectorState {

    override val selectedIndex: Float
        get() = _selectedIndex.value
    override val startCornerPercent: Int
        get() = _startCornerPercent.value.toInt()
    override val endCornerPercent: Int
        get() = _endCornerPercent.value.toInt()

    private var _selectedIndex = Animatable(selected_option.toFloat())
    private var _startCornerPercent = Animatable(
        if (selected_option == 0) {
            50f
        } else {
            15f
        }
    )
    private var _endCornerPercent = Animatable(
        if (selected_option == option_count - 1) {
            50f
        } else {
            15f
        }
    )

    private val animationSpec = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )

    override fun selectOption(scope: CoroutineScope, index: Int) {
        scope.launch {
            _selectedIndex.animateTo(
                targetValue = index.toFloat(),
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _startCornerPercent.animateTo(
                targetValue = if (index == 0) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _endCornerPercent.animateTo(
                targetValue = if (index == option_count - 1) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
    }
}

// 9
@Composable
fun rememberMultiSelectorState(
    option_count: Int,
    selected_option: Int,
) = remember {
    MultiSelectorStateImpl(
        option_count,
        selected_option,
    )
}

enum class MultiSelectorOption {
    Option,
    Background,
}

@Composable
fun MultiSelector(
    option_count: Int,
    selected_option: Int,
    modifier: Modifier = Modifier,
    selector_modifier: Modifier = Modifier,
    rounding: Int = 50,
    colour: Color = Color.Unspecified,
    background_colour: Color = Color.Unspecified,
    state: MultiSelectorState = rememberMultiSelectorState(
        option_count = option_count,
        selected_option = selected_option,
    ),
    on_selected: (Int) -> Unit,
    get_option: @Composable (Int) -> Unit
) {
    require(option_count >= 2) { "This composable requires at least 2 options" }

    LaunchedEffect(key1 = option_count, key2 = selected_option) {
        state.selectOption(this, selected_option)
    }

    Layout(
        modifier = modifier
            .clip(shape = RoundedCornerShape(percent = rounding))
            .background(if (background_colour == Color.Unspecified) MaterialTheme.colorScheme.background else background_colour)
            .border(Dp.Hairline, Color.Black, RoundedCornerShape(percent = rounding)),
        content = {

            for (i in 0 until option_count) {
                Box(
                    modifier = selector_modifier
                        .layoutId(MultiSelectorOption.Option)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { on_selected(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    get_option(i)
                }
                Box(
                    modifier = selector_modifier
                        .layoutId(MultiSelectorOption.Background)
                        .clip(
                            shape = RoundedCornerShape(
                                rounding
//                                topStartPercent = state.startCornerPercent,
//                                bottomStartPercent = state.startCornerPercent,
//                                topEndPercent = state.endCornerPercent,
//                                bottomEndPercent = state.endCornerPercent,
                            )
                        )
                        .background(if (colour == Color.Unspecified) MaterialTheme.colorScheme.primary else colour)
                )
            }
        }
    ) { measurables, constraints ->
        val optionWidth = constraints.maxWidth / option_count
        val optionConstraints = Constraints.fixed(
            width = optionWidth,
            height = constraints.maxHeight,
        )
        val optionPlaceables = measurables
            .filter { measurable -> measurable.layoutId == MultiSelectorOption.Option }
            .map { measurable -> measurable.measure(optionConstraints) }
        val backgroundPlaceable = measurables
            .first { measurable -> measurable.layoutId == MultiSelectorOption.Background }
            .measure(optionConstraints)
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            // 4
            backgroundPlaceable.placeRelative(
                x = (state.selectedIndex * optionWidth).toInt(),
                y = 0,
            )
            optionPlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = optionWidth * index,
                    y = 0,
                )
            }
        }
    }
}