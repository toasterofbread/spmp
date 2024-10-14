package com.toasterofbread.spmp.model.settings.category

import dev.toastbits.ytmkt.formats.PipedVideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.YoutubeiVideoFormatsEndpoint
import dev.toastbits.ytmkt.model.YtmApi

actual fun VideoFormatsEndpointType.isAvailable(): Boolean =
    when (this) {
        VideoFormatsEndpointType.YOUTUBEI -> true
        VideoFormatsEndpointType.PIPED -> true
        VideoFormatsEndpointType.NEWPIPE -> false
    }

actual fun VideoFormatsEndpointType.instantiate(api: YtmApi): VideoFormatsEndpoint =
    when (this) {
        VideoFormatsEndpointType.YOUTUBEI -> YoutubeiVideoFormatsEndpoint(api)
        VideoFormatsEndpointType.PIPED -> PipedVideoFormatsEndpoint(api)
        VideoFormatsEndpointType.NEWPIPE -> throw IllegalStateException()
    }
