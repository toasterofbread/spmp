package com.toasterofbread.spmp.model.mediaitem.library

import com.toasterofbread.spmp.platform.download.DownloadStatus

internal expect class LocalSongSyncLoader(): SyncLoader<DownloadStatus>
