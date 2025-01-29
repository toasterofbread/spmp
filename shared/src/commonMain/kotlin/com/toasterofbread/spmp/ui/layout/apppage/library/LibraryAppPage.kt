package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.Dp
import dev.toastbits.composekit.components.platform.composable.BackHandler
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.components.utils.composable.*
import dev.toastbits.composekit.components.utils.modifier.scrollWithoutClip
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.apppage.library.pageselector.*
import dev.toastbits.composekit.util.composable.copy
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.library_local
import spmp.shared.generated.resources.library_account
import spmp.shared.generated.resources.error_yt_feed_parse_failed

abstract class LibrarySubPage(val context: AppContext) {
    abstract fun getIcon(): ImageVector

    open fun isHidden(): Boolean = false
    open fun enableSearching(): Boolean = true
    open fun enableSorting(): Boolean = true
    open fun getDefaultSortType(): MediaItemSortType = MediaItemSortType.PLAY_COUNT
    open fun nativeSortTypeLabel(): String? = null

    open fun canShowAltContent(): Boolean = false
    open fun getAltContentButtons(): Pair<Pair<StringResource, ImageVector>, Pair<StringResource, ImageVector>> =
        Pair(
            Pair(Res.string.library_local, Icons.Default.Inventory2),
            Pair(Res.string.library_account, Icons.Default.Cloud)
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
    open fun RowOrColumnScope.SideContent(showing_alt_content: Boolean) {}

    @Composable
    fun LibraryPageTitle(title: String, modifier: Modifier = Modifier) {
        if (FormFactor.observe().value != FormFactor.LANDSCAPE) {
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
    internal var current_tab: LibrarySubPage by mutableStateOf(tabs.first())

    internal var showing_search_field: Boolean by mutableStateOf(false)
    var search_filter: String? by mutableStateOf(null)

    private var show_sort_type_menu: Boolean by mutableStateOf(false)
    var sort_type: MediaItemSortType by mutableStateOf(current_tab.getDefaultSortType())
    var reverse_sort: Boolean by mutableStateOf(false)

    var external_load_error: Throwable? by mutableStateOf(null)

    internal var showing_alt_content: Boolean by mutableStateOf(false)

    override fun onOpened(from_item: MediaItemHolder?) {
        setCurrentTab(tabs.first { !it.isHidden() })
    }

    override fun onClosed(next_page: AppPage?, going_back: Boolean) {
        external_load_error = null
    }

    internal fun setCurrentTab(tab: LibrarySubPage) {
        showing_search_field = false
        search_filter = null
        show_sort_type_menu = false
        sort_type = tab.getDefaultSortType()
        reverse_sort = false
        showing_alt_content = false

        current_tab = tab
    }

    @Composable
    internal fun SearchButton(icon: ImageVector = Icons.Default.Search) {
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
    internal fun SortButton() {
        IconButton({
            show_sort_type_menu = !show_sort_type_menu
        }) {
            Icon(Icons.Default.Sort, null)
        }
    }

    @Composable
    override fun shouldShowPrimaryBarContent(): Boolean = true

    @Composable
    override fun PrimaryBarContent(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        val form_factor: FormFactor by FormFactor.observe()
        LibraryIconButtonPageSelector(
            slot,
            content_padding,
            lazy,
            modifier,
            show_page_buttons = true,
            show_contextual_buttons = form_factor == FormFactor.LANDSCAPE,
            show_source_buttons = form_factor == FormFactor.LANDSCAPE,
            separate_source_and_contextual = form_factor == FormFactor.LANDSCAPE
        )
        return true
    }

    @Composable
    override fun shouldShowSecondaryBarContent(): Boolean =
        FormFactor.observe().value != FormFactor.LANDSCAPE

    @Composable
    override fun SecondaryBarContent(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        LibraryIconButtonPageSelector(
            slot,
            content_padding,
            lazy,
            modifier,
            show_page_buttons = false,
            show_contextual_buttons = true,
            show_source_buttons = true
        )
        return true
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current

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
                        message = stringResource(Res.string.error_yt_feed_parse_failed),
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
