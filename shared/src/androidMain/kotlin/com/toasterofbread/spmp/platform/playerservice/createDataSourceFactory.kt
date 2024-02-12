package com.toasterofbread.spmp.platform.playerservice

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import com.toasterofbread.spmp.platform.processMediaDataSpec
import kotlinx.coroutines.runBlocking
import java.io.IOException

internal fun ForegroundPlayerService.createDataSourceFactory(): DataSource.Factory {
    return ResolvingDataSource.Factory({
        DefaultDataSource.Factory(this).createDataSource()
    }) { data_spec: DataSpec ->
        try {
            return@Factory runBlocking {
                processMediaDataSpec(data_spec, context, context.isConnectionMetered()).also {
                    loudness_enhancer?.update(current_song, context)
                }
            }
        }
        catch (e: Throwable) {
            throw IOException(e)
        }
    }
}
