package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.SearchPage
import com.toasterofbread.spmp.ui.layout.library.LibraryPage
import com.toasterofbread.spmp.ui.layout.songfeedpage.SongFeedPage
import com.toasterofbread.spmp.ui.theme.Theme

class MainPageState {
    val SongFeed = SongFeedPage(this)
    val Library = LibraryPage(this)
    val Search = SearchPage(this)
    val Default: MainPage = SongFeed

    var current_page by mutableStateOf(Default)

    fun setPage(page: MainPage? = null): Boolean {
        val new_page = page ?: Default
        if (new_page != current_page) {
            current_page = new_page
            new_page.onOpened()
            return true
        }
        return false
    }
}

abstract class MainPage(val state: MainPageState) {
    @Composable
    abstract fun Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    )

    @Composable
    open fun showTopBarContent() = false
    @Composable
    open fun TopBarContent(
        modifier: Modifier,
        close: () -> Unit
    ) {}

    open fun onOpened() {}

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FilterChipsRow(
        chip_count: Int,
        isChipSelected: (Int) -> Boolean,
        onChipSelected: (Int) -> Unit,
        modifier: Modifier = Modifier,
        spacing: Dp = 10.dp,
        chipContent: @Composable (Int) -> Unit
    ) {
        LazyRow(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(chip_count) { index ->
                Crossfade(isChipSelected(index)) { selected ->
                    ElevatedFilterChip(
                        selected,
                        {
                            onChipSelected(index)
                        },
                        { chipContent(index) },
                        colors = with(Theme) {
                            FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = background,
                                labelColor = on_background,
                                selectedContainerColor = accent,
                                selectedLabelColor = on_accent
                            )
                        },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Theme.on_background
                        )
                    )
                }
            }
        }
    }
}
