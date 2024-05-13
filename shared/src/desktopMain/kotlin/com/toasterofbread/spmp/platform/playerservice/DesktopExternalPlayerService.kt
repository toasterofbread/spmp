package com.toasterofbread.spmp.platform.playerservice

abstract class DesktopExternalPlayerService: ExternalPlayerService(plays_audio = false) {
    // private var cancelling_radio = true
    // private val server_listener: PlayerListener =
    //     object : PlayerListener() {
    //         override fun onRadioCancelRequested() {
    //             cancelling_radio = true
    //             radio_instance.cancelRadio()
    //             cancelling_radio = false
    //         }
    //     }

    // override fun onRadioCancelled() {
    //     if (cancelling_radio) {
    //         return
    //     }
    //     super.onRadioCancelled()
    //     server.sendRadioCancellationEvent()
    // }
}
