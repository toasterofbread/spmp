package com.spectre7.spmp

import android.app.Notification
import android.os.Environment.getExternalStorageDirectory
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Requirements
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

class PlayerDownloadService: DownloadService(FOREGROUND_NOTIFICATION_ID_NONE) {
    override fun getDownloadManager(): DownloadManager {
        val database = MainActivity.database
        val path = File(getExternalStorageDirectory(), getString(R.string.app_name))

        val cache = SimpleCache(
            path,
            NoOpCacheEvictor(),
            database
        );

        val ret = DownloadManager(
            this,
            database,
            cache,
            DefaultHttpDataSource.Factory(),
            Runnable::run
        );

        ret.maxParallelDownloads = 5
        ret.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
                }   
                override fun onDownloadsPausedChanged(manager: DownloadManager, downloadsPaused: Boolean) {
                }
                override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
                }
            }
        )

        return ret
    }

    override fun getScheduler(): Scheduler? {
        return null
    }

    override fun getForegroundNotification(downloads: MutableList<Download>, @Requirements.RequirementFlags notMetRequirements: Int): Notification {
        throw UnsupportedOperationException()
    }
}