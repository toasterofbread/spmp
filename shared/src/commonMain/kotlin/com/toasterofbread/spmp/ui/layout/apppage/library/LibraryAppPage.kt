package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.common.copy
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.composekit.utils.composable.PlatformClickableIconButton
import com.toasterofbread.composekit.utils.composable.ResizableOutlinedTextField
import com.toasterofbread.composekit.utils.composable.SidebarButtonSelector
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.composekit.utils.composable.wave.WaveShape
import com.toasterofbread.composekit.utils.modifier.scrollWithoutClip
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.CoroutineScope

abstract class LibrarySubPage(val context: AppContext) {
    abstract fun getIcon(): ImageVector

    open fun isHidden(): Boolean = false
    open fun enableSearching(): Boolean = true
    open fun enableSorting(): Boolean = true
    open fun getDefaultSortType(): MediaItemSortType = MediaItemSortType.PLAY_COUNT
    open fun nativeSortTypeLabel(): String? = null

    open fun canShowAltContent(): Boolean = false
    open fun getAltContentButtons(): Pair<Pair<String, ImageVector>, Pair<String, ImageVector>> =
        Pair(
            Pair(getString("library_local"), Icons.Default.Inventory2),
            Pair(getString("library_account"), Icons.Default.Cloud)
        )

    @Composable
    abstract fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        showing_alt_content: Boolean,
        modifier: Modifier
    )

    @Composable
    open fun SideContent(showing_alt_content: Boolean) {}

    @Composable
    fun LibraryPageTitle(title: String, modifier: Modifier = Modifier) {
        if (LocalPlayerState.current.form_factor != FormFactor.LANDSCAPE) {
            return
        }

        Text(
            title,
            modifier.padding(bottom = 10.dp),
            style = MaterialTheme.typography.displaySmall
        )
    }
}

class LibraryAppPage(override val state: AppPageState): AppPage() {
    val tabs: List<LibrarySubPage> = listOf(
        LibraryPlaylistsPage(state.context),
        LibrarySongsPage(state.context),
        LibraryAlbumsPage(state.context),
        LibraryArtistsPage(state.context),
        LibraryProfilePage(state.context)
    )
    private var current_tab: LibrarySubPage by mutableStateOf(tabs.first())

    private var showing_search_field: Boolean by mutableStateOf(false)
    var search_filter: String? by mutableStateOf(null)

    private var show_sort_type_menu: Boolean by mutableStateOf(false)
    var sort_type: MediaItemSortType by mutableStateOf(current_tab.getDefaultSortType())
    var reverse_sort: Boolean by mutableStateOf(false)

    var external_load_error: Throwable? by mutableStateOf(null)

    private var showing_alt_content: Boolean by mutableStateOf(false)

    override fun onOpened(from_item: MediaItemHolder?) {
        setCurrentTab(tabs.first { !it.isHidden() })
    }

    override fun onClosed(next_page: AppPage?) {
        external_load_error = null
    }

    private fun setCurrentTab(tab: LibrarySubPage) {
        showing_search_field = false
        search_filter = null
        show_sort_type_menu = false
        sort_type = tab.getDefaultSortType()
        reverse_sort = false
        showing_alt_content = false

        current_tab = tab
    }

    @Composable
    override fun showTopBar(): Boolean = LocalPlayerState.current.form_factor == FormFactor.PORTRAIT

    @Composable
    override fun showTopBarContent(): Boolean = true

    @Composable
    private fun SearchButton(icon: ImageVector = Icons.Default.Search) {
        val player: PlayerState = LocalPlayerState.current
        val keyboard_controller: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current

        Crossfade(showing_search_field) { searching ->
            PlatformClickableIconButton(
                onClick = {
                    if (searching) {
                        keyboard_controller?.hide()
                    }
                    showing_search_field = !searching
                    search_filter = null
                },
                onAltClick = {
                    if (!searching) {
                        player.openAppPage(player.app_page_state.Search)
                        player.context.vibrateShort()
                    }
                }
            ) {
                Icon(
                    if (searching) Icons.Default.Close else icon,
                    null
                )
            }
        }
    }

    @Composable
    private fun SortButton() {
        IconButton({
            show_sort_type_menu = !show_sort_type_menu
        }) {
            Icon(Icons.Default.Sort, null)
        }
    }

