package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi

fun testVideoFormatMethods(ids: List<String>, context: PlatformContext, filter: ((YoutubeVideoFormat) -> Boolean)? = null) {
    val api = YoutubeMusicApi(context)

    val methods: Map<String, VideoFormatsEndpoint> = mapOf(
        Pair("NewPipe", NewPipeVideoFormatsEndpoint(api)),
        Pair("Piped API", PipedVideoFormatsEndpoint(api)),
        Pair("Youtubei", YoutubeiVideoFormatsEndpoint(api)),
        Pair("Youtube player", YoutubePlayerVideoFormatsEndpoint(api))
    )

    println("--- Begin test ---")

    val totals: MutableMap<String, Long> = methods.mapValues { 0L }.toMutableMap()
    for (id in ids) {
        println("Testing id $id")
        for (method in methods) {
            println("Testing method ${method.key}")
            val start = System.currentTimeMillis()

            method.value.getVideoFormats(id, filter).fold(
                { formats ->
                    println("Method ${method.key} produced ${formats.size} formats")
                },
                { error ->
                    println("Method ${method.key} failed: $error")
                }
            )

            totals[method.key] = totals[method.key]!! + (System.currentTimeMillis() - start)
        }
    }

    println("Test results:")

    for (total in totals) {
        println("${total.key}: ${(total.value.toFloat() / ids.size) / 1000f}s")
    }

    println("--- End test ---")
}
