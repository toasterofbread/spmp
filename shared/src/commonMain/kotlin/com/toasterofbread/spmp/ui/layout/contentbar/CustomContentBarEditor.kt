package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.composekit.utils.composable.StickyHeightColumn
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import kotlin.math.roundToInt
import com.toasterofbread.composekit.utils.modifier.disableGestures

private const val BAR_SIZE_DP_STEP: Float = 10f

@Composable
private fun BarConfig(bar: CustomContentBar, onEdit: (CustomContentBar) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Bar name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Text(getString("content_bar_editor_bar_name"))

            TextField(
                bar.bar_name,
                { onEdit(bar.copy(bar_name = it)) },
                Modifier.fillMaxWidth().weight(1f).appTextField(),
                singleLine = true
            )
        }

        // Bar size
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(getString("content_bar_editor_bar_size"))
            Spacer(Modifier.fillMaxWidth().weight(1f))

            IconButton({
                onEdit(bar.copy(size_dp = (bar.size_dp - BAR_SIZE_DP_STEP).coerceAtLeast(BAR_SIZE_DP_STEP)))
            }) {
                Icon(Icons.Default.Remove, null)
            }

            Text(bar.size_dp.roundToInt().toString() + "dp")

            IconButton({
                onEdit(bar.copy(size_dp = bar.size_dp + BAR_SIZE_DP_STEP))
            }) {
                Icon(Icons.Default.Add, null)
            }

            IconButton(
                {
                    onEdit(bar.copy(size_dp = CUSTOM_CONTENT_BAR_DEFAULT_SIZE_DP))
                },
                Modifier.padding(start = 10.dp)
                ) {
                Icon(Icons.Default.Refresh, null)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomContentBarEditor(editing_bar: CustomContentBar, commit: (CustomContentBar) -> Unit) {
    val player: PlayerState = LocalPlayerState.current

    val vertical_bar: Boolean = false
    val delete_button_offset: Dp = 42.dp

    var bar: CustomContentBar by remember { mutableStateOf(editing_bar) }
    var selected_element: Int? by remember { mutableStateOf(null) }
    var show_template_selector: Boolean by remember { mutableStateOf(false) }

    OnChangedEffect(editing_bar) {
        bar = editing_bar
    }

    fun editElementData(action: MutableList<ContentBarElementData>.() -> Unit) {
        val data: MutableList<ContentBarElementData> = bar.element_data.toMutableList()
        action(data)
        bar = bar.copy(element_data = data)
        commit(bar)
    }

    fun deleteElement(index: Int) {
        if (selected_element == index) {
            selected_element = null
        }
        else {
            selected_element?.also { element ->
                if (element > index) {
                    selected_element = element - 1
                }
            }
        }

        editElementData {
            removeAt(index)
        }
    }

    fun onElementClicked(index: Int) {
        if (selected_element == index) {
            selected_element = null
        }
        else {
            selected_element = index
        }
    }

    if (show_template_selector) {
        CustomContentBarTemplate.SelectionDialog { template ->
            show_template_selector = false
            if (template == null) {
                return@SelectionDialog
            }

            editElementData {
                clear()
                addAll(template.getElements().map { it.getData() })
            }
        }
    }

    StickyHeightColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BarConfig(bar) {
            bar = it
            commit(bar)
        }

        Column(
            Modifier
                .background(player.theme.vibrant_accent, RoundedCornerShape(16.dp))
                .padding(15.dp)
        ) {
        CompositionLocalProvider(LocalContentColor provides player.theme.vibrant_accent.getContrasted()) {
            val button_colours: ButtonColors = ButtonDefaults.buttonColors(
                containerColor = player.theme.background,
                contentColor = player.theme.on_background
            )

            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElementSelector(button_colours, Modifier.fillMaxWidth().weight(1f)) { element_type ->
                    bar = bar.copy(
                        element_data = listOf(ContentBarElementData(element_type)) + bar.element_data
                    )
                    selected_element = 0

                    commit(bar)
                }

                Button(
                    { show_template_selector = !show_template_selector },
                    colors = button_colours
                ) {
                    Icon(Icons.Default.ViewQuilt, null, Modifier.padding(end = 10.dp))
                    Text(getString("content_bar_editor_pick_template"))
                }
            }

            Box(Modifier.padding(bottom = 30.dp)) {
                bar.CustomBarContent(
                    vertical = vertical_bar,
                    background_colour = Theme.Colour.BACKGROUND,
                    content_padding = PaddingValues(),
                    apply_size = false,
                    selected_element_override = selected_element?.takeIf { bar.elements[it] !is ContentBarElementSpacer } ?: -1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(player.theme.background, RoundedCornerShape(16.dp))
                        .height(60.dp)
                        .padding(5.dp),
                    getSpacerElementModifier = { index, spacer -> with(spacer) {
                        Modifier
                            .clickable { onElementClicked(index) }
                            .border(2.dp, player.theme.vibrant_accent)
                            .thenIf(index == selected_element) {
                                background(player.theme.vibrant_accent)
                            }
                    }},
                    shouldShowButton = { true },
                    buttonContent = { index, element, width ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .platformClickable(onClick = { onElementClicked(index) }),
                            contentAlignment = Alignment.Center
                        ) {
                            element.Element(vertical_bar, width, Modifier.disableGestures())
                        }

                        IconButton(
                            { deleteElement(index) },
                            Modifier.offset(y = delete_button_offset).wrapContentSize(unbounded = true)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = player.theme.background)
                        }
                    }
                )

                if (bar.elements.isEmpty()) {
                    Text(
                        getString("content_bar_editor_no_elements_added"),
                        Modifier.align(Alignment.Center),
                        color = player.theme.on_background
                    )
                }
            }

            var element_index: Int by remember { mutableStateOf(selected_element ?: -1) }
            LaunchedEffect(selected_element) {
                selected_element?.also {
                    element_index = it
                }
            }

            NullableValueAnimatedVisibility(
                selected_element?.let { bar.elements.getOrNull(it) },
                enter = expandVertically(),
                exit = shrinkVertically()
            ) { element ->
                if (element == null) {
                    return@NullableValueAnimatedVisibility
                }

                Box(
                    Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .background(player.theme.background, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    CompositionLocalProvider(LocalContentColor provides player.theme.on_background) {
                        ElementEditor(
                            element,
                            element_index,
                            move = { to ->
                                if (to < 0 || to > bar.elements.size - 1) {
                                    return@ElementEditor
                                }

                                editElementData {
                                    add(to, removeAt(element_index))
                                    selected_element = to
                                }
                            }
                        ) {
                            editElementData {
                                this[element_index] = element.getData()
                            }
                        }
                    }
                }
            }
        }}
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElementSelector(
    button_colours: ButtonColors,
    modifier: Modifier = Modifier,
    onSelected: (ContentBarElement.Type) -> Unit
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, null)
            Text(getString("content_bar_editor_add_element"))
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (type in ContentBarElement.Type.entries.filter { it.isAvailable() }) {
                Button(
                    { onSelected(type) },
                    colors = button_colours
                ) {
                    Icon(type.getIcon(), null)
                    Text(type.getName(), softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun ElementEditor(
    element: ContentBarElement,
    index: Int,
    move: (Int) -> Unit,
    modifier: Modifier = Modifier,
    commit: () -> Unit
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                getString("content_bar_editor_configure_element_\$x").replace("\$x", (index + 1).toString()),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.fillMaxWidth().weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(getString("content_bar_editor_move_element"), Modifier.padding(end = 5.dp))

                IconButton({ move(index - 1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }

                IconButton({ move(index + 1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }
        }

        element.ConfigurationItems(onModification = commit)
    }
}
