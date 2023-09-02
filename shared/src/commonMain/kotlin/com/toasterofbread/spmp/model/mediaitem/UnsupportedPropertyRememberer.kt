package com.toasterofbread.spmp.model.mediaitem

class UnsupportedPropertyRememberer(
    private val can_read: Boolean = false,
    private val can_write: Boolean = false,
    private val getUnsupportedUsageMessage: ((on_read: Boolean) -> String)? = null
): PropertyRememberer() {
    override fun onRead(key: String) {
        if (!can_read) {
            val message: String = getUnsupportedUsageMessage?.invoke(true) ?: ""
            throw UnsupportedOperationException("Property '$key' cannot be read from. $message")
        }
    }

    override fun onWrite(key: String) {
        if (!can_write) {
            val message: String = getUnsupportedUsageMessage?.invoke(false) ?: ""
            throw UnsupportedOperationException("Property '$key' cannot be written to. $message")
        }
    }
}
