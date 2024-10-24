package com.toasterofbread.spmp.platform.playerservice

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import kotlinx.coroutines.runBlocking
import java.io.IOException

@OptIn(UnstableApi::class)
internal fun ForegroundPlayerService.createDataSourceFactory(processor: MediaDataSpecProcessor): DataSource.Factory {
    return ResolvingDataSource.Factory({
        DefaultDataSource.Factory(this).createDataSource()
    }) { data_spec: DataSpec ->
        try {
            return@Factory runBlocking {
                processor.processMediaDataSpec(data_spec).also {
                    loudness_enhancer?.update(current_song, context)
                }
            }
        }
        catch (e: Throwable) {
            throw IOException(e)
        }
    }
}
