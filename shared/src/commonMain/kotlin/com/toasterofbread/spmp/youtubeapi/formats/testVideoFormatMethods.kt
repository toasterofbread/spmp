package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun testVideoFormatMethods(
    ids: List<String>,
    context: PlatformContext,
    filter: ((YoutubeVideoFormat) -> Boolean)? = null,
) = withContext(Dispatchers.IO) {
    val api = YoutubeMusicApi(context)
    api.init()

    val methods: Map<String, VideoFormatsEndpoint> = mapOf(
        Pair("Piped", PipedVideoFormatsEndpoint(api)),
        Pair("Youtubei", YoutubeiVideoFormatsEndpoint(api))
    )

    println("--- Begin test ---")

    val totals: MutableMap<String, Long?> = methods.mapValues { 0L }.toMutableMap()
    for (method in methods) {
        println("-- Testing method ${method.key} with ${ids.size} songs --")

        var total: Long = 0

        for (id in ids) {
            println("Testing id $id")

            val start = System.currentTimeMillis()
            val result = method.value.getVideoFormats(id, filter)
            val duration = System.currentTimeMillis() - start

            println("Finished in ${duration}ms")
            total += duration

            result.fold(
                { formats ->
                    println("Method produced ${formats.size} formats for $id")
                },
                { error ->
                    println("Method failed ($id): $error")
                    totals[method.key] = null
                }
            )

            if (totals[method.key] == null) {
                break
            }

            total += (System.currentTimeMillis() - start)
        }

        if (totals[method.key] == null) {
            continue
        }

        totals[method.key] = total
    }

    println("\nTest results:")

    for (total in totals) {
        if (total.value == null) {
            println("${total.key}: Test failed")
        }
        else {
            println("${total.key}: ${(total.value!!.toFloat() / ids.size) / 1000f}s")
        }
    }

    println("--- End test ---")
}
