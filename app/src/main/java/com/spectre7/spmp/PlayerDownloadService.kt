package com.spectre7.spmp

import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.spectre7.utils.getString
import com.spectre7.spmp.R
import java.util.concurrent.Executors
import java.io.File

class PlayerDownloadService: DownloadService(DownloadService.FOREGROUND_NOTIFICATION_ID_NONE) {
    override fun getDownloadManager(): DownloadManager {
        val database = MainActivity.database
        val path = File(MainActivity.context.getExternalStorageDirectory(), getString(R.string.app_name))

        val cache = SimpleCache(
            path,
            NoOpCacheEvictor(),
            database
        );

        val ret = DownloadManager(
            this,
            database,
            cache,
            DefaultHttpDataSource.Factory()
        );

        ret.maxParallelDownloads = 5
        ret.addListener { 
            object : DownloadManager.Listener {
                override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
                }   
                override fun onDownloadsPausedChanged(manager: DownloadManager, downloadsPaused: Boolean) {
                }
                override fun onDownloadChanged(manager: DownloadManager, download: Download) {
                }
            }
        }

        return ret
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, 1)
    }

    override fun getForegroundNotification(
        throw UnsupportedOperationException()
    }
}