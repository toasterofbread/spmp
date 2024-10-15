package com.toasterofbread.spmp.platform

import dev.datlag.kcef.KCEF
import java.io.File
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.webview_runtime_downloading

actual suspend fun initWebViewLogin(
    context: AppContext,
    onProgress: (Float, String?) -> Unit
): Result<Boolean> = runCatching {
    if (KCEF.newClientOrNull() != null) {
        return@runCatching false
    }

    val webview_runtime_downloading: String = getString(Res.string.webview_runtime_downloading)

    return@runCatching withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            runBlocking {
                KCEF.init(
                    builder = {
                        installDir(context.getFilesDir()!!.resolve("kcef-bundle").file)
                        progress {
                            onDownloading {
                                onProgress(it / 100f, webview_runtime_downloading)
                            }
                            onInitialized {
                                continuation.resume(false)
                            }
                        }
                        settings {
                            cachePath = context.getCacheDir()!!.resolve("kcef").absolute_path
                        }
                    },
                    onError = { error ->
                        if (error != null) {
                            error.printStackTrace()
                            try {
                                continuation.resumeWithException(error)
                            }
                            catch (_: IllegalStateException) {}
                        }
                    },
                    onRestartRequired = {
                        try {
                            continuation.resume(true)
                        }
                        catch (_: IllegalStateException) {}
                    }
                )
            }
        }
    }
}