    @Composable
    override fun TopBarContent(modifier: Modifier, close: () -> Unit) {
        val player: PlayerState = LocalPlayerState.current

        Column(modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                AnimatedVisibility(current_tab.enableSearching()) {
                    SearchButton()
                }

                Row(Modifier.fillMaxWidth().weight(1f)) {
                    AnimatedVisibility(
                        showing_search_field && current_tab.enableSearching(),
                        enter = fadeIn() + expandHorizontally(clip = false)
                    ) {
                        ResizableOutlinedTextField(
                            search_filter ?: "",
                            { search_filter = it },
                            Modifier.height(45.dp).fillMaxWidth().weight(1f),
                            singleLine = true
                        )
                    }

                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        val shown_tabs = tabs.filter { !it.isHidden() }

                        for (tab in shown_tabs.withIndex()) {
                            Crossfade(tab.value == current_tab) { selected ->
                                Box(
                                    Modifier
                                        .fillMaxWidth(
                                            1f / (shown_tabs.size - tab.index)
                                        )
                                        .padding(horizontal = 5.dp)
                                ) {
                                    ElevatedFilterChip(
                                        selected,
                                        {
                                            setCurrentTab(tab.value)
                                        },
                                        {
                                            Box(Modifier.fillMaxWidth().padding(end = 8.dp), contentAlignment = Alignment.Center) {
                                                Icon(tab.value.getIcon(), null, Modifier.requiredSizeIn(minWidth = 20.dp, minHeight = 20.dp))
                                            }
                                        },
                                        colors = with(player.theme) {
                                            FilterChipDefaults.elevatedFilterChipColors(
                                                containerColor = background,
                                                labelColor = on_background,
                                                selectedContainerColor = accent,
                                                selectedLabelColor = on_accent
                                            )
                                        },
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = player.theme.on_background,
                                            enabled = true,
                                            selected = selected
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(current_tab.enableSorting()) {
                    SortButton()
                }
            }

            AnimatedVisibility(!current_tab.canShowAltContent(), Modifier.align(Alignment.End)) {
                current_tab.SideContent(showing_alt_content)
            }

            AnimatedVisibility(current_tab.canShowAltContent()) {
                Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    @Composable
                    fun getButtonColours(current: Boolean) =
                        ButtonDefaults.buttonColors(
                            containerColor = if (current) player.theme.vibrant_accent else player.theme.vibrant_accent.copy(alpha = 0.1f),
                            contentColor = if (current) player.theme.vibrant_accent.getContrasted() else player.theme.on_background
                        )

                    val (main_button, alt_button) = remember(current_tab) { current_tab.getAltContentButtons() }

                    Button(
                        { showing_alt_content = false },
                        Modifier.fillMaxWidth(0.5f).weight(1f),
                        colors = getButtonColours(!showing_alt_content)
                    ) {
                        Text(main_button.first, textAlign = TextAlign.Center)
                    }

                    Button(
                        { showing_alt_content = true },
                        Modifier.fillMaxWidth().weight(1f),
                        colors = getButtonColours(showing_alt_content)
                    ) {
                        Text(alt_button.first, textAlign = TextAlign.Center)
                    }

                    current_tab.SideContent(showing_alt_content)
                }
            }
        }
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()

        val sidebar_background_colour: Color = player.theme.background.amplify(0.05f)
        val wave_border_offset: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }

        MediaItemSortType.SelectionMenu(
            show_sort_type_menu,
            sort_type,
            { show_sort_type_menu = false },
            {
                if (it == sort_type) {
                    reverse_sort = !reverse_sort
                }
                else {
                    sort_type = it
                }
            },
            current_tab.nativeSortTypeLabel()
        )

        BackHandler(showing_search_field && current_tab.enableSearching()) {
            showing_search_field = false
        }

        Row(modifier) {
            if (player.form_factor == FormFactor.LANDSCAPE) {
                BoxWithConstraints {
                    Column(
                        modifier = Modifier
                            .heightIn(min = this@BoxWithConstraints.maxHeight)
                            .zIndex(1f)
                            .drawWithContent {
                                drawContent()
                                leftBorderContent(player.theme.accent.copy(alpha = 0.25f), sidebar_background_colour) { wave_border_offset.value }
                            }
                            .background(sidebar_background_colour)
                            .padding(10.dp)
                            .padding(
                                start = 7.dp,
                                top = WindowInsets.getTop(),
                                bottom = player.nowPlayingBottomPadding(true)
                            )
                            .width(50.dp)
                            .fillMaxHeight()
                            .scrollWithoutClip(
                                rememberScrollState(),
                                is_vertical = true
                            )
                    ) {
                        SidebarButtonSelector(
                            selected_button = current_tab,
                            buttons = tabs,
                            indicator_colour = player.theme.vibrant_accent,
                            scrolling = false,
                            onButtonSelected = { tab ->
                                if (tab != current_tab) {
                                    val increasing: Boolean = tabs.indexOf(tab) > tabs.indexOf(current_tab)
                                    current_tab = tab
                                    coroutine_scope.launchSingle {
                                        wave_border_offset.animateTo(
                                            if (increasing) wave_border_offset.value + 20f
                                            else wave_border_offset.value - 20f,
                                            tween(500)
                                        )
                                    }
                                }
                            },
                            showButton = { tab ->
                                !tab.isHidden()
                            }
                        ) { tab ->
                            val colour: Color =
                                if (tab == current_tab) player.theme.on_accent
                                else player.theme.on_background

                            CompositionLocalProvider(LocalContentColor provides colour) {
                                Icon(tab.getIcon(), null, Modifier.requiredSizeIn(minWidth = 20.dp, minHeight = 20.dp))
                            }
                        }

                        Spacer(Modifier.fillMaxHeight().weight(1f))

                        val (main_button, alt_button) = remember(current_tab) { current_tab.getAltContentButtons() }

                        SidebarButtonSelector(
                            selected_button = showing_alt_content,
                            buttons = listOf(false, true),
                            indicator_colour = player.theme.vibrant_accent,
                            scrolling = false,
                            onButtonSelected = {
                                showing_alt_content = it
                            },
                            showButton = {
                                current_tab.canShowAltContent()
                            },
                            extraContent = {
                                if (!it) {
                                    current_tab.SideContent(showing_alt_content)

                                    AnimatedVisibility(current_tab.enableSearching()) {
                                        SearchButton(Icons.Default.FilterAlt)
                                    }

                                    BoxWithConstraints(
                                        Modifier
                                            .fillMaxWidth()
                                            .weight(1f, false)
                                            .requiredHeight(0.dp)
                                            .zIndex(-10f)
                                    ) {
                                        val field_size: DpSize = DpSize(250.dp, 65.dp)
                                        val focus_requester: FocusRequester = remember { FocusRequester() }

                                        this@SidebarButtonSelector.AnimatedVisibility(
                                            showing_search_field && current_tab.enableSearching(),
                                            Modifier
                                                .requiredSize(field_size)
                                                .offset(x = (field_size.width + maxWidth) / 2, y = (-45f / 2f).dp),
                                            enter = slideInHorizontally() { -it * 2 },
                                            exit = slideOutHorizontally() { -it * 2 }
                                        ) {
                                            LaunchedEffect(Unit) {
                                                focus_requester.requestFocus()
                                            }

                                            Row(
                                                Modifier
                                                    .background(player.theme.background.amplify(0.025f), MaterialTheme.shapes.small)
                                                    .requiredSize(field_size)
                                                    .padding(10.dp)
                                                    .padding(end = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                AnimatedVisibility(current_tab.enableSorting()) {
                                                    SortButton()
                                                }

                                                ResizableOutlinedTextField(
                                                    search_filter ?: "",
                                                    { search_filter = it },
                                                    Modifier.fillMaxWidth().weight(1f).focusRequester(focus_requester),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(current_tab.canShowAltContent()) {
                                        Spacer(Modifier.height(20.dp))
                                    }
                                }
                            }
                        ) {
                            val colour: Color =
                                if (it == showing_alt_content) player.theme.on_accent
                                else player.theme.on_background

                            CompositionLocalProvider(LocalContentColor provides colour) {
                                Icon(
                                    if (it) alt_button.second
                                    else main_button.second,
                                    null,
                                    Modifier.requiredSizeIn(minWidth = 20.dp, minHeight = 20.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                external_load_error != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                var error: Throwable? by remember { mutableStateOf(external_load_error) }
                LaunchedEffect(external_load_error) {
                    if (external_load_error != null) {
                        error = external_load_error
                    }
                }

                error?.also {
                    ErrorInfoDisplay(
                        it,
                        isDebugBuild(),
                        modifier = Modifier.padding(content_padding.copy(bottom = 20.dp)),
                        message = getString("error_yt_feed_parse_failed"),
                        onRetry = {
                            player.app_page_state.SongFeed.retrying = true
                            player.openAppPage(player.app_page_state.SongFeed)
                        },
                        onDismiss = {
                            external_load_error = null
                        },
                        disable_parent_scroll = false
                    )
                }
            }

            Crossfade(Pair(current_tab, showing_alt_content)) {
                val (tab, showing_account) = it
                tab.
                Page(
                    this@LibraryAppPage,
                    content_padding.copy(top = if (external_load_error != null) 0.dp else null),
                    multiselect_context,
                    showing_account,
                    Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun ContentDrawScope.leftBorderContent(colour: Color, background_colour: Color, getOffset: () -> Float = { 0f }) {
    val shape: Shape =
        WaveShape(
            (size.height / 10.dp.toPx()).toInt(),
            getOffset() + (size.height / 2),
            width_multiplier = 2f
        )
    val outline: Outline =
        shape.createOutline(Size(size.height, 5.dp.toPx()), LayoutDirection.Ltr, this)

    scale(2f, Offset.Zero) {
        rotate(90f, Offset.Zero) {
            translate(top = (-4).dp.toPx()) {
                drawOutline(outline, colour)
                translate(top = (-1).dp.toPx()) {
                    drawOutline(outline, background_colour)
                }
            }
        }
    }
}
