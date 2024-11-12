package com.toasterofbread.spmp.util

fun <T> MutableList<T>.removeLastBuiltIn(): T {
    if (isEmpty()) {
        throw NoSuchElementException()
    }

    return removeAt(size - 1)
}
