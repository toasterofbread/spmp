package com.toasterofbread.spmp.platform

import dev.datlag.kcef.KCEF
import java.io.File
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun initWebViewLogin(
    context: AppContext,
    onProgress: (Float, String?) -> Unit
): Result<Boolean> = runCatching {
    if (KCEF.newClientOrNull() != null) {
        return@runCatching false
    }

    return@runCatching withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            runBlocking {
                KCEF.init(
                    builder = {
                        installDir(context.getFilesDir().resolve("kcef-bundle"))
                        progress {
                            onDownloading {
                                onProgress(it / 100f, getString("webview_runtime_downloading"))
                            }
                            onInitialized {
                                continuation.resume(false)
                            }
                        }
                        settings {
                            cachePath = context.getCacheDir().resolve("kcef").absolutePath
                        }
                    },
                    onError = { error ->
                        if (error != null) {
                            continuation.resumeWithException(error)
                        }
                    },
                    onRestartRequired = {
                        continuation.resume(true)
                    }
                )
            }
        }
    }
}
