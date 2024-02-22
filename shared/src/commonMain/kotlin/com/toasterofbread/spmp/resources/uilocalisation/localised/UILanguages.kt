package com.toasterofbread.spmp.resources.uilocalisation.localised

object UILanguages {
    val en: String = "en-GB"
    val ja: String = "ja-JP"
    val pl: String = "pl-PL"
    val es: String = "es-US"
    val zh: String = "zh-CN"
}

fun <T> Map<String, T>.getByLanguage(language: String): IndexedValue<Map.Entry<String, T>>? {
    val exact: IndexedValue<Map.Entry<String, T>>? = entries.withIndex().firstOrNull { it.value.key == language }
    if (exact != null) {
        return exact
    }

    val primary: String = language.primary_language
    for (entry in entries.withIndex()) {
        if (entry.value.key.primary_language == primary) {
            return entry
        }
    }

    return null
}

val String.primary_language: String get() = this.split('-', limit = 2).first()

fun String.matchesLanguage(other: String): Boolean {
    return this == other || this.primary_language == other.primary_language
}
