package com.toasterofbread.spmp.model.mediaitem

fun MediaItem.toInfoString(): String {
    if (this !is MediaItemData) {
        return toString()
    }

    val string: StringBuilder = StringBuilder(toString())

    val values: Map<String, Any?> = getDataValues()
    if (values.isNotEmpty()) {
        string.append(" {")
        for (entry in values) {
            string.append("\n    ${entry.key}=${entry.value}")
        }
        string.append("\n}")
    }

    return string.toString()
}
