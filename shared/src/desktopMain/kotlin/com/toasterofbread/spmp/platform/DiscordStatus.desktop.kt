package com.toasterofbread.spmp.platform

// TODO

actual class DiscordStatus {
    actual companion object {
        actual fun isSupported(): Boolean = false
        actual fun tokenRequired(): Boolean = false
    }

    actual val enabled: Boolean
        get() = TODO("Not yet implemented")

    actual fun enable() {
    }

    actual fun disable() {
    }

}