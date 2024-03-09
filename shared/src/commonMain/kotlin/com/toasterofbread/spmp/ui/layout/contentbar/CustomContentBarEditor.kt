package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import kotlin.math.roundToInt

private const val BAR_SIZE_DP_STEP: Float = 10f

@Composable
private fun BarConfig(bar: CustomContentBar, onEdit: (CustomContentBar) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Bar name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(getString("content_bar_editor_bar_name"))

            TextField(
                bar.bar_name,
                { onEdit(bar.copy(bar_name = it)) },
                Modifier.fillMaxWidth().weight(1f),
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

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BarConfig(bar) {
            bar = it
            commit(bar)
        }

        var action_row_height: Int by remember { mutableStateOf(0) }

        FlowRow(
            Modifier
                .fillMaxWidth()
                .onSizeChanged {
                    action_row_height = it.height
                },
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElementSelector(Modifier.fillMaxWidth().weight(1f)) { element_type ->
                bar = bar.copy(
                    element_data = listOf(ContentBarElementData(element_type)) + bar.element_data
                )
                selected_element = 0

                commit(bar)
            }

            var move_row_height: Int by remember { mutableStateOf(0) }
            var move_row_position: Float by remember { mutableStateOf(0f) }

            NullableValueAnimatedVisibility(
                selected_element,
                Modifier
                    .fillMaxHeight()
                    .onSizeChanged {
                        move_row_height = it.height
                    }
                    .onGloballyPositioned {
                        move_row_position = it.positionInParent().y
                    }
                    .offset {
                        IntOffset(
                            0,
                            if (move_row_position == 0f) action_row_height - move_row_height
                            else 0
                        )
                    },
            ) { element_index ->
                if (element_index == null) {
                    return@NullableValueAnimatedVisibility
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getString("content_bar_editor_move_element"), Modifier.padding(end = 5.dp))

                    IconButton({
                        if (element_index != 0) {
                            selected_element = element_index - 1
                            editElementData {
                                add(element_index - 1, removeAt(element_index))
                            }
                        }
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                    }

                    IconButton({
                        if (element_index != bar.element_data.size - 1) {
                            selected_element = element_index + 1
                            editElementData {
                                add(element_index + 1, removeAt(element_index))
                            }
                        }
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }
            }
        }

        bar.CustomBarContent(
            vertical = vertical_bar,
            background_colour = Theme.Colour.BACKGROUND,
            content_padding = PaddingValues(),
            selected_element_override = selected_element?.takeIf { bar.elements[it] !is ContentBarElementSpacer } ?: -1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp)
                .heightIn(min = 60.dp)
                .border(2.dp, player.theme.vibrant_accent, RoundedCornerShape(16.dp))
                .padding(5.dp),
            getSpacerElementModifier = { index, spacer -> with(spacer) {
                return@with getSpacerModifier(vertical_bar)
                    .clickable { onElementClicked(index) }
                    .border(2.dp, player.theme.vibrant_accent)
                    .thenIf(index == selected_element) {
                        background(player.theme.vibrant_accent)
                    }
            }},
            shouldShowButton = { true },
            buttonContent = { index, element ->
                Box(
                    Modifier.platformClickable(onClick = { onElementClicked(index) })
                ) {
                    element.Element(vertical_bar, Modifier.disableGestures())
                }

                IconButton(
                    { deleteElement(index) },
                    Modifier.offset(y = delete_button_offset).wrapContentSize(unbounded = true)
                ) {
                    Icon(Icons.Default.Delete, null, tint = player.theme.on_background)
                }
            }
        )

        NullableValueAnimatedVisibility(selected_element) { element_index ->
            val element: ContentBarElement =
                element_index?.let { bar.elements.getOrNull(it) } ?: return@NullableValueAnimatedVisibility

            ElementEditor(element, element_index) {
                editElementData {
                    this[element_index] = element.getData()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElementSelector(
    modifier: Modifier = Modifier,
    onSelected: (ContentBarElement.Type) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current

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
                Button({ onSelected(type) }) {
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
    modifier: Modifier = Modifier,
    commit: () -> Unit
) {
    Column(modifier) {
        Text(
            getString("content_bar_editor_configure_element_\$x").replace("\$x", (index + 1).toString()),
            style = MaterialTheme.typography.headlineSmall
        )

        element.Configuration(modifier) {
            commit()
        }
    }
}

fun Modifier.disableGestures(disabled: Boolean = true): Modifier =
    if (disabled)
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(pass = PointerEventPass.Initial)
                        .changes
                        .forEach { it.consume() }
                }
            }
        }
    else this
