package com.toasterofbread.spmp.ui.layout.apppage.controlpanelpage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyColumn
import com.toasterofbread.composekit.utils.common.copy
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.rememberSongDownloads
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

@Composable
fun ControlPanelDownloadsPage(modifier: Modifier, multiselect_context: MediaItemMultiSelectContext? = null, content_padding: PaddingValues = PaddingValues()) {
    val downloads: List<DownloadStatus> by rememberSongDownloads()
    
    Row(
        modifier.padding(content_padding.copy(bottom = 0.dp)),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val bottom_padding: Dp = content_padding.calculateBottomPadding()
        DownloadList(getString("control_panel_downloads_in_progress"), downloads.filter { !it.isCompleted() }, Modifier.fillMaxWidth(0.5f), multiselect_context, bottom_padding)
        DownloadList(getString("control_panel_downloads_completed"), downloads.filter { it.isCompleted() }, Modifier.fillMaxWidth(), multiselect_context, bottom_padding)
    }
}

@Composable
private fun DownloadList(
    title: String,
    downloads: List<DownloadStatus>,
    modifier: Modifier = Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null,
    bottom_padding: Dp = 0.dp
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            
            Spacer(Modifier.fillMaxWidth().weight(1f))
            
            Text(downloads.size.toString(), style = MaterialTheme.typography.titleMedium)
            
            if (multiselect_context != null) {
                val songs: List<Song> = remember(downloads) { downloads.map { it.song } }
                multiselect_context.CollectionToggleButton(songs)
            }
        }
        
        ScrollBarLazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = bottom_padding)) {
            items(downloads) { download ->
                DownloadStatusDisplay(download, multiselect_context)
            }
        }
    }
}

@Composable
private fun DownloadStatusDisplay(download: DownloadStatus, multiselect_context: MediaItemMultiSelectContext? = null) {
    MediaItemPreviewLong(download.song, multiselect_context = multiselect_context)
}
