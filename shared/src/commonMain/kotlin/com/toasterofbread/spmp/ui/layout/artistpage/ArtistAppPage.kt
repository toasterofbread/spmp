package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.util.platform.launchSingle
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.ui.layout.artistpage.lff.LFFArtistPage
import com.toasterofbread.spmp.platform.FormFactor
import dev.toastbits.ytmkt.endpoint.ArtistWithParamsEndpoint
import dev.toastbits.ytmkt.model.external.YoutubePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class ArtistAppPage(
    override val state: AppPageState,
    override val item: Artist,
    internal val browse_params: Pair<YoutubePage.BrowseParamsData, ArtistWithParamsEndpoint>? = null
): AppPageWithItem() {
    internal var load_error: Throwable? by mutableStateOf(null)
    internal val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    internal var previous_item: MediaItemHolder? by mutableStateOf(null)
        private set
    private var item_load_state: State<Boolean>? by mutableStateOf(null)

    internal val loading: Boolean get() = item_load_state?.value == true
    internal var refreshed: Boolean by mutableStateOf(false)

    override fun onOpened(from_item: MediaItemHolder?) {
        super.onOpened(from_item)
        previous_item = from_item
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        val form_factor: FormFactor by FormFactor.observe()
        item_load_state = item.loadDataOnChange(state.context)

        DisposableEffect(Unit) {
            load_error = null
            onDispose {
                coroutine_scope.cancel()
            }
        }

        if (form_factor.is_large) {
            LFFArtistPage(item, modifier, content_padding, multiselect_context)
        }
        else {
            SFFArtistPage(item, modifier, content_padding, multiselect_context)
        }
    }

    override fun canReload(): Boolean = true
    override fun onReload() {
        load_error = null
        refreshed = true
        coroutine_scope.launchSingle {
            MediaItemLoader.loadArtist(item.getEmptyData(), state.context).onFailure {
                load_error = it
            }
        }
    }

    @Composable
    override fun isReloading(): Boolean = item_load_state?.value == true
}
