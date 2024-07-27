package com.toasterofbread.spmp.model.mediaitem.library

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus

internal actual class LocalSongSyncLoader actual constructor(): SyncLoader<DownloadStatus>() {
    override suspend fun internalPerformSync(context: AppContext): Map<String, DownloadStatus> = emptyMap()
}
